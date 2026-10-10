#!/usr/bin/env python3
"""Fetch the original Cyrillic Marck Script font only when building Android.

Source: Google Fonts (Google/fonts, ofl/marckscript/MarckScript-Regular.ttf).
License: SIL Open Font License 1.1; see assets/fonts/MarckScript-OFL.txt.
The exact upstream Git blob SHA-1 is pinned to prevent accidental substitution.
No runtime network dependency: the font is embedded in the resulting APK.
"""
from pathlib import Path
import hashlib
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / "app/src/main/res/font/marck_script.ttf"
BLOB = "024294bb007f565dab86593181b4a86aeba56338"
SIZE = 83664
URL = "https://raw.githubusercontent.com/google/fonts/main/ofl/marckscript/MarckScript-Regular.ttf"

def verified(data: bytes) -> bool:
    sha = hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
    return len(data) == SIZE and sha == BLOB

def main():
    if DEST.exists() and verified(DEST.read_bytes()):
        print("Verified cached Cyrillic handwriting font")
        return
    for retry in range(3):
        try:
            request = urllib.request.Request(URL, headers={"User-Agent": "Madarij-Android-Build/1.40"})
            with urllib.request.urlopen(request, timeout=25) as stream:
                data = stream.read()
            if not verified(data):
                raise RuntimeError("Font integrity mismatch (pinned Google Fonts Git blob)")
            DEST.parent.mkdir(parents=True, exist_ok=True)
            DEST.write_bytes(data)
            print("Verified and embedded Cyrillic handwriting font", len(data), "bytes")
            return
        except Exception:
            if retry >= 2:
                raise
            time.sleep(2 * (retry + 1))

if __name__ == "__main__":
    main()
