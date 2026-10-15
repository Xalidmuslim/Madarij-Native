#!/usr/bin/env python3
"""Validate both reader tables of contents against the packaged source texts.

This checks referential integrity and exact intra-paragraph landmarks.
A mechanical check cannot certify editorial topic titles as scholarly headings.
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "app/src/main/assets"


def read(filename: str) -> dict:
    return json.loads((ROOT / filename).read_text(encoding="utf-8"))


def run() -> None:
    corpus = [read("corpus.json"), read("volume2-preview.json")]
    structures = [read("structure.json"), read("volume2-structure.json")]
    paragraphs = {}
    chapter_paragraphs = {}
    for volume, book in enumerate(corpus, 1):
        for chapter in book["chapters"]:
            ids = set()
            for p in chapter["paragraphs"]:
                pid = p["id"]
                assert pid not in paragraphs, f"Duplicate paragraph ID: {pid}"
                assert p.get("ru", "").strip(), f"Missing Russian text: {pid}"
                paragraphs[pid] = (volume, p["ru"])
                ids.add(pid)
            chapter_paragraphs[chapter["id"]] = ids

    seen = set()
    topics = 0
    exact = 0
    counts = [0, 0]
    for volume, struct in enumerate(structures, 1):
        for group in struct["groups"]:
            for chapter in group["items"]:
                assert chapter["chapterId"] in chapter_paragraphs, (
                    f"TOC chapter missing in corpus: {chapter['chapterId']}"
                )
                entry = chapter.get("entryParagraphId")
                if entry:
                    assert entry in paragraphs, f"Missing TOC entry: {entry}"
                    assert paragraphs[entry][0] == volume, f"Wrong-volume entry: {entry}"
                for topic in chapter.get("topics", []):
                    pid = topic["paragraphId"]
                    title = topic["title"].strip()
                    assert title, f"Empty topic title: {pid}"
                    assert pid in paragraphs, f"TOC target missing in corpus: {pid}"
                    source_volume, russian = paragraphs[pid]
                    assert source_volume == volume, f"Wrong-volume target: {pid}"
                    key = (volume, pid)
                    assert key not in seen, f"Ambiguous multiple topic titles at {pid}"
                    seen.add(key)
                    topics += 1
                    counts[volume - 1] += 1
                    landmark = topic.get("startText", "").strip()
                    if landmark:
                        occurrence = russian.casefold().count(landmark.casefold())
                        assert occurrence == 1, (
                            f"Landmark must occur once in author text: {pid} / "
                            f"{landmark!r} / count={occurrence}"
                        )
                        exact += 1

    assert counts == [79, 150], f"Unexpected TOC coverage: {counts}"
    assert exact >= 93, f"Precise landmarks lost: {exact} (expected >= 93)"
    print(
        f"PASS: {topics} topic links, {counts[0]} in I and {counts[1]} in II; "
        f"{exact} unique exact-word landmarks, zero missing targets."
    )
    print("NOTE: semantic title correspondence still requires editorial review.")


if __name__ == "__main__":
    run()
