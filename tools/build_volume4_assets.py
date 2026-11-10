#!/usr/bin/env python3
"""Package the preserved fourth-volume draft for the existing native reader.

The 25-page navigation ranges are editorial browsing aids, NOT invented
author chapters. No translation, Arabic text, or protected earlier volume
is edited. The draft is not certified for publication.
"""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "research/volume4"
ASSETS = ROOT / "app/src/main/assets"
EDITION = "مدارج السالكين، ج 4، دار عطاءات العلم / دار ابن حزم، 1441/2019"


def require(condition, message):
    if not condition:
        raise ValueError(message)


def build():
    source = json.loads((DATA / "translation.json").read_text(encoding="utf-8"))
    require(source["volume"] == 4 and not source["publication_ready"]
            and not source["translation_independently_verified"],
            "Volume 4 must remain an unverified working draft")
    units = source["paragraphs"]
    require(bool(units), "No author paragraphs")
    require(len({p["id"] for p in units}) == len(units), "Duplicate translation ID")
    require(all(p["ar"].strip() and p["ru"].strip() and p["source_ids"]
                and p["printed_pages"] for p in units), "Incomplete paragraph")
    pages = {n for p in units for n in p["printed_pages"]}
    require(pages == set(range(3, 557)), "Printed page coverage differs from 3–556")
    seen_sources = [s for p in units for s in p["source_ids"]]
    require(len(seen_sources) == len(set(seen_sources)), "Repeated original fragment")
    require([p["printed_pages"][0] for p in units] ==
            sorted(p["printed_pages"][0] for p in units), "Author order changed")

    chapters, toc = [], []
    # Browse by printed page ranges until the source's author-chapter titles
    # undergo independent verification. Do NOT label these ranges as chapters.
    for start in range(3, 557, 25):
        end = min(start + 24, 556)
        body = [p for p in units if start <= p["printed_pages"][0] <= end]
        if not body:
            continue
        chapter_id = f"madarij-v4-p{start:03d}-p{end:03d}-draft"
        page_numbers = sorted({page for p in body for page in p["printed_pages"]})
        title = f"Страницы {start}–{end}"
        paragraphs, topics = [], []
        for p in body:
            status = ("draft_self_checked" if p["draft_status"] == "self_reviewed"
                      else "draft_unchecked")
            require(p["draft_status"] in
                    {"self_reviewed", "self_reviewed_context_question_open"},
                    f"Unknown draft status {p['id']}")
            origin = (EDITION + "; с. " +
                      ", ".join(str(n) for n in p["printed_pages"]) +
                      "; " + "+".join(p["source_ids"]) +
                      "; рабочий перевод ИИ, независимая редактура отсутствует")
            paragraph = {**p, "order": len(paragraphs), "role": "author",
                         "status": status, "source": origin}
            paragraphs.append(paragraph)
            if p["ar"].strip() == "فصل" and len(paragraphs) != 1:
                topics.append({
                    "paragraphId": p["id"],
                    "title": f"Раздел · с. {p['printed_pages'][0]}",
                    "provenance": "Авторский заголовок فصل; навигация по печатной странице."
                })
        chapters.append({
            "id": chapter_id, "volume": 4, "order": len(chapters),
            "title": title, "printed_pages": page_numbers,
            "source": EDITION + f"; страницы {start}–{end}. Черновой перевод; независимая редактура не выполнена.",
            "expected_paragraph_ids": [p["id"] for p in paragraphs],
            "paragraphs": paragraphs
        })
        toc.append({
            "chapterId": chapter_id, "title": title,
            "subtitle": f"Том 4 · черновик · страницы {start}–{end}",
            "entryParagraphId": paragraphs[0]["id"],
            "topics": topics,
            "entryNote": "Навигация по диапазонам страниц, а не авторские главы."
        })
    require([p["id"] for c in chapters for p in c["paragraphs"]] ==
            [p["id"] for p in units], "Translation paragraphs changed order")
    bundle = {
        "book": "Мадаридж ас-саликин — Степени идущих",
        "volume": 4, "source_edition": EDITION,
        "source_url": "https://ibnelqayem.com/ar/books/mdarj-alsalkyn-j4-62",
        "author_printed_pages": list(range(3, 557)),
        "translation_completeness": "full_author_body_working_draft_pages_3_to_556",
        "publication_ready": False, "translation_independently_verified": False,
        "editorial_footnotes_imported": False,
        "edition": {
            "draft": True, "author_text_rights": "public_domain",
            "translation_rights": "own_draft_not_independently_checked",
            "structure_verified": False, "rights_status": "pending_editorial_review"
        },
        "expected_chapter_ids": [c["id"] for c in chapters], "chapters": chapters
    }
    structure = {"groups": [{
        "id": "volume4-2019-working-draft", "title": "Том 4 · черновик",
        "provenance": "Рабочий перевод авторского текста. Группы по страницам — не авторские главы; независимая редактура не выполнена.",
        "items": toc
    }]}
    return bundle, structure


def validate(bundle, structure):
    source = json.loads((DATA / "translation.json").read_text(encoding="utf-8"))
    original = source["paragraphs"]
    paragraphs = [p for c in bundle["chapters"] for p in c["paragraphs"]]
    require(len(paragraphs) == len(original), "Missing volume 4 paragraphs")
    for left, right in zip(paragraphs, original):
        for field in ("id", "ar", "ru", "source_ids", "printed_pages"):
            require(left[field] == right[field], f"Author text changed: {right['id']} / {field}")
    require(not bundle["publication_ready"] and
            not bundle["translation_independently_verified"],
            "Unverified draft must not be advertised as publication ready")
    require([c["id"] for c in bundle["chapters"]] ==
            bundle["expected_chapter_ids"], "Chapter order altered")
    by_chapter = {c["id"]: {p["id"] for p in c["paragraphs"]} for c in bundle["chapters"]}
    items = [i for g in structure["groups"] for i in g["items"]]
    require([item["chapterId"] for item in items] ==
            bundle["expected_chapter_ids"], "Table of contents mismatch")
    for item in items:
        ids = [item["entryParagraphId"]] + [t["paragraphId"] for t in item["topics"]]
        require(all(i in by_chapter[item["chapterId"]] for i in ids),
                "Broken reader paragraph anchor")
    # Guard the preservation contract for the first three volumes.
    protected = json.loads((DATA / "protected-volumes.json").read_text(encoding="utf-8"))
    for path, expected in protected["sha256"].items():
        actual = hashlib.sha256((ROOT / path).read_bytes()).hexdigest()
        require(actual == expected, f"Earlier volume altered: {path}")
    return {
        "volume": 4, "author_pages": 554, "author_paragraphs": len(paragraphs),
        "navigation_ranges": len(bundle["chapters"]),
        "open_review_paragraphs": sum(
            p["status"] == "draft_unchecked" for p in paragraphs),
        "publication_ready": False, "independent_human_review": False,
        "protected_earlier_volumes": len(protected["sha256"])
    }


def main():
    bundle, structure = build()
    report = validate(bundle, structure)
    ASSETS.mkdir(parents=True, exist_ok=True)
    for filename, obj in (
        ("volume4-draft.json", bundle),
        ("volume4-structure.json", structure)
    ):
        (ASSETS / filename).write_text(
            json.dumps(obj, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8"
        )
    print(json.dumps(report, ensure_ascii=False))


if __name__ == "__main__":
    main()
