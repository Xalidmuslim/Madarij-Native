#!/usr/bin/env python3
"""Package the existing translation without editing prose or certifying it.

The chapter boundaries below point to author introductions, not page blocks.
Unnumbered fasl headings remain in the text and become subordinate TOC links.
"""
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "research/volume3"
ASSETS = ROOT / "app/src/main/assets"
EDITION = "مدارج السالكين، ج 3، دار عطاءات العلم / دار ابن حزم، 1441/2019، تحقيق محمد عزير شمس"
# Source ID of the introductory paragraph, stable English slug, Russian title.
CHAPTERS = [
    ("v3-p003-02", "ithar", "Предпочтение других себе"),
    ("v3-p024-02", "khuluq", "Добрый нрав"),
    ("v3-p065-02", "tawadu", "Смирение"),
    ("v3-p086-02", "futuwwa", "Благородство — футувва"),
    ("v3-p104-02", "murua", "Нравственное достоинство — муруа"),
    ("v3-p108-02", "basata", "Непринуждённость"),
    ("v3-p116-02", "azm", "Решимость"),
    ("v3-p122-02", "irada", "Стремление"),
    ("v3-p140-02", "adab", "Учтивость — адаб"),
    ("v3-p170-02", "yaqin", "Убеждённость"),
    ("v3-p184-02", "uns", "Отрада близости к Аллаху"),
    ("v3-p207-02", "dhikr", "Поминание"),
    ("v3-p231-02", "faqr", "Нужда в Аллахе"),
    ("v3-p248-02", "ghina", "Возвышенное богатство"),
    ("v3-p254-02", "murad", "Тот, кого избрали"),
    ("v3-p263-02", "ihsan", "Ихсан"),
    ("v3-p270-02", "ilm", "Знание"),
    ("v3-p292-02", "hikma", "Мудрость"),
    ("v3-p300-02", "firasa", "Проницательность"),
    ("v3-p319-02", "tazim", "Почитание"),
    ("v3-p330-02", "ilham", "Вдохновение"),
    ("v3-p331-02", "sakina", "Спокойствие — сакина"),
    ("v3-p347-02", "tumanina", "Умиротворённость"),
    ("v3-p360-02", "himma", "Высшее устремление"),
    ("v3-p365-02", "mahabba", "Любовь"),
    ("v3-p419-02", "ghayra", "Ревность"),
    ("v3-p432-02", "shawq", "Томление"),
    ("v3-p455-02", "wajd", "Сердечное переживание — ваджд"),
    ("v3-p484-02", "dhawq", "Вкушение — заук"),
    ("v3-p503-02", "lahz", "Мимолётный взгляд"),
    ("v3-p544-02", "waqt", "Время"),
    ("v3-p566-02", "safa", "Чистота"),
]


def heading(p):
    return bool(re.fullmatch(r"فصل(?:[\u064b-\u065f\u0670ٌ\s].*)?", p["ar"])) and len(p["ar"]) < 80


def build():
    translation = json.loads((DATA / "translation.json").read_text())
    reviews = json.loads((DATA / "self-review.json").read_text())["paragraphs"]
    units = translation["paragraphs"]
    index = {ref: i for i, u in enumerate(units) for ref in u["source_ids"]}
    boundaries = []
    for ref, slug, title in CHAPTERS:
        pos = index[ref]
        assert pos > 0 and heading(units[pos - 1]), ref
        boundaries.append((pos - 1, slug, title))
    assert boundaries[0][0] == 0
    assert [b[0] for b in boundaries] == sorted({b[0] for b in boundaries})
    chapters, items, pending = [], [], []
    for n, (start, slug, title) in enumerate(boundaries):
        end = boundaries[n + 1][0] if n + 1 < len(boundaries) else len(units)
        body = units[start:end]
        pages = sorted({page for u in body for page in u["printed_pages"]})
        cid = f"madarij-v3-p{pages[0]:03d}-{slug}"
        page_label = f"с. {pages[0]}–{pages[-1]}"
        source = f"{EDITION}; {page_label}. Контроль: PDF 2019 года. Рабочий перевод и самопроверка ИИ; независимая редактура не выполнена."
        paragraphs, topics = [], []
        for u in body:
            key = "+".join(u["source_ids"])
            unresolved = "pending_contextual_review" in reviews[key]["stages"].values()
            if unresolved:
                pending.append(key)
            ps = u["printed_pages"]
            origin = f"{EDITION}; с. " + ", ".join(map(str, ps)) + "; " + key
            p = dict(u, order=len(paragraphs), role="author",
                     status="draft_unchecked" if unresolved else "draft_self_checked",
                     source=origin)
            # Notes are distinct reader records; never merge them into author prose.
            notes = p.pop("editorial_notes", [])
            paragraphs.append(p)
            if heading(u) and u is not body[0]:
                label = u["ru"] if u["ru"] != "Раздел" else "Раздел"
                topics.append({"paragraphId": u["id"], "title": f"{label} · с. {ps[0]}",
                               "provenance": "Авторское деление فصل; номер страницы добавлен для навигации."})
            for j, note in enumerate(notes, 1):
                paragraphs.append({"id": u["id"] + f"-editor-note-{j}",
                                   "order": len(paragraphs), "ar": "", "ru": note["ru"],
                                   "role": "editor_note", "status": "draft_unchecked",
                                   "source": "Собственное пояснение редакции приложения; " + origin,
                                   "printed_pages": ps, "note_kind": note["kind"],
                                   "related_author_paragraph_id": u["id"]})
        chapters.append({"id": cid, "volume": 3, "order": n, "title": title,
                         "source": source, "printed_pages": pages,
                         "expected_paragraph_ids": [p["id"] for p in paragraphs],
                         "paragraphs": paragraphs})
        items.append({"chapterId": cid, "title": title, "subtitle": f"Том 3 · страницы {pages[0]}–{pages[-1]}",
                      "entryParagraphId": body[0]["id"], "topics": topics,
                      "entryNote": "Начало авторской главы; перевод названия взят из вводного абзаца."})
    bundle = {"book": "Мадаридж ас-саликин — Степени идущих", "volume": 3,
              "source_edition": EDITION, "source_url": "https://ibnelqayem.com/ar/books/mdarj-alsalkyn-j3-61",
              "control_pdf_sha256": "8b40f810e8e365d98c036ceb1328eb683521147f7ecf68d2a5c304b04128e427",
              "total_printed_pages": 584, "author_printed_pages": list(range(3, 585)),
              "translation_completeness": "complete_working_translation_pages_3_to_584",
              "publication_ready": False, "translation_independently_verified": False,
              "editorial_footnotes_imported": False, "unresolved_source_ids": pending,
              "edition": {"draft": True, "author_text_rights": "public_domain",
                          "translation_rights": "own_draft_not_independently_checked",
                          "structure_verified": False, "rights_status": "pending_editorial_review"},
              "expected_chapter_ids": [c["id"] for c in chapters], "chapters": chapters}
    structure = {"groups": [{"id": "volume3-2019", "title": "Том 3",
                              "provenance": "Авторские главы и подразделы из контрольного PDF 2019 года; названия переведены. Современное издательское оглавление не воспроизводится.",
                              "items": items}]}
    return bundle, structure


def main():
    bundle, structure = build()
    for name, obj in [("volume3-draft.json", bundle), ("volume3-structure.json", structure)]:
        (ASSETS / name).write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n")
    print(f"Volume 3: {len(bundle['chapters'])} chapters, {sum(len(c['paragraphs']) for c in bundle['chapters'])} reader records; draft, not publication ready")


if __name__ == "__main__":
    main()
