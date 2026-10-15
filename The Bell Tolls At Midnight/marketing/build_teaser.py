#!/usr/bin/env python3
"""Rebuild the marketing teaser from images + narration audio."""

from __future__ import annotations

import os
import shutil
import subprocess
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent
IMG_DIR = ROOT / "images"
AUDIO = ROOT / "audio" / "narration.mp3"
OUT = ROOT / "The_Bell_Tolls_at_Midnight_Teaser.mp4"
WORK = Path("/tmp/bell_video")

W, H, FPS = 1920, 1080, 30
FADE = 0.45

# (image filename, duration seconds) — sync to audio/narration.vtt
SEGMENTS = [
    ("01-gravedawn-fog.jpg", 3.30),
    ("02-salt-circles.jpg", 5.40),
    ("03-tower-on-hill.jpg", 4.45),
    ("04-empty-door-lamp.jpg", 10.00),
    ("05-priests-and-exodus.jpg", 11.00),
    ("06-adventurers-road.jpg", 8.35),
    ("07-party-faces-tower.jpg", 5.60),
    ("08-midnight-bell.jpg", 7.40),
    ("09-title-card.jpg", 14.00),
]


def run(cmd: list[str]) -> None:
    subprocess.check_call(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def normalize_still(src: Path, dest: Path) -> None:
    im = Image.open(src).convert("RGB")
    scale = max(W / im.width, H / im.height)
    nw, nh = int(im.width * scale) + 2, int(im.height * scale) + 2
    im = im.resize((nw, nh), Image.Resampling.LANCZOS)
    left = (nw - W) // 2
    top = (nh - H) // 2
    im.crop((left, top, left + W, top + H)).save(dest, quality=95)


def make_clip(still: Path, clip: Path, dur: float, index: int) -> None:
    frames = max(int(round(dur * FPS)), 1)
    if index % 2 == 0:
        zexpr = f"min(1.0+0.08*{frames}/on,{1.08})"
    else:
        zexpr = f"if(eq(on,1),1.08,max(1.08-0.08*on/{frames},1.0))"
    vf = (
        f"scale=8000:-1,"
        f"zoompan=z='{zexpr}':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)'"
        f":d={frames}:s={W}x{H}:fps={FPS},format=yuv420p"
    )
    run(
        [
            "ffmpeg",
            "-y",
            "-loop",
            "1",
            "-i",
            str(still),
            "-vf",
            vf,
            "-t",
            f"{dur:.3f}",
            "-r",
            str(FPS),
            "-c:v",
            "libx264",
            "-pix_fmt",
            "yuv420p",
            "-preset",
            "veryfast",
            "-crf",
            "18",
            "-an",
            str(clip),
        ]
    )


def main() -> None:
    if not AUDIO.exists():
        raise SystemExit(f"Missing narration audio: {AUDIO}")

    if WORK.exists():
        shutil.rmtree(WORK)
    WORK.mkdir(parents=True)

    clip_paths: list[tuple[Path, float]] = []
    for i, (name, dur) in enumerate(SEGMENTS):
        still = WORK / f"still_{i:02d}.jpg"
        clip = WORK / f"clip_{i:02d}.mp4"
        normalize_still(IMG_DIR / name, still)
        make_clip(still, clip, dur, i)
        clip_paths.append((clip, dur))
        print(f"clip {i}: {name} ({dur:.2f}s)")

    offsets: list[float] = []
    acc = 0.0
    for i, (_, dur) in enumerate(clip_paths):
        if i == 0:
            acc = dur
            continue
        off = acc - FADE
        offsets.append(off)
        acc = off + dur

    fc: list[str] = []
    prev = "[0:v]"
    n = len(clip_paths)
    for i in range(1, n):
        out_label = f"[v{i}]" if i < n - 1 else "[vout]"
        fc.append(
            f"{prev}[{i}:v]xfade=transition=fade:duration={FADE}:offset={offsets[i - 1]:.3f}{out_label}"
        )
        prev = f"[v{i}]"
    filter_complex = ";".join(fc) + f";[{n}:a]apad[aout]"

    inputs: list[str] = []
    for path, _ in clip_paths:
        inputs += ["-i", str(path)]
    inputs += ["-i", str(AUDIO)]

    cmd = [
        "ffmpeg",
        "-y",
        *inputs,
        "-filter_complex",
        filter_complex,
        "-map",
        "[vout]",
        "-map",
        "[aout]",
        "-c:v",
        "libx264",
        "-pix_fmt",
        "yuv420p",
        "-preset",
        "medium",
        "-crf",
        "18",
        "-c:a",
        "aac",
        "-b:a",
        "192k",
        "-t",
        f"{acc:.3f}",
        "-movflags",
        "+faststart",
        str(OUT),
    ]
    # Show this final encode's progress
    subprocess.check_call(cmd)
    print(f"Wrote {OUT} ({acc:.1f}s)")


if __name__ == "__main__":
    main()
