from __future__ import annotations

import queue
import re
import subprocess
import threading
import os
from dataclasses import dataclass
from pathlib import Path

from app.services.executors import Aria2Executor, BaseExecutor, FfmpegExecutor, YtDlpExecutor
from app.storage.jobs import JobsStore, NewJob, utc_now_iso


@dataclass(frozen=True)
class DownloadRequest:
    show_id: str
    show_title: str
    episodes: list[str]
    mode: str
    quality: str
    source_url: str = ""
    source_type: str = ""
    referer: str = ""


class DownloadService:
    PROGRESS_PATTERNS = [
        re.compile(r"(?P<pct>\d+(?:\.\d+)?)%"),
    ]

    def __init__(self, jobs_store: JobsStore, downloads_root: Path, ani_cli_path: Path) -> None:
        self.jobs_store = jobs_store
        self.downloads_root = downloads_root
        self.ani_cli_path = ani_cli_path
        self._queue: queue.Queue[str] = queue.Queue()
        self._cancelled: set[str] = set()
        self._lock = threading.Lock()
        self._worker_thread = threading.Thread(target=self._worker_loop, daemon=True)
        self._worker_thread_started = False
        self._yt_dlp = YtDlpExecutor()
        self._ffmpeg = FfmpegExecutor()
        self._aria2 = Aria2Executor()

    def start(self) -> None:
        self.jobs_store.mark_running_jobs_recoverable()
        if not self._worker_thread_started:
            self._worker_thread.start()
            self._worker_thread_started = True

    def enqueue(self, req: DownloadRequest) -> list[str]:
        safe_show = self._safe_show_name(req.show_title)
        created = self.jobs_store.create_jobs(
            [
                NewJob(
                    show_id=req.show_id,
                    show_title=req.show_title,
                    episode=ep,
                    mode=req.mode,
                    quality=req.quality,
                    output_path=str((self.downloads_root / safe_show / f"episode-{ep}.mp4").resolve()),
                    source_url=req.source_url,
                    source_type=req.source_type,
                    referer=req.referer,
                )
                for ep in req.episodes
            ]
        )
        for job_id in created:
            self._queue.put(job_id)
        return created

    def list_jobs(self) -> list[dict]:
        return self.jobs_store.list_jobs()

    def get_job(self, job_id: str) -> dict | None:
        return self.jobs_store.get_job(job_id)

    def cancel(self, job_id: str) -> bool:
        job = self.jobs_store.get_job(job_id)
        if not job:
            return False
        if job["status"] in {"done", "failed", "cancelled"}:
            return False
        with self._lock:
            self._cancelled.add(job_id)
        self.jobs_store.update_job_status(job_id, status="cancelled", finished_at=utc_now_iso(), error_message="")
        self.jobs_store.append_event(job_id, "warn", "Cancellation requested")
        return True

    def _worker_loop(self) -> None:
        while True:
            job_id = self._queue.get()
            try:
                self._process_job(job_id)
            finally:
                self._queue.task_done()

    def _process_job(self, job_id: str) -> None:
        job = self.jobs_store.get_job(job_id)
        if not job:
            return
        if self._is_cancelled(job_id):
            return

        self.jobs_store.update_job_status(job_id, status="running", started_at=utc_now_iso(), error_message="")
        self.jobs_store.append_event(job_id, "info", "Download started")

        if not job["source_url"] and not self.ani_cli_path.exists():
            self.jobs_store.update_job_status(
                job_id,
                status="failed",
                error_message=f"ani-cli not found: {self.ani_cli_path}",
                finished_at=utc_now_iso(),
            )
            self.jobs_store.append_event(job_id, "error", "ani-cli executable is missing")
            return

        show_dir = self.downloads_root / self._safe_show_name(job["show_title"])
        show_dir.mkdir(parents=True, exist_ok=True)

        env = {**os.environ, "ANI_CLI_DOWNLOAD_DIR": str(show_dir)}
        command, executor = self._resolve_command_and_executor(job, show_dir)
        self.jobs_store.append_event(job_id, "info", f"Executing: {' '.join(command)}")

        def on_line(clean: str) -> None:
            if not clean:
                return
            self.jobs_store.append_event(job_id, "info", clean)
            self._update_progress_from_line(job_id, clean)

        code = executor.run(command, on_line, env=env, should_stop=lambda: self._is_cancelled(job_id))
        if self._is_cancelled(job_id):
            self.jobs_store.update_job_status(job_id, status="cancelled", finished_at=utc_now_iso())
            self.jobs_store.append_event(job_id, "warn", "Download cancelled")
        elif code == 0:
            self.jobs_store.update_job_status(job_id, status="done", finished_at=utc_now_iso())
            self.jobs_store.update_progress(job_id, progress_pct=100.0)
            self.jobs_store.append_event(job_id, "info", "Download completed")
        else:
            self.jobs_store.update_job_status(
                job_id,
                status="failed",
                error_message=f"Downloader exited with code {code}",
                finished_at=utc_now_iso(),
            )
            self.jobs_store.append_event(job_id, "error", f"Downloader exited with code {code}")

    def _update_progress_from_line(self, job_id: str, line: str) -> None:
        for pattern in self.PROGRESS_PATTERNS:
            match = pattern.search(line)
            if match:
                pct = float(match.group("pct"))
                if 0 <= pct <= 100:
                    self.jobs_store.update_progress(job_id, progress_pct=pct)
                return

    def _resolve_command_and_executor(self, job: dict, show_dir: Path) -> tuple[list[str], BaseExecutor]:
        output_path = Path(job["output_path"])
        source_url = str(job.get("source_url") or "").strip()
        source_type = str(job.get("source_type") or "").strip().lower()
        referer = str(job.get("referer") or "").strip()

        if source_url:
            if source_type == "m3u8_ffmpeg":
                return self._ffmpeg.build_command(source_url, output_path, referer), self._ffmpeg
            if source_type == "mp4_aria2":
                return self._aria2.build_command(source_url, output_path, referer), self._aria2
            return self._yt_dlp.build_command(source_url, output_path, referer), self._yt_dlp

        command = [
            str(self.ani_cli_path),
            "-d",
            "-S",
            "1",
            "-e",
            str(job["episode"]),
            str(job["show_title"]),
        ]
        if job["mode"] == "dub":
            command.insert(1, "--dub")
        if job["quality"] not in {"", "best"}:
            command[1:1] = ["-q", str(job["quality"])]
        return command, BaseExecutor()

    def _is_cancelled(self, job_id: str) -> bool:
        with self._lock:
            return job_id in self._cancelled

    @staticmethod
    def _safe_show_name(show_title: str) -> str:
        safe = "".join(c for c in show_title if c.isalnum() or c in (" ", "-", "_")).strip()
        return safe or "show"
