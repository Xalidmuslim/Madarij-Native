#!/usr/bin/env python3
"""Prepare a Russian-only Android asset bundle without destroying source texts.

The canonical bilingual JSON files remain in src/main/assets for Arabic
collation and protected-hash validation. Android packages ONLY the generated
assets. The first five Russian literary revisions are applied to the generated
first volume, with strict anchors and stable paragraph/chapter identifiers.
"""
from __future__ import annotations

import json
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/assets"
DEST = ROOT / "app/build/generated/russian-only-assets"
EDITIONS = ROOT / "research/editorial/volume1/batch01_pages003_034.json"

# Covers Arabic letters, vowel marks, presentation forms and numerals.
ARABIC = re.compile(r"[\u0600-\u06FF\u0750-\u077F\u0870-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]")
BRACED_ARABIC = re.compile(
    r"\{[^{}\n]*[\u0600-\u06FF\u0750-\u077F\u0870-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF][^{}\n]*\}[ \t]*(?:[—–-][ \t]*)?"
)
BOOKS = ("corpus.json", "volume2-preview.json", "volume3-draft.json", "volume4-draft.json")


def russian_only(value: str) -> str:
    """Delete Arabic script without paraphrasing or deleting its Russian translation."""
    if not ARABIC.search(value):
        return value
    value = BRACED_ARABIC.sub("", value)
    value = ARABIC.sub("", value)
    value = re.sub(r"«[ \t]*»|„[ \t]*“", "", value)
    value = re.sub(r"\([ \t]*\)|\[[ \t]*\]", "", value)
    value = re.sub(r"(:)[ \t]*[—–][ \t]*(?=[«„\"])", r"\1 ", value)
    value = re.sub(r"[ \t]+[—–][ \t]*(?=[,;:.!?])", "", value)
    value = re.sub(r"[ \t]+([,;:.!?])", r"\1", value)
    value = re.sub(r"[ \t]{2,}", " ", value)
    return value.strip()


def remove_arabic(node, key: str = ""):
    if isinstance(node, dict):
        return {
            name: ("" if name in ("ar", "sourceTitleAr", "source_title_ar")
                   else remove_arabic(value, name))
            for name, value in node.items()
        }
    if isinstance(node, list):
        return [remove_arabic(item, key) for item in node]
    if isinstance(node, str):
        return russian_only(node)
    return node


def apply_first_batch(book: dict, manifest: dict) -> int:
    scope = manifest["scope"]
    chapters = book["chapters"]
    section_ids = [chapter["id"] for chapter in chapters[:5]]
    if section_ids != scope["section_ids"] or (scope["first_page"], scope["last_page"]) != (3, 34):
        raise ValueError("Volume 1 first-five-section boundary changed; review required")
    originals = {p["id"]: p for chapter in chapters[:5] for p in chapter["paragraphs"]}
    changed = set()
    for edit in manifest["replacements"]:
        pid = edit["paragraphId"]
        if pid not in originals:
            raise ValueError(f"Edit outside first five sections: {pid}")
        p = originals[pid]
        before, after = edit["find"], edit["replace"]
        if not before or not after or p["ru"].count(before) != 1:
            raise ValueError(f"Literary correction anchor changed: {pid}, {before[:60]!r}")
        p["ru"] = p["ru"].replace(before, after, 1)
        if p["ru"].strip() == "":
            raise ValueError(f"Empty translated paragraph: {pid}")
        changed.add(pid)
    if len(manifest["replacements"]) != 21 or len(changed) != 20:
        raise ValueError("Incomplete or duplicated literary editorial batch")
    return len(changed)


def build(source: Path = SOURCE, target: Path = DEST, edits: Path = EDITIONS) -> dict:
    if source.resolve() == target.resolve() or source in target.parents:
        raise ValueError("Refusing to overwrite canonical bilingual originals")
    manifest = json.loads(edits.read_text(encoding="utf-8"))
    if target.exists():
        shutil.rmtree(target)
    shutil.copytree(source, target)
    if any(not (target / name).is_file() for name in BOOKS):
        raise ValueError("Missing one of four book assets; generate volume4 before packaging")

    first_volume = json.loads((target / "corpus.json").read_text(encoding="utf-8"))
    changed = apply_first_batch(first_volume, manifest)
    paragraphs = {}
    for name in BOOKS:
        path = target / name
        book = first_volume if name == "corpus.json" else json.loads(path.read_text(encoding="utf-8"))
        original_ids = [(ch["id"], [p["id"] for p in ch["paragraphs"]]) for ch in book["chapters"]]
        book = remove_arabic(book)
        assert [(ch["id"], [p["id"] for p in ch["paragraphs"]]) for ch in book["chapters"]] == original_ids
        assert all(p["ru"].strip() and not p["ar"] for ch in book["chapters"] for p in ch["paragraphs"])
        paragraphs[name] = sum(len(ch["paragraphs"]) for ch in book["chapters"])
        path.write_text(json.dumps(book, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    # The study corpus and structural metadata can also contain Arabic quotes.
    for path in target.rglob("*.json"):
        if path.name in BOOKS:
            continue
        data = json.loads(path.read_text(encoding="utf-8"))
        path.write_text(json.dumps(remove_arabic(data), ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    for path in target.rglob("*.json"):
        content = path.read_text(encoding="utf-8")
        if ARABIC.search(content):
            raise ValueError(f"Arabic script still present in Android asset: {path}")
    print(f"PASS: Russian-only packaged assets; {changed} edited paragraphs; all four volumes preserved: {paragraphs}")
    return paragraphs


if __name__ == "__main__":
    build()
