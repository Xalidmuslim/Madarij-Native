#!/usr/bin/env python3
"""Check volume 3 data integrity; --require-complete rejects partial work.

No mechanical check certifies Russian prose, religious citations or fidelity.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "research/volume3"


def require(condition, message):
    if not condition:
        raise ValueError(message)


def sha(text):
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def ranges(numbers):
    result = []
    for n in numbers:
        if result and n == result[-1][-1] + 1:
            result[-1].append(n)
        else:
            result.append([n])
    return ", ".join(str(g[0]) if len(g) == 1 else f"{g[0]}–{g[-1]}" for g in result)


def validate(data=DATA, require_complete=False):
    protection = json.loads((data / "protected-assets.json").read_text())
    for file, digest in protection["sha256"].items():
        require(hashlib.sha256((ROOT / file).read_bytes()).hexdigest() == digest,
                f"Protected volume 1/2 asset changed: {file}")
    pages = list(map(json.loads, (data / "source-pages.jsonl").read_text().splitlines()))
    numbers = [r["printed_page"] for r in pages]
    require(numbers == sorted(set(numbers)), "Duplicate or unordered source pages")
    require(all(type(n) is int and 3 <= n <= 584 for n in numbers), "Invalid page number")
    source = {}
    for row in pages:
        n = row["printed_page"]
        require(row["paragraphs"], f"Empty source page {n}")
        for i, p in enumerate(row["paragraphs"], 1):
            require(p["id"] == f"v3-p{n:03d}-{i:02d}", "Source ID/paragraph order mismatch")
            require(p["id"] not in source, "Duplicate source paragraph ID")
            require(p["ar"].strip() and sha(p["ar"]) == p["sha256"], f"Source hash mismatch: {p['id']}")
            source[p["id"]] = p
    translation = json.loads((data / "translation.json").read_text())
    require(translation["volume"] == 3, "Wrong volume")
    require(translation["publication_ready"] is False and translation["translation_independently_verified"] is False,
            "Partial self-reviewed work must not be certified")
    used = []
    translation_ids = set()
    for p in translation["paragraphs"]:
        refs = p["source_ids"]
        require(refs and all(s in source for s in refs), "Missing source paragraph or empty source refs")
        require(p["id"] == refs[0] + "-ru" and p["id"] not in translation_ids, "Translation ID mismatch or duplicate")
        translation_ids.add(p["id"])
        expected_pages = sorted({int(s.split("-")[1][1:]) for s in refs})
        require(p["printed_pages"] == expected_pages, f"Printed page identifiers mismatch: {refs}")
        require(isinstance(p["ru"], str) and p["ru"].strip(), f"Empty translation: {refs}")
        require("\ufffd" not in p["ru"] and not re.search(r"-\s*\n", p["ru"]), f"Damaged Russian text: {refs}")
        if "ar" in p:
            require(p["ar"] == " ".join(source[s]["ar"] for s in refs), f"Arabic mismatch: {refs}")
        for verse in re.findall(r"\{[^{}]+\}", " ".join(source[s]["ar"] for s in refs)):
            require(verse in p["ru"], f"Arabic Quran quotation missing/changed in Russian: {refs}")
        used.extend(refs)
    require(len(used) == len(set(used)), "Duplicate translated source paragraphs")
    # Drafting is strictly sequential. Extra boundary-source pages are allowed.
    require(used == list(source)[:len(used)], "Translation skips or reorders source paragraphs")
    translated = [r["printed_page"] for r in pages
                  if all(p["id"] in used for p in r["paragraphs"])]
    if used:
        require(numbers[:len(translated)] == list(range(3, 3 + len(translated))),
                "Missing source pages inside the translated prefix")
    review_file = data / "self-review.json"
    reviewed = 0
    literary_reviewed = 0
    if review_file.exists():
        review = json.loads(review_file.read_text())
        require(review["external_independent_review"] is False, "Unsupported independent-review claim")
        for p in translation["paragraphs"]:
            key = "+".join(p["source_ids"])
            if key not in review["paragraphs"]:
                continue
            evidence = review["paragraphs"][key]
            ar = " ".join(source[s]["ar"] for s in p["source_ids"])
            require(evidence["sha256"] == sha(ar + "\0" + p["ru"]), f"Stale review: {key}")
            stages = evidence["stages"]
            require(set(stages) == {"semantic", "literary", "back_comparison"}, f"Missing review stage: {key}")
            require(stages["literary"] == "self_checked_russian_only", f"Unsupported literary status: {key}")
            require(all(stages[s] in {"self_checked_against_web_transcription", "pending_contextual_review"}
                        for s in ["semantic", "back_comparison"]), f"Unsupported review status: {key}")
            literary_reviewed += 1
            if all(stages[s] == "self_checked_against_web_transcription" for s in ["semantic", "back_comparison"]):
                reviewed += 1
    missing_source = sorted(set(range(3, 585)) - set(numbers))
    missing_translation = sorted(set(range(3, 585)) - set(translated))
    report = {"source_pages_captured": len(pages), "translated_pages": translated,
              "translation_paragraphs": len(translation["paragraphs"]),
              "self_reviewed_paragraphs": reviewed, "visually_collated_pages": 0,
              "literary_self_reviewed_paragraphs": literary_reviewed,
              "missing_source_pages": ranges(missing_source),
              "missing_translation_pages": ranges(missing_translation),
              "protected_assets_unchanged": len(protection["sha256"]),
              "complete": not missing_source and not missing_translation,
              "publication_ready": False, "app_integration_checked": False}
    if require_complete:
        require(report["complete"], "Volume INCOMPLETE; missing translations: " + ranges(missing_translation))
        require(reviewed == len(translation["paragraphs"]), "Missing self-review evidence")
    return report


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--require-complete", action="store_true")
    args = ap.parse_args()
    try:
        print(json.dumps(validate(require_complete=args.require_complete), ensure_ascii=False, indent=2))
    except (ValueError, KeyError, TypeError, OSError, json.JSONDecodeError) as exc:
        ap.exit(1, f"FAIL: {exc}\n")


if __name__ == "__main__":
    main()
