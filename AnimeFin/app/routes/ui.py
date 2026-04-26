from __future__ import annotations

from flask import Blueprint, current_app, render_template


ui_bp = Blueprint("ui", __name__)


@ui_bp.get("/")
def index():
    cfg = current_app.config["APP_CONFIG"]
    return render_template("index.html", downloads_dir=str(cfg.downloads_dir))


@ui_bp.get("/downloads")
def downloads_page():
    cfg = current_app.config["APP_CONFIG"]
    return render_template("downloads.html", downloads_dir=str(cfg.downloads_dir))
