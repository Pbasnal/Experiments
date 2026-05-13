"""
AnimePahe Downloader

A Python application for downloading anime episodes from AnimePahe.

Vendored for Docker: GUI import is optional (PyQt6 not installed).
Upstream animepahe-dl==5.10.0 — refresh this file if you bump the package pin.
"""

__version__ = "5.10.0"

# Main entry points
from .main import main
from .cli import cli_main, run_interactive_mode

try:
    from .gui import run_gui
except ImportError:
    run_gui = None

__all__ = ["main", "cli_main", "run_interactive_mode", "run_gui"]
