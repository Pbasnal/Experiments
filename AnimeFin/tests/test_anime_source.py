import json

from app.services.anime_source import AnimeSourceService


class _FakeResponse:
    def __init__(self, payload: dict):
        self._payload = payload

    def read(self) -> bytes:
        return json.dumps(self._payload).encode("utf-8")

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        return None


def test_search_shows_parses_results(monkeypatch):
    payload = {
        "data": {
            "shows": {
                "edges": [
                    {
                        "_id": "show-1",
                        "name": "Frieren",
                        "availableEpisodes": {"sub": 10, "dub": 8},
                    }
                ]
            }
        }
    }

    def fake_urlopen(*args, **kwargs):  # noqa: ARG001
        return _FakeResponse(payload)

    monkeypatch.setattr("urllib.request.urlopen", fake_urlopen)

    service = AnimeSourceService("https://example.test/api", "https://example.test", "agent")
    results = service.search_shows("frieren", mode="sub")
    assert results == [{"id": "show-1", "title": "Frieren", "episode_count": 10}]


def test_list_episodes_returns_sorted_unique_values(monkeypatch):
    payload = {
        "data": {
            "show": {
                "_id": "show-1",
                "availableEpisodesDetail": {"sub": [3, 1, 2, 2, 1.5]},
            }
        }
    }

    def fake_urlopen(*args, **kwargs):  # noqa: ARG001
        return _FakeResponse(payload)

    monkeypatch.setattr("urllib.request.urlopen", fake_urlopen)

    service = AnimeSourceService("https://example.test/api", "https://example.test", "agent")
    episodes = service.list_episodes("show-1", mode="sub")
    assert episodes == ["1", "1.5", "2", "3"]
