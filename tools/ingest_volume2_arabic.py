#!/usr/bin/env python3
"""Research-only source inventory for Madarij as-Salikin vol.2, 2019 edition.

Pages 1..659 belong to the printed volume; source PDF has 675 leaves including
end matter. Arabic is public-domain author text; contemporary edition footnotes
are deliberately excluded. Nothing produced by this script is a verified
Russian translation or religious citation verification.
"""
import argparse
import concurrent.futures as futures
import hashlib
from html.parser import HTMLParser
import json
from pathlib import Path
import re
import threading
import time
from urllib.request import Request, urlopen

BASE = "https://maknoon.org/ai/view.php?bk=madarej_2&p={}"
SCAN = "https://maknoon.org/ai/bk/madarej_2/{}.png"
PDF = "https://d1.islamhouse.com/data/ar/ih_books/single-03/ar-Madarij-alSalikee-02.pdf"
lock = threading.Lock()
next_request = 0.0

class Caption(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.in_cap = False
        self.chunks = []
        self.headings = []
        self.in_h4 = False
        self.h4_bits = []
    def handle_starttag(self, tag, attrs):
        if tag == "figcaption" and dict(attrs).get("id") == "cap":
            self.in_cap = True
        elif self.in_cap:
            if tag in ("br", "p", "h4"):
                self.chunks.append("\n")
            if tag == "h4":
                self.in_h4 = True
                self.h4_bits = []
    def handle_endtag(self, tag):
        if not self.in_cap:
            return
        if tag == "h4":
            self.headings.append("".join(self.h4_bits).strip())
            self.in_h4 = False
            self.chunks.append("\n")
        elif tag == "figcaption":
            self.in_cap = False
        elif tag in ("p", "div"):
            self.chunks.append("\n")
    def handle_data(self, data):
        if self.in_cap:
            self.chunks.append(data)
            if self.in_h4:
                self.h4_bits.append(data)

def decode_page(html, number):
    parser = Caption()
    parser.feed(html)
    lines = [re.sub(r"[\u200e\u200f\ufeff]", "", line).strip()
             for line in "".join(parser.chunks).splitlines() if line.strip()]
    # In this PDF, the first couple of pages may contain title material without
    # searchable captions. Preserve them as images, not imaginary author prose.
    if not parser.headings and number in (1, 2):
        return {"printed_page": number, "status": "front_matter_image_only",
                "arabic_body": "", "translation_status": "not_applicable",
                "religious_citations_status": "not_applicable",
                "source_image": SCAN.format(number)}
    if not parser.headings or "مدارج السالكين" not in parser.headings[0]:
        raise ValueError("source or edition mismatch / missing caption")
    if not any(re.fullmatch(r"الصفحة\s+" + str(number), l) for l in parser.headings):
        raise ValueError("page number missing from caption")
    lines = [line for line in lines if line not in parser.headings]
    index = next((i for i, line in enumerate(lines) if re.fullmatch(r"[-—ـ]{5,}", line)), len(lines))
    body = "\n".join(lines[:index]).strip()
    notes = "\n".join(lines[index+1:]).strip() if index < len(lines) else ""
    return {"printed_page": number, "source_image": SCAN.format(number),
            "source_html": BASE.format(number), "arabic_body": body,
            "body_sha256": hashlib.sha256(body.encode()).hexdigest(),
            "arabic_letters": len(re.findall(r"[ء-ي]", body)),
            "editor_footnotes_excluded": bool(notes),
            "editor_footnotes_sha256": hashlib.sha256(notes.encode()).hexdigest() if notes else None,
            "footnote_refs_for_review": body.count("¬"),
            "status": "needs_visual_collation", "translation_status": "untranslated",
            "religious_citations_status": "not_independently_verified"}

def paced():
    global next_request
    with lock:
        now = time.monotonic()
        wait = max(0, next_request - now)
        next_request = max(now, next_request) + 0.65
    if wait:
        time.sleep(wait)

def fetch_page(n):
    for attempt in range(4):
        paced()
        try:
            request = Request(BASE.format(n), headers={"User-Agent": "Madarij-Editorial-Study/1.0",
                           "Accept": "text/html"})
            with urlopen(request, timeout=40) as response:
                html = response.read().decode("utf-8", errors="replace")
            return decode_page(html, n)
        except Exception as exc:
            issue = "{}: {}".format(type(exc).__name__, str(exc)[:160])
            time.sleep((attempt+1)*2)
    return {"printed_page": n, "status": "failed", "error": issue}

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--start", type=int, default=1)
    ap.add_argument("--end", type=int, default=659)
    ap.add_argument("--output", default="volume2-source-audit")
    args = ap.parse_args()
    if not (1 <= args.start <= args.end <= 659):
        ap.error("pages outside volume two")
    root = Path(args.output)
    root.mkdir(parents=True, exist_ok=True)
    with futures.ThreadPoolExecutor(max_workers=3) as pool:
        rows = sorted(pool.map(fetch_page, range(args.start, args.end+1)),
                      key=lambda v:v["printed_page"])
    output = root / "arabic_pages.jsonl"
    output.write_text("".join(json.dumps(row, ensure_ascii=False)+"\n" for row in rows),
                      encoding="utf-8")
    failed = [row["printed_page"] for row in rows if row["status"] == "failed"]
    blank = [row["printed_page"] for row in rows
             if row["status"] == "needs_visual_collation" and not row.get("arabic_body")]
    report = {
        "print_edition": "Nabil bin Nassar al-Sindi, Dar Ataat al-Ilm / Dar Ibn Hazm, 2019",
        "source_pdf": PDF, "source_text": BASE.format("{page}"),
        "pages_expected": args.end-args.start+1,
        "pages_retrieved": len(rows)-len(failed), "missing_pages": failed,
        "empty_body_pages_to_review": blank,
        "arabic_characters": sum(row.get("arabic_letters", 0) for row in rows),
        "pages_with_separated_editorial_notes": sum(bool(row.get("editor_footnotes_excluded")) for row in rows),
        "arabic_pages_visually_verified": 0,
        "translated_pages": 0, "hadith_verified_pages": 0, "quran_verified_pages": 0,
        "status": "source_inventory_only",
        "publishable": False,
        "note": "Physical page endings are not author paragraph boundaries; do not concatenate blindly. Editorial notes are not included in Arabic author body. Manual collation with source PDF required."
    }
    (root/"source_audit.json").write_text(json.dumps(report, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False))
    if failed:
        raise SystemExit("Incomplete Arabic source: missing printed pages")

if __name__ == "__main__":
    main()
