from __future__ import annotations

import shutil
from pathlib import Path


class MediaStore:
    def __init__(self, downloads_root: Path) -> None:
        self.downloads_root = downloads_root.resolve()
        self.downloads_root.mkdir(parents=True, exist_ok=True)

    def list_media(self) -> list[dict[str, str]]:
        items: list[dict[str, str]] = []
        for path in sorted(self.downloads_root.glob("**/*"), key=lambda p: str(p).lower()):
            if path.is_dir():
                continue
            rel = path.relative_to(self.downloads_root)
            media_id = rel.as_posix()
            items.append(
                {
                    "media_id": media_id,
                    "show": rel.parts[0] if rel.parts else "unknown",
                    "path": str(path),
                }
            )
        return items

    def _safe_path(self, media_id: str) -> Path:
        candidate = (self.downloads_root / media_id).resolve()
        if self.downloads_root not in candidate.parents and candidate != self.downloads_root:
            raise ValueError("Invalid media id path.")
        return candidate

    def delete_media(self, media_id: str) -> dict[str, str]:
        target = self._safe_path(media_id)
        if not target.exists():
            raise FileNotFoundError(media_id)

        if target.is_dir():
            shutil.rmtree(target)
            return {"deleted": media_id, "type": "directory"}

        target.unlink()
        parent = target.parent
        while parent != self.downloads_root and not any(parent.iterdir()):
            parent.rmdir()
            parent = parent.parent
        return {"deleted": media_id, "type": "file"}
