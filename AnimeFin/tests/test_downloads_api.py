from pathlib import Path

from app.config import AppConfig
from app.main import create_app


class _FakeAnimeSource:
    def search_shows(self, query, mode):  # noqa: ARG002
        return [{"id": "show-1", "title": "Frieren", "episode_count": 10}]

    def list_episodes(self, show_id, mode):  # noqa: ARG002
        return ["1", "2", "3"]


class _FakeDownloads:
    def __init__(self):
        self.jobs = {
            "job-1": {
                "id": "job-1",
                "show_id": "show-1",
                "show_title": "Frieren",
                "episode": "1",
                "status": "queued",
                "progress_pct": 0.0,
                "events": [],
            }
        }

    def enqueue(self, req):  # noqa: ARG002
        return ["job-1"]

    def list_jobs(self):
        return list(self.jobs.values())

    def get_job(self, job_id):
        return self.jobs.get(job_id)

    def cancel(self, job_id):
        return job_id in self.jobs


class _FakeMedia:
    def list_media(self):
        return []

    def delete_media(self, media_id):  # noqa: ARG002
        return {"deleted": media_id}


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
    app.extensions["anime_source"] = _FakeAnimeSource()
    app.extensions["downloads"] = _FakeDownloads()
    app.extensions["media"] = _FakeMedia()
    return app


def test_search_and_episode_routes(tmp_path):
    app = _build_test_app(tmp_path)
    client = app.test_client()

    search_res = client.get("/api/search?q=frieren&mode=sub")
    assert search_res.status_code == 200
    assert search_res.get_json()["results"][0]["title"] == "Frieren"

    episodes_res = client.get("/api/shows/show-1/episodes?mode=sub")
    assert episodes_res.status_code == 200
    assert episodes_res.get_json()["episodes"] == ["1", "2", "3"]


def test_download_create_and_cancel_routes(tmp_path):
    app = _build_test_app(tmp_path)
    client = app.test_client()
    create_res = client.post(
        "/api/downloads",
        json={
            "show_id": "show-1",
            "show_title": "Frieren",
            "episodes": ["1", "2"],
            "mode": "sub",
            "quality": "best",
        },
    )
    assert create_res.status_code == 202
    assert create_res.get_json()["job_ids"] == ["job-1"]

    list_res = client.get("/api/downloads")
    assert list_res.status_code == 200
    assert len(list_res.get_json()["jobs"]) == 1

    cancel_res = client.delete("/api/downloads/job-1")
    assert cancel_res.status_code == 200
