#!/usr/bin/env python3
"""Embed licensed, pinned Cyrillic handwriting typefaces for Android builds.

Fonts: Marck Script (legacy fallback) and Bad Script (v1.44 quotation face).
Both are obtained from Google Fonts and verified against the published Git
blob checksum (SHA-1 with the Git blob prefix). No runtime network dependency.
License copies are included under app/src/main/assets/fonts/.
"""
from pathlib import Path
import hashlib
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
FONTS = (
    ("marck_script.ttf",
     "024294bb007f565dab86593181b4a86aeba56338", 83664,
     "https://raw.githubusercontent.com/google/fonts/main/ofl/marckscript/MarckScript-Regular.ttf"),
    ("bad_script.ttf",
     "6a2aabeb4b5f7b2d6885fb8b39241c310e383528", 184216,
     "https://raw.githubusercontent.com/google/fonts/main/ofl/badscript/BadScript-Regular.ttf"),
)

def verified(data: bytes, expected_sha: str, expected_size: int) -> bool:
    actual = hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
    return len(data) == expected_size and actual == expected_sha

def prepare(name: str, expected_sha: str, size: int, url: str) -> None:
    dest = ROOT / "app/src/main/res/font" / name
    if dest.exists() and verified(dest.read_bytes(), expected_sha, size):
        print("Verified cached Cyrillic handwriting font:", name)
        return
    for attempt in range(3):
        try:
            request = urllib.request.Request(url, headers={"User-Agent": "Madarij-Android-Build/1.44"})
            with urllib.request.urlopen(request, timeout=35) as source:
                payload = source.read()
            if not verified(payload, expected_sha, size):
                raise RuntimeError(f"Unexpected font content: {name}")
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_bytes(payload)
            print("Verified and embedded handwriting font:", name, len(payload))
            return
        except Exception:
            if attempt == 2:
                raise
            time.sleep(2 * (attempt + 1))

def main() -> None:
    for font in FONTS:
        prepare(*font)

if __name__ == "__main__":
    main()
