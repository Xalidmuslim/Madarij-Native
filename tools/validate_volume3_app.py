#!/usr/bin/env python3
"""Validate the packaged reader corpus and TOC, not translation quality or UI."""
import json
from pathlib import Path

from tools.build_volume3_assets import build
from tools.validate_volume3 import validate as validate_translation, require

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"


def validate(bundle=None, structure=None):
    coverage = validate_translation()
    expected, expected_structure = build()
    if bundle is None:
        bundle = json.loads((ASSETS / "volume3-draft.json").read_text())
    if structure is None:
        structure = json.loads((ASSETS / "volume3-structure.json").read_text())
    require(coverage["complete"], "Incomplete volume 3 source/translation coverage")
    require(bundle["volume"] == 3 and bundle["publication_ready"] is False and
            bundle["translation_independently_verified"] is False, "Unsupported certification")
    chapters = bundle["chapters"]
    require([c["id"] for c in chapters] == bundle["expected_chapter_ids"] == expected["expected_chapter_ids"],
            "Missing, duplicate or reordered author chapter")
    old = [json.loads((ASSETS / f).read_text()) for f in ["corpus.json", "volume2-preview.json"]]
    old_chapters = {c["id"] for v in old for c in v["chapters"]}
    old_ids = {p["id"] for v in old for c in v["chapters"] for p in c["paragraphs"]}
    require(not old_chapters.intersection(bundle["expected_chapter_ids"]), "Chapter ID collision with volumes 1/2")
    ids, author, pages = set(), [], set()
    by_chapter = {}
    for n, c in enumerate(chapters):
        require(c["volume"] == 3 and c["order"] == n, "Invalid volume/chapter order")
        ps = c["paragraphs"]
        require([p["id"] for p in ps] == c["expected_paragraph_ids"] and ps, "Paragraph manifest mismatch")
        by_chapter[c["id"]] = {p["id"] for p in ps}
        for j, p in enumerate(ps):
            require(p["order"] == j and p["id"] not in ids and p["id"] not in old_ids,
                    "Duplicate, reordered or colliding paragraph")
            ids.add(p["id"])
            require(p["ru"].strip() and p["source"].strip(), "Empty reader text/source")
            require(p["status"] in {"draft_unchecked", "draft_self_checked"}, "Unsupported paragraph status")
            if p["role"] == "author":
                require(p["ar"].strip() and p["source_ids"], "Empty author original or page identifiers")
                author.append(p)
                pages.update(p["printed_pages"])
            else:
                require(p["role"] == "editor_note" and p["ar"] == "" and "source_ids" not in p,
                        "App note mixed into author text")
                require(j > 0 and ps[j-1]["id"] == p["related_author_paragraph_id"], "Detached app note")
    require(pages == set(range(3, 585)), "Missing printed pages")
    translation = json.loads((ROOT / "research/volume3/translation.json").read_text())["paragraphs"]
    require([p["id"] for p in author] == [p["id"] for p in translation], "Missing or reordered author paragraph")
    for actual, original in zip(author, translation):
        for key in ["ar", "ru", "source_ids", "printed_pages"]:
            require(actual[key] == original[key], f"Packaging changed {key}: {original['id']}")
    links = 0
    items = [item for g in structure["groups"] for item in g["items"]]
    require([i["chapterId"] for i in items] == bundle["expected_chapter_ids"], "TOC chapter order mismatch")
    for item in items:
        targets = [item["entryParagraphId"]] + [t["paragraphId"] for t in item["topics"]]
        require(len(set(targets)) == len(targets) and set(targets).issubset(by_chapter[item["chapterId"]]),
                "Missing, duplicated or cross-chapter TOC target")
        links += len(targets)
    # Full equality also guards lost fasl links, omitted own notes, metadata and
    # status changes that are not caught by the deliberately specific checks.
    require(bundle == expected, "Reader asset differs from the preserved translation/metadata")
    require(structure == expected_structure, "Missing or changed author heading/navigation metadata")
    return {"volume": 3, "author_pages": len(pages), "author_paragraphs": len(author),
            "app_notes": len(ids) - len(author), "chapters": len(chapters), "toc_targets": links,
            "unresolved_source_ids": bundle["unresolved_source_ids"],
            "protected_volume1_2_files_unchanged": coverage["protected_assets_unchanged"],
            "packaging_and_navigation_checked": True, "device_runtime_checked": False,
            "publication_ready": False, "independent_human_review": False}


if __name__ == "__main__":
    print(json.dumps(validate(), ensure_ascii=False, indent=2))
