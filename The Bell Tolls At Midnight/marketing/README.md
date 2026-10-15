# Marketing Teaser — *The Bell Tolls at Midnight*

Dark fantasy teaser (~66s): AI stills + spoken narration + title CTA.

## Deliverable

- **Video:** `The_Bell_Tolls_at_Midnight_Teaser.mp4` (1920×1080, H.264 + AAC)
- **Script:** `narration_script.md` (spoiler-safe; secrets blacklisted)
- **Stills:** `images/01`–`09`
- **Audio:** `audio/narration.mp3` (en-GB-ThomasNeural, slowed slightly)

## What the ad sells (surface only)

Dying coastal town · salt wards · disappearances · the tower on the hill · solstice midnight · five adventurers · invitation to read when the story is complete.

**Intentionally omitted:** anything beneath the tower, the true nature of the “witch,” romantic history tied to the bell, church cover-up specifics, and other PDF lore secrets.

## Rebuild

```bash
# Requires: ffmpeg, python3 + pillow, edge-tts
export PATH="$HOME/.local/bin:$PATH"
python3 build_teaser.py
```

Voice, timings, and image list live in `build_teaser.py`.
