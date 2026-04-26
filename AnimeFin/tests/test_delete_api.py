from pathlib import Path

from app.config import AppConfig
from app.main import create_app
from app.storage.media import MediaStore


def _build_test_app(tmp_path):
    cfg = AppConfig(
        base_dir=Path.cwd(),
        downloads_dir=tmp_path / "downloads",
        database_path=tmp_path / "jobs.sqlite3",
        ani_cli_path=tmp_path / "ani-cli",
        allanime_api="https://example.test",
        allanime_referer="https://example.test",
        user_agent="test-agent",
        host="127.0.0.1",
        port=5001,
        debug=False,
    )
    app = create_app(cfg)
    app.testing = True
    app.extensions["media"] = MediaStore(cfg.downloads_dir)
    return app


def test_delete_media_item(tmp_path):
    app = _build_test_app(tmp_path)
    file_path = tmp_path / "downloads" / "Show Name" / "episode-1.mp4"
    file_path.parent.mkdir(parents=True, exist_ok=True)
    file_path.write_bytes(b"test")

    client = app.test_client()
    list_res = client.get("/api/media")
    assert list_res.status_code == 200
    items = list_res.get_json()["items"]
    assert len(items) == 1
    media_id = items[0]["media_id"]

    delete_res = client.delete(f"/api/media/{media_id}")
    assert delete_res.status_code == 200
    assert not file_path.exists()
