from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class AppConfig:
    base_dir: Path
    downloads_dir: Path
    database_path: Path
    ani_cli_path: Path
    allanime_api: str
    allanime_referer: str
    user_agent: str
    host: str
    port: int
    debug: bool


def load_config() -> AppConfig:
    base_dir = Path(__file__).resolve().parent.parent
    downloads_dir = Path(os.getenv("DOWNLOADS_DIR", base_dir / "downloads")).resolve()
    database_path = Path(os.getenv("DATABASE_PATH", base_dir / "data" / "jobs.sqlite3")).resolve()
    ani_cli_path = Path(os.getenv("ANI_CLI_PATH", base_dir / "ani-cli" / "ani-cli")).resolve()

    downloads_dir.mkdir(parents=True, exist_ok=True)
    database_path.parent.mkdir(parents=True, exist_ok=True)

    return AppConfig(
        base_dir=base_dir,
        downloads_dir=downloads_dir,
        database_path=database_path,
        ani_cli_path=ani_cli_path,
        allanime_api=os.getenv("ALLANIME_API", "https://api.allanime.day/api"),
        allanime_referer=os.getenv("ALLANIME_REFERER", "https://allmanga.to"),
        user_agent=os.getenv(
            "USER_AGENT",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
        ),
        host=os.getenv("FLASK_HOST", "0.0.0.0"),
        port=int(os.getenv("FLASK_PORT", "5001")),
        debug=os.getenv("FLASK_DEBUG", "0") == "1",
    )
