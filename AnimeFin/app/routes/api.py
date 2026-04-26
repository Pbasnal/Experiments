from __future__ import annotations

from flask import Blueprint, current_app, jsonify, request


api_bp = Blueprint("api", __name__, url_prefix="/api")


def _services():
    anime_source = current_app.extensions["anime_source"]
    downloads = current_app.extensions["downloads"]
    media = current_app.extensions["media"]
    return anime_source, downloads, media


@api_bp.get("/health")
def health() -> tuple[dict, int]:
    return {"ok": True}, 200


@api_bp.get("/search")
def search_shows():
    anime_source, _, _ = _services()
    query = (request.args.get("q") or "").strip()
    mode = (request.args.get("mode") or "sub").strip().lower()
    if not query:
        return jsonify({"error": "Missing required query parameter: q"}), 400
    if mode not in {"sub", "dub"}:
        return jsonify({"error": "mode must be sub or dub"}), 400
    return jsonify({"query": query, "mode": mode, "results": anime_source.search_shows(query, mode)})


@api_bp.get("/shows/<show_id>/episodes")
def show_episodes(show_id: str):
    anime_source, _, _ = _services()
    mode = (request.args.get("mode") or "sub").strip().lower()
    if mode not in {"sub", "dub"}:
        return jsonify({"error": "mode must be sub or dub"}), 400
    return jsonify({"show_id": show_id, "mode": mode, "episodes": anime_source.list_episodes(show_id, mode)})


@api_bp.post("/downloads")
def create_downloads():
    _, downloads, _ = _services()
    body = request.get_json(force=True)
    show_id = (body.get("show_id") or "").strip()
    show_title = (body.get("show_title") or "").strip()
    mode = (body.get("mode") or "sub").strip().lower()
    quality = (body.get("quality") or "best").strip().lower()
    source_url = (body.get("source_url") or "").strip()
    source_type = (body.get("source_type") or "").strip().lower()
    referer = (body.get("referer") or "").strip()
    episodes = body.get("episodes") or []

    if not show_id or not show_title:
        return jsonify({"error": "show_id and show_title are required"}), 400
    if mode not in {"sub", "dub"}:
        return jsonify({"error": "mode must be sub or dub"}), 400
    if not isinstance(episodes, list) or not episodes:
        return jsonify({"error": "episodes must be a non-empty array"}), 400

    from app.services.downloads import DownloadRequest

    job_ids = downloads.enqueue(
        DownloadRequest(
            show_id=show_id,
            show_title=show_title,
            episodes=[str(ep) for ep in episodes],
            mode=mode,
            quality=quality,
            source_url=source_url,
            source_type=source_type,
            referer=referer,
        )
    )
    return jsonify({"job_ids": job_ids}), 202


@api_bp.get("/downloads")
def list_downloads():
    _, downloads, _ = _services()
    jobs = downloads.list_jobs()
    grouped = {"queued": [], "running": [], "done": [], "failed": [], "cancelled": [], "other": []}
    for job in jobs:
        key = job["status"] if job["status"] in grouped else "other"
        grouped[key].append(job)
    return jsonify({"jobs": jobs, "groups": grouped})


@api_bp.get("/downloads/<job_id>")
def get_download(job_id: str):
    _, downloads, _ = _services()
    job = downloads.get_job(job_id)
    if not job:
        return jsonify({"error": "Job not found"}), 404
    return jsonify(job)


@api_bp.delete("/downloads/<job_id>")
def cancel_download(job_id: str):
    _, downloads, _ = _services()
    if not downloads.cancel(job_id):
        return jsonify({"error": "Unable to cancel job"}), 400
    return jsonify({"ok": True, "job_id": job_id})


@api_bp.get("/media")
def list_media():
    _, _, media = _services()
    return jsonify({"items": media.list_media()})


@api_bp.delete("/media/<path:media_id>")
def delete_media(media_id: str):
    _, _, media = _services()
    try:
        result = media.delete_media(media_id)
    except FileNotFoundError:
        return jsonify({"error": "Media item not found"}), 404
    except ValueError:
        return jsonify({"error": "Invalid media path"}), 400
    return jsonify(result)
