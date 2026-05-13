from __future__ import annotations

import json
import os
import re
from pathlib import Path
from typing import Any
from urllib.parse import urlparse


def _default_base_url() -> str:
    return os.getenv("ANIMEPAHE_BASE_URL", "https://animepahe.com").rstrip("/")


def animepahe_config_path(runtime_home: Path) -> Path:
    return (runtime_home / ".config" / "animepahe-dl" / "config.json").resolve()


def ensure_animepahe_config_dir(runtime_home: Path) -> Path:
    path = animepahe_config_path(runtime_home)
    path.parent.mkdir(parents=True, exist_ok=True)
    return path


def validate_base_url(raw: str) -> str:
    s = (raw or "").strip().rstrip("/")
    if not s:
        raise ValueError("base_url is required")
    if len(s) > 256:
        raise ValueError("base_url is too long")
    parsed = urlparse(s)
    if parsed.scheme not in {"https", "http"}:
        raise ValueError("base_url must start with http:// or https://")
    if not parsed.netloc or re.search(r"\s", s):
        raise ValueError("base_url must include a valid host")
    return s


def read_animepahe_settings(runtime_home: Path) -> dict[str, Any]:
    path = ensure_animepahe_config_dir(runtime_home)
    if not path.is_file():
        return {"base_url": _default_base_url(), "config_path": str(path)}
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError):
        data = {}
    base = str(data.get("base_url") or _default_base_url()).rstrip("/")
    return {"base_url": base, "config_path": str(path)}


def write_animepahe_base_url(runtime_home: Path, base_url: str) -> dict[str, Any]:
    normalized = validate_base_url(base_url)
    path = ensure_animepahe_config_dir(runtime_home)
    data: dict[str, Any] = {}
    if path.is_file():
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, OSError):
            data = {}
    data["base_url"] = normalized
    path.write_text(json.dumps(data, indent=4), encoding="utf-8")
    return {"base_url": normalized, "config_path": str(path)}


def ensure_default_animepahe_config(runtime_home: Path) -> None:
    """Create config.json with default base_url if missing (first deploy)."""
    path = ensure_animepahe_config_dir(runtime_home)
    if path.is_file():
        return
    payload = {"base_url": _default_base_url()}
    path.write_text(json.dumps(payload, indent=4), encoding="utf-8")
