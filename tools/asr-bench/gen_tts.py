"""Generate benchmark audio clips with edge-tts: a Russian voice and a multilingual one.

Usage: python -I gen_tts.py sentences.json <out_dir>   (needs: pip install edge-tts)
"""
import asyncio
import json
import os
import sys

import edge_tts

VOICES = ["ru-RU-SvetlanaNeural", "en-US-AndrewMultilingualNeural"]


async def main() -> None:
    sentences = json.load(open(sys.argv[1], encoding="utf-8"))
    out_dir = sys.argv[2]
    os.makedirs(out_dir, exist_ok=True)
    for s in sentences:
        for voice in VOICES:
            short = voice.split("-")[2].replace("Neural", "").lower()
            path = os.path.join(out_dir, f"{s['id']}_{short}.mp3")
            if os.path.exists(path) and os.path.getsize(path) > 0:
                continue
            await edge_tts.Communicate(s["say"], voice).save(path)
            print("ok", path, flush=True)


asyncio.run(main())
