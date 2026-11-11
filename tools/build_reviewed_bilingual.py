#!/usr/bin/env python3
"""Reproducibly export the first volume as a complete Arabic/Russian comparison corpus.

No APK build, no deletion of source Arabic, no mutation of canonical assets.
The source corpus and ordered, anchored editorial batches stay in GitHub.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/assets/corpus.json"
MANIFEST_DIR = ROOT / "research/editorial/volume1"
OUTPUT = ROOT / "research/editorial/volume1/working-bilingual-corpus.json"


def reviewed_corpus(source: Path = SOURCE, manifest_dir: Path = MANIFEST_DIR) -> tuple[dict, dict]:
    original_bytes = source.read_bytes()
    original = json.loads(original_bytes)
    # Separate author chapters and paper-range navigation; never invent author chapters.
    section_ids = [c["id"] for c in original["chapters"]]
    paragraphs = {p["id"]: p for c in original["chapters"] for p in c["paragraphs"]}
    if len(paragraphs) != sum(len(c["paragraphs"]) for c in original["chapters"]):
        raise ValueError("Duplicate paragraph IDs in bilingual original")
    batches = sorted(manifest_dir.glob("batch[0-9][0-9]_pages*.json"))
    if not batches:
        raise ValueError("No literary editorial batches found")
    report = {"base_source_sha256": hashlib.sha256(original_bytes).hexdigest(),
              "sections": len(section_ids), "paragraphs": len(paragraphs), "batches": []}
    for path in batches:
        data = json.loads(path.read_text(encoding="utf-8"))
        scope = data["scope"]
        first, last = scope["first_section"], scope["last_section"]
        if section_ids[first-1:last] != scope["section_ids"] or last-first+1 != 5:
            raise ValueError(f"Navigation scope differs from approved five sections: {path}")
        allowed = {p["id"] for ch in original["chapters"][first-1:last]
                   for p in ch["paragraphs"]}
        if "reviewed_paragraph_ids" in data and set(data["reviewed_paragraph_ids"]) != allowed:
            raise ValueError(f"Reviewed paragraph manifest incomplete: {path}")
        touched = set()
        for edit in data["replacements"]:
            pid, old, new = edit["paragraphId"], edit["find"], edit["replace"]
            if pid not in allowed or not old or not new:
                raise ValueError(f"Invalid edit ID or empty replacement: {pid}")
            p = paragraphs[pid]
            if p["ru"].count(old) != 1:
                raise ValueError(f"Edit anchor stale or repeated in {pid}: {old[:70]}")
            p["ru"] = p["ru"].replace(old, new, 1)
            if not p["ru"].strip():
                raise ValueError(f"Empty Russian text after edit: {pid}")
            touched.add(pid)
        report["batches"].append({"file": path.name, "sections": [first,last],
                                  "replacements": len(data["replacements"]),
                                  "edited_paragraphs": len(touched),
                                  "reviewed_paragraphs": len(allowed)})
    if any(not p.get("ar", "").strip() and p.get("role") != "editor_note"
           for p in paragraphs.values()):
        raise ValueError("Author Arabic source lost")
    if any(not p["ru"].strip() for p in paragraphs.values()):
        raise ValueError("Russian translation lost")
    return original, report


def build(source: Path = SOURCE, manifest_dir: Path = MANIFEST_DIR, output: Path = OUTPUT):
    if source.resolve() == output.resolve():
        raise ValueError("Source and output must be separate")
    corpus, report = reviewed_corpus(source, manifest_dir)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(corpus, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    report["output_sha256"] = hashlib.sha256(output.read_bytes()).hexdigest()
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return report


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", type=Path, default=OUTPUT)
    args = parser.parse_args()
    build(output=args.out)
