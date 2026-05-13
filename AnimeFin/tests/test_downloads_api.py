from pathlib import Path

from app.config import AppConfig
from app.main import create_app


class _FakeAnimeSource:
    def search_shows(self, query, mode):  # noqa: ARG002
        self.last_search_mode = mode
        return [{"id": "show-1", "title": "Frieren", "episode_count": 10}]

    def list_episodes(self, show_id, mode):  # noqa: ARG002
        self.last_episodes_mode = mode
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
        animepahe_dl_exe=None,
        animepahe_runtime_home=tmp_path / "animepahe-home",
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


def test_routes_default_mode_to_dub(tmp_path):
    app = _build_test_app(tmp_path)
    client = app.test_client()

    search_res = client.get("/api/search?q=frieren")
    assert search_res.status_code == 200
    assert app.extensions["anime_source"].last_search_mode == "dub"

    episodes_res = client.get("/api/shows/show-1/episodes")
    assert episodes_res.status_code == 200
    assert app.extensions["anime_source"].last_episodes_mode == "dub"

    create_res = client.post(
        "/api/downloads",
        json={
            "show_id": "show-1",
            "show_title": "Frieren",
            "episodes": ["1", "2"],
            "quality": "best",
        },
    )
    assert create_res.status_code == 202


def test_animepahe_settings_get_put(tmp_path):
    app = _build_test_app(tmp_path)
    client = app.test_client()
    get_res = client.get("/api/settings/animepahe")
    assert get_res.status_code == 200
    body = get_res.get_json()
    assert "base_url" in body
    assert "config_path" in body
    assert Path(body["config_path"]).name == "config.json"

    put_res = client.put("/api/settings/animepahe", json={"base_url": "https://animepahe.example"})
    assert put_res.status_code == 200
    assert put_res.get_json()["base_url"] == "https://animepahe.example"

    get2 = client.get("/api/settings/animepahe")
    assert get2.get_json()["base_url"] == "https://animepahe.example"


def test_animepahe_settings_put_validation(tmp_path):
    app = _build_test_app(tmp_path)
    client = app.test_client()
    bad = client.put("/api/settings/animepahe", json={"base_url": "ftp://x"})
    assert bad.status_code == 400


def test_download_rejects_invalid_downloader(tmp_path):
    app = _build_test_app(tmp_path)
    client = app.test_client()
    res = client.post(
        "/api/downloads",
        json={
            "show_id": "show-1",
            "show_title": "Frieren",
            "episodes": ["1"],
            "downloader": "nope",
        },
    )
    assert res.status_code == 400


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
