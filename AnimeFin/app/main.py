from __future__ import annotations

from flask import Flask

from app.config import AppConfig, load_config
from app.routes.api import api_bp
from app.routes.ui import ui_bp
from app.services.anime_source import AnimeSourceService
from app.services.downloads import DownloadService
from app.storage.jobs import JobsStore
from app.storage.media import MediaStore


def create_app(config: AppConfig | None = None) -> Flask:
    cfg = config or load_config()
    app = Flask(
        __name__,
        template_folder=str(cfg.base_dir / "app" / "templates"),
        static_folder=str(cfg.base_dir / "app" / "static"),
        static_url_path="/static",
    )
    app.config["APP_CONFIG"] = cfg

    jobs_store = JobsStore(cfg.database_path)
    anime_source = AnimeSourceService(cfg.allanime_api, cfg.allanime_referer, cfg.user_agent)
    media_store = MediaStore(cfg.downloads_dir)
    downloads = DownloadService(jobs_store, cfg.downloads_dir, cfg.ani_cli_path)
    downloads.start()

    app.extensions["jobs_store"] = jobs_store
    app.extensions["anime_source"] = anime_source
    app.extensions["media"] = media_store
    app.extensions["downloads"] = downloads

    app.register_blueprint(api_bp)
    app.register_blueprint(ui_bp)
    return app


def main() -> None:
    cfg = load_config()
    app = create_app(cfg)
    app.run(host=cfg.host, port=cfg.port, debug=cfg.debug, threaded=True)


if __name__ == "__main__":
    main()
