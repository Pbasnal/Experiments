from __future__ import annotations

import json
import urllib.error
import urllib.request
from typing import Any


class AnimeSourceService:
    def __init__(self, api_url: str, referer: str, user_agent: str) -> None:
        self.api_url = api_url
        self.referer = referer
        self.user_agent = user_agent

    def _post_graphql(self, payload: dict[str, Any], timeout: int = 20) -> dict[str, Any]:
        request = urllib.request.Request(
            self.api_url,
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Content-Type": "application/json",
                "Referer": self.referer,
                "User-Agent": self.user_agent,
            },
            method="POST",
        )
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return json.loads(response.read().decode("utf-8"))

    def search_shows(self, query: str, mode: str = "sub") -> list[dict[str, Any]]:
        gql = (
            "query( $search: SearchInput $limit: Int $page: Int "
            "$translationType: VaildTranslationTypeEnumType "
            "$countryOrigin: VaildCountryOriginEnumType ) { "
            "shows( search: $search limit: $limit page: $page translationType: $translationType "
            "countryOrigin: $countryOrigin ) { edges { _id name availableEpisodes __typename } }}"
        )
        payload: dict[str, Any] = {
            "variables": {
                "search": {"allowAdult": False, "allowUnknown": False, "query": query},
                "limit": 40,
                "page": 1,
                "translationType": mode,
                "countryOrigin": "ALL",
            },
            "query": gql,
        }
        try:
            data = self._post_graphql(payload)
        except (urllib.error.URLError, TimeoutError, json.JSONDecodeError):
            return []

        edges = (((data or {}).get("data") or {}).get("shows") or {}).get("edges") or []
        results: list[dict[str, Any]] = []
        for edge in edges:
            if not edge:
                continue
            available = edge.get("availableEpisodes") or {}
            episode_count = available.get(mode) or 0
            if not episode_count:
                continue
            results.append(
                {
                    "id": edge.get("_id"),
                    "title": (edge.get("name") or "").replace('\\"', ""),
                    "episode_count": episode_count,
                }
            )
        return results

    def list_episodes(self, show_id: str, mode: str = "sub") -> list[str]:
        gql = "query ($showId: String!) { show( _id: $showId ) { _id availableEpisodesDetail }}"
        payload: dict[str, Any] = {"variables": {"showId": show_id}, "query": gql}
        try:
            data = self._post_graphql(payload)
        except (urllib.error.URLError, TimeoutError, json.JSONDecodeError):
            return []

        details = (((data or {}).get("data") or {}).get("show") or {}).get("availableEpisodesDetail") or {}
        episodes = details.get(mode) or []
        numeric = sorted({str(e) for e in episodes}, key=lambda x: float(x))
        return numeric
