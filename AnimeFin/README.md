# Flask Anime Downloader

Flask API + simple web UI for:
- searching anime titles (sub/dub),
- triggering episode downloads,
- monitoring download job status/progress/history,
- deleting downloaded media safely.

No streaming playback features are implemented.

## Architecture Overview

- `app/services/anime_source.py`: AllAnime GraphQL search and episode listing
- `app/services/downloads.py`: queue, worker lifecycle, download execution, progress parsing
- `app/storage/jobs.py`: SQLite persistence for jobs + events
- `app/storage/media.py`: downloaded media listing + safe deletion
- `app/routes/api.py`: API endpoints
- `app/routes/ui.py`: simple search page and downloads dashboard

## Requirements

- Python 3.9+
- Local `ani-cli` script at `./ani-cli/ani-cli` (default path)
- For direct source-url downloads (optional): `yt-dlp`, `ffmpeg`, `aria2c`

## Setup

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
```

## Run

```bash
python3 app.py
```

Then open:
- [http://localhost:5001](http://localhost:5001) for search/enqueue
- [http://localhost:5001/downloads](http://localhost:5001/downloads) for monitoring/cleanup

## API Endpoints

- `GET /api/health`
- `GET /api/search?q=<query>&mode=sub|dub`
- `GET /api/shows/<show_id>/episodes?mode=sub|dub`
- `POST /api/downloads`
  - body:
    - `show_id` (required)
    - `show_title` (required)
    - `episodes` (required array)
    - `mode` (`sub` or `dub`)
    - `quality` (default `best`)
    - optional direct-source fields:
      - `source_url`
      - `source_type` (`m3u8_ffmpeg` or `mp4_aria2`; defaults to yt-dlp flow for direct url)
      - `referer`
- `GET /api/downloads`
- `GET /api/downloads/<job_id>`
- `DELETE /api/downloads/<job_id>` (cancel queued/running job)
- `GET /api/media`
- `DELETE /api/media/<media_id>`

## Safety Notes

- Media deletion only works inside configured `DOWNLOADS_DIR`.
- Path traversal and parent-escape paths are rejected.
- On restart, in-flight jobs are marked `failed_recoverable`.

## Tests

```bash
pytest -q
```

## Docker: Flask + Jellyfin (separate containers, shared downloads)

Two containers share the same host folder `./downloads` (mounted as `/media/downloads` in each):

- **Flask** writes downloads there and serves the web UI/API on port **5001**.
- **Jellyfin** reads that folder for libraries (mount is read-only in the Jellyfin container).

```bash
./scripts/setup-jellyfin.sh up
```

Endpoints:
- Flask: [http://localhost:5001](http://localhost:5001)
- Jellyfin: [http://localhost:8096](http://localhost:8096)

In Jellyfin, add a library with folder **`/media/downloads`**.

Persistent data:
- Flask SQLite and app data: `./data/app-data`
- Jellyfin config: `./data/jellyfin-config`
- Jellyfin cache: `./data/jellyfin-cache`

Logs: `./scripts/setup-jellyfin.sh logs` (or `logs-flask` / `logs-jellyfin`).
