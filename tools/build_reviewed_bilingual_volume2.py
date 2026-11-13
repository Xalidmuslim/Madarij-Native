#!/usr/bin/env python3
"""Rebuild volume 2 literary revision from immutable canonical bilingual text.

All edits are exact, ID-scoped, reviewed Russian substitutions. The source,
the Arabic original, quotation text, and reader metadata stay unchanged.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/assets/volume2-preview.json"
MANIFEST_DIR = ROOT / "research/editorial/volume2"
OUTPUT = MANIFEST_DIR / "working-bilingual-corpus.json"
MANIFEST_PATTERN = "batch[0-9][0-9]_pages*.json"


def quoted_text(value: str) -> list[str]:
    """Protect both common Russian quotation-mark conventions."""
    return re.findall(r"«[^»]*»|“[^”]*”", value, flags=re.S)


def source_pages(paragraph: dict) -> tuple[int, int]:
    match = re.search(r"стр\.\s*(\d+)(?:[–-](\d+))?", paragraph["source"])
    if not match:
        raise ValueError("Missing printed-page provenance: " + paragraph["id"])
    return int(match.group(1)), int(match.group(2) or match.group(1))


def reviewed_corpus(source: Path = SOURCE, manifest_dir: Path = MANIFEST_DIR) -> tuple[dict, dict]:
    source_bytes = source.read_bytes()
    sha = hashlib.sha256(source_bytes).hexdigest()
    revised = json.loads(source_bytes)
    chapters = revised["chapters"]
    if revised["volume"] != 2 or len(chapters) != 9:
        raise ValueError("Unexpected volume 2 canonical structure")
    paragraphs = [p for chapter in chapters for p in chapter["paragraphs"]]
    if len(paragraphs) != 676:
        raise ValueError("Volume 2 paragraph count changed")
    index = {p["id"]: n for n, p in enumerate(paragraphs)}
    if len(index) != len(paragraphs):
        raise ValueError("Duplicate author paragraph IDs")
    by_id = {p["id"]: p for p in paragraphs}
    used = set()
    manifests = sorted(manifest_dir.glob(MANIFEST_PATTERN))
    if not manifests:
        raise ValueError("No volume 2 literary review manifests")
    report = {
        "source_sha256": sha,
        "total_chapters": len(chapters),
        "total_paragraphs": len(paragraphs),
        "batches": []
    }
    for path in manifests:
        manifest = json.loads(path.read_text(encoding="utf-8"))
        if manifest.get("source") != "app/src/main/assets/volume2-preview.json":
            raise ValueError("Wrong volume 2 source file: " + path.name)
        if manifest.get("source_sha256") != sha:
            raise ValueError("Canonical volume 2 SHA changed: " + path.name)
        scope = manifest["scope"]
        ids = scope["paragraph_ids"]
        if not ids or ids != manifest["reviewed_paragraph_ids"] or len(ids) != len(set(ids)):
            raise ValueError("Incomplete or duplicated reviewed IDs: " + path.name)
        if any(pid not in index for pid in ids) or any(pid in used for pid in ids):
            raise ValueError("Unknown or overlapping reviewed paragraphs: " + path.name)
        positions = [index[pid] for pid in ids]
        if positions != list(range(positions[0], positions[-1] + 1)):
            raise ValueError("Editorial scope is not contiguous: " + path.name)
        actual_chapter_ids = [
            chapter["id"] for chapter in chapters
            if any(p["id"] in ids for p in chapter["paragraphs"])
        ]
        if actual_chapter_ids != scope["chapter_ids"]:
            raise ValueError("Chapter IDs changed: " + path.name)
        pages = [source_pages(by_id[pid]) for pid in ids]
        if (min(a for a, _ in pages), max(b for _, b in pages)) != (
            scope["first_page"], scope["last_page"]
        ):
            raise ValueError("Printed page boundaries changed: " + path.name)
        touched = set()
        for edit in manifest["replacements"]:
            pid, old, new = edit["paragraphId"], edit["find"], edit["replace"]
            if pid not in ids or not old or not new or not edit.get("reason"):
                raise ValueError("Out-of-scope or empty edit: " + pid)
            paragraph = by_id[pid]
            original_ru = paragraph["ru"]
            if original_ru.count(old) != 1:
                raise ValueError("Literary edit anchor stale or repeated: " + pid + " " + old[:80])
            if quoted_text(original_ru) != quoted_text(original_ru.replace(old, new, 1)):
                raise ValueError("Protected quotation changed: " + pid)
            paragraph["ru"] = original_ru.replace(old, new, 1)
            if not paragraph["ru"].strip():
                raise ValueError("Russian paragraph erased: " + pid)
            touched.add(pid)
        used.update(ids)
        report["batches"].append({
            "file": path.name,
            "first_page": scope["first_page"],
            "last_page": scope["last_page"],
            "reviewed_paragraphs": len(ids),
            "replacements": len(manifest["replacements"]),
            "edited_paragraphs": len(touched),
            "rejected_candidates": len(manifest.get("rejected_candidates", [])),
        })
    if any(not p.get("ar", "").strip() or not p.get("ru", "").strip() for p in paragraphs):
        raise ValueError("Arabic original or Russian text is missing")
    report["reviewed_paragraphs"] = len(used)
    report["replacements"] = sum(b["replacements"] for b in report["batches"])
    report["edited_paragraphs"] = sum(b["edited_paragraphs"] for b in report["batches"])
    return revised, report


def build(source: Path = SOURCE, manifest_dir: Path = MANIFEST_DIR,
          output: Path = OUTPUT) -> dict:
    if source.resolve() == output.resolve():
        raise ValueError("Refusing to overwrite canonical volume 2")
    reviewed, report = reviewed_corpus(source, manifest_dir)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(reviewed, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return report


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", type=Path, default=OUTPUT)
    args = parser.parse_args()
    build(output=args.out)
