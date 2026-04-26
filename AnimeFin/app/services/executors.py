from __future__ import annotations

import os
import subprocess
from pathlib import Path
from typing import Callable


LineHandler = Callable[[str], None]


class BaseExecutor:
    def run(
        self,
        command: list[str],
        line_handler: LineHandler,
        env: dict[str, str] | None = None,
        should_stop: Callable[[], bool] | None = None,
    ) -> int:
        process = subprocess.Popen(
            command,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
            env=env or os.environ.copy(),
        )
        assert process.stdout is not None
        for line in process.stdout:
            if should_stop and should_stop():
                process.terminate()
                break
            line_handler(line.rstrip())
        return process.wait()


class YtDlpExecutor(BaseExecutor):
    def build_command(self, url: str, output_path: Path, referer: str = "") -> list[str]:
        command = [
            "yt-dlp",
            "--newline",
            "--no-skip-unavailable-fragments",
            "--fragment-retries",
            "infinite",
            "-N",
            "16",
            "-o",
            str(output_path),
            url,
        ]
        if referer:
            command[1:1] = ["--referer", referer]
        return command


class FfmpegExecutor(BaseExecutor):
    def build_command(self, url: str, output_path: Path, referer: str = "") -> list[str]:
        command = [
            "ffmpeg",
            "-extension_picky",
            "0",
            "-loglevel",
            "error",
            "-stats",
            "-i",
            url,
            "-c",
            "copy",
            str(output_path),
        ]
        if referer:
            command[1:1] = ["-referer", referer]
        return command


class Aria2Executor(BaseExecutor):
    def build_command(self, url: str, output_path: Path, referer: str = "") -> list[str]:
        command = [
            "aria2c",
            "--enable-rpc=false",
            "--check-certificate=false",
            "--summary-interval=0",
            "-x",
            "16",
            "-s",
            "16",
            url,
            "--dir",
            str(output_path.parent),
            "-o",
            output_path.name,
            "--download-result=hide",
        ]
        if referer:
            command[1:1] = [f"--referer={referer}"]
        return command
