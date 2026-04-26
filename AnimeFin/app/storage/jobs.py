from __future__ import annotations

import sqlite3
import uuid
from contextlib import contextmanager
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterator


def utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


@dataclass(frozen=True)
class NewJob:
    show_id: str
    show_title: str
    episode: str
    mode: str
    quality: str
    output_path: str
    source_url: str = ""
    source_type: str = ""
    referer: str = ""


class JobsStore:
    def __init__(self, db_path: Path) -> None:
        self._db_path = db_path
        self._initialize()

    @contextmanager
    def _connect(self) -> Iterator[sqlite3.Connection]:
        conn = sqlite3.connect(self._db_path)
        conn.row_factory = sqlite3.Row
        try:
            yield conn
            conn.commit()
        finally:
            conn.close()

    def _initialize(self) -> None:
        with self._connect() as conn:
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS jobs (
                    id TEXT PRIMARY KEY,
                    show_id TEXT NOT NULL,
                    show_title TEXT NOT NULL,
                    episode TEXT NOT NULL,
                    mode TEXT NOT NULL,
                    quality TEXT NOT NULL,
                    status TEXT NOT NULL,
                    progress_pct REAL NOT NULL DEFAULT 0,
                    bytes_downloaded INTEGER NOT NULL DEFAULT 0,
                    bytes_total INTEGER NOT NULL DEFAULT 0,
                    output_path TEXT NOT NULL,
                    source_url TEXT NOT NULL DEFAULT '',
                    source_type TEXT NOT NULL DEFAULT '',
                    referer TEXT NOT NULL DEFAULT '',
                    error_message TEXT NOT NULL DEFAULT '',
                    created_at TEXT NOT NULL,
                    started_at TEXT,
                    finished_at TEXT
                )
                """
            )
            columns = conn.execute("PRAGMA table_info(jobs)").fetchall()
            names = {column["name"] for column in columns}
            if "source_url" not in names:
                conn.execute("ALTER TABLE jobs ADD COLUMN source_url TEXT NOT NULL DEFAULT ''")
            if "source_type" not in names:
                conn.execute("ALTER TABLE jobs ADD COLUMN source_type TEXT NOT NULL DEFAULT ''")
            if "referer" not in names:
                conn.execute("ALTER TABLE jobs ADD COLUMN referer TEXT NOT NULL DEFAULT ''")
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS download_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    job_id TEXT NOT NULL,
                    level TEXT NOT NULL,
                    message TEXT NOT NULL,
                    timestamp TEXT NOT NULL,
                    FOREIGN KEY(job_id) REFERENCES jobs(id)
                )
                """
            )

    def create_jobs(self, jobs: list[NewJob]) -> list[str]:
        created_ids: list[str] = []
        now = utc_now_iso()
        with self._connect() as conn:
            for job in jobs:
                job_id = str(uuid.uuid4())
                conn.execute(
                    """
                    INSERT INTO jobs (
                        id, show_id, show_title, episode, mode, quality, status,
                        progress_pct, bytes_downloaded, bytes_total, output_path,
                        source_url, source_type, referer, error_message, created_at, started_at, finished_at
                    ) VALUES (?, ?, ?, ?, ?, ?, 'queued', 0, 0, 0, ?, ?, ?, ?, '', ?, NULL, NULL)
                    """,
                    (
                        job_id,
                        job.show_id,
                        job.show_title,
                        job.episode,
                        job.mode,
                        job.quality,
                        job.output_path,
                        job.source_url,
                        job.source_type,
                        job.referer,
                        now,
                    ),
                )
                conn.execute(
                    """
                    INSERT INTO download_events(job_id, level, message, timestamp)
                    VALUES (?, 'info', 'Job queued', ?)
                    """,
                    (job_id, now),
                )
                created_ids.append(job_id)
        return created_ids

    def list_jobs(self) -> list[dict[str, Any]]:
        with self._connect() as conn:
            rows = conn.execute(
                """
                SELECT *
                FROM jobs
                ORDER BY created_at DESC
                """
            ).fetchall()
        return [dict(r) for r in rows]

    def get_job(self, job_id: str) -> dict[str, Any] | None:
        with self._connect() as conn:
            row = conn.execute("SELECT * FROM jobs WHERE id = ?", (job_id,)).fetchone()
            if not row:
                return None
            events = conn.execute(
                """
                SELECT level, message, timestamp
                FROM download_events
                WHERE job_id = ?
                ORDER BY id ASC
                LIMIT 500
                """,
                (job_id,),
            ).fetchall()
        job = dict(row)
        job["events"] = [dict(e) for e in events]
        return job

    def update_job_status(
        self,
        job_id: str,
        *,
        status: str,
        error_message: str = "",
        started_at: str | None = None,
        finished_at: str | None = None,
    ) -> None:
        with self._connect() as conn:
            conn.execute(
                """
                UPDATE jobs
                SET status = ?, error_message = ?, started_at = COALESCE(?, started_at), finished_at = ?
                WHERE id = ?
                """,
                (status, error_message, started_at, finished_at, job_id),
            )

    def update_progress(
        self,
        job_id: str,
        *,
        progress_pct: float | None = None,
        bytes_downloaded: int | None = None,
        bytes_total: int | None = None,
    ) -> None:
        with self._connect() as conn:
            existing = conn.execute(
                "SELECT progress_pct, bytes_downloaded, bytes_total FROM jobs WHERE id = ?",
                (job_id,),
            ).fetchone()
            if not existing:
                return
            conn.execute(
                """
                UPDATE jobs
                SET progress_pct = ?, bytes_downloaded = ?, bytes_total = ?
                WHERE id = ?
                """,
                (
                    existing["progress_pct"] if progress_pct is None else progress_pct,
                    existing["bytes_downloaded"] if bytes_downloaded is None else bytes_downloaded,
                    existing["bytes_total"] if bytes_total is None else bytes_total,
                    job_id,
                ),
            )

    def append_event(self, job_id: str, level: str, message: str) -> None:
        with self._connect() as conn:
            conn.execute(
                """
                INSERT INTO download_events(job_id, level, message, timestamp)
                VALUES (?, ?, ?, ?)
                """,
                (job_id, level, message, utc_now_iso()),
            )

    def mark_running_jobs_recoverable(self) -> list[str]:
        with self._connect() as conn:
            rows = conn.execute(
                "SELECT id FROM jobs WHERE status IN ('running', 'queued') ORDER BY created_at ASC"
            ).fetchall()
            ids = [r["id"] for r in rows]
            if not ids:
                return []
            for job_id in ids:
                conn.execute(
                    """
                    UPDATE jobs
                    SET status = 'failed_recoverable',
                        error_message = 'App restarted before this job completed',
                        finished_at = ?
                    WHERE id = ?
                    """,
                    (utc_now_iso(), job_id),
                )
                conn.execute(
                    """
                    INSERT INTO download_events(job_id, level, message, timestamp)
                    VALUES (?, 'warn', 'Marked failed_recoverable after restart', ?)
                    """,
                    (job_id, utc_now_iso()),
                )
        return ids
