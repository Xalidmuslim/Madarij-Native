#!/usr/bin/env python3
"""Extract author paragraphs from saved ibnelqayem volume 3 HTML, excluding notes.

Fetch HTML separately. This offline importer neither downloads nor certifies it.
Usage: python3 tools/volume3_source.py /path/to/page-3.html ...
"""
import argparse
import hashlib
from html.parser import HTMLParser
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "research/volume3/source-pages.jsonl"


def sha(text):
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


class AuthorText(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.body_depth = 0
        self.in_paragraph = False
        self.note_depth = 0
        self.bits = []
        self.paragraphs = []
        self.page_numbers = []
        self.excluded_note_refs = 0

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        classes = attrs.get("class", "").split()
        if tag == "input" and "page-input" in classes:
            self.page_numbers.append(int(attrs["value"]))
        if tag == "div":
            if self.body_depth:
                self.body_depth += 1
            elif "book-content-texts" in classes:
                self.body_depth = 1
        if not self.body_depth:
            return
        if tag == "p" and "selectionShareable" in classes:
            self.in_paragraph = True
            self.bits = []
        if self.in_paragraph and tag == "a":
            self.note_depth += 1
            self.excluded_note_refs += 1
        if self.in_paragraph and tag == "br":
            self.bits.append(" ")

    def handle_endtag(self, tag):
        if tag == "a" and self.note_depth:
            self.note_depth -= 1
        if tag == "p" and self.in_paragraph:
            value = re.sub(r"\s+", " ", "".join(self.bits)).strip()
            value = re.sub(r" +([،؛:.])", r"\1", value)
            if value:
                self.paragraphs.append(value)
            self.in_paragraph = False
        if tag == "div" and self.body_depth:
            self.body_depth -= 1

    def handle_data(self, data):
        if self.in_paragraph and not self.note_depth:
            self.bits.append(data)


def extract(html, number):
    parser = AuthorText()
    parser.feed(html)
    if parser.page_numbers != [number] or not 3 <= number <= 584:
        raise ValueError(f"Page identity mismatch: requested {number}, found {parser.page_numbers}")
    if not parser.paragraphs or not all(re.search(r"[ء-ي]", p) for p in parser.paragraphs):
        raise ValueError(f"Empty or non-Arabic author paragraph on page {number}")
    return {
        "printed_page": number,
        "source_url": f"https://ibnelqayem.com/ar/books/61?page={number}&is_intro=0",
        "paragraphs": [
            {"id": f"v3-p{number:03d}-{i:02d}", "ar": text, "sha256": sha(text)}
            for i, text in enumerate(parser.paragraphs, 1)
        ],
        "excluded_editorial_note_refs": parser.excluded_note_refs,
        "source_status": "web_transcription_not_visually_collated",
    }


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("html", nargs="+", type=Path)
    args = ap.parse_args()
    rows = {}
    if OUTPUT.exists():
        rows = {r["printed_page"]: r for r in map(json.loads, OUTPUT.read_text().splitlines())}
    for path in args.html:
        match = re.fullmatch(r"page-(\d+)\.html", path.name)
        if not match:
            ap.error(f"Expected page-N.html: {path}")
        number = int(match[1])
        row = extract(path.read_text(encoding="utf-8"), number)
        if number in rows and rows[number] != row:
            raise ValueError(f"Source changed on page {number}; collate variants before replacement")
        rows[number] = row
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text("".join(json.dumps(rows[n], ensure_ascii=False) + "\n" for n in sorted(rows)), encoding="utf-8")
    print(f"Saved {len(rows)} source pages; none certified against scans")


if __name__ == "__main__":
    main()
