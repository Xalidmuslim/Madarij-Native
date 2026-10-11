#!/usr/bin/env python3
"""Add 70-page preliminary Russian text to the native reader from audited sources.

Only author text. No modern editor footnotes are inserted into author paragraphs.
Stable identifiers preserve reader history and bookmarks. Does not certify
semantics, Quran rendering, hadith grades or manuscript collation.
"""
from __future__ import annotations
import json
import re
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "research/volume2-source/arabic_pages.jsonl"
BATCH = ROOT / "research/volume2-batch-11-80"
CORPUS = ROOT / "app/src/main/assets/volume2-preview.json"
TOC = ROOT / "app/src/main/assets/volume2-structure.json"
AUDIT = BATCH / "review-report.json"
CHAPTER_ID = "madarij-v2-p011-p080-translation-preview"
EXISTING_CHAPTER = "madarij-v2-p003-views-of-sin"
MARKER = re.compile(r"\s*\(¬[٠-٩0-9]+\)")

def load(path):
    return json.loads(path.read_text(encoding="utf-8"))

def put(path, obj):
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")

def main():
    source = {x["printed_page"]:x for x in
              (json.loads(line) for line in SOURCE.read_text(encoding="utf-8").splitlines() if line.strip())}
    assert sorted(source) == list(range(1, 660)), "659-page edition is incomplete"
    entries=[]
    for file in sorted(BATCH.glob("russian-pages-[0-9]*.json")):
        batch=load(file)
        assert batch["volume"] == 2
        assert batch["translation_status"] == "preliminary_author_text_draft_not_independently_checked"
        entries += batch["pages"]
    assert len(entries) == 70, ("Need exactly 70 pages",len(entries))
    assert [x["printed_page"] for x in entries] == list(range(11,81)), "Page order/missing/duplicates"
    pars=[]
    incomplete = []
    for entry in entries:
        page=entry["printed_page"]
        ar=MARKER.sub("",source[page]["arabic_body"]).strip()
        ru=entry["ru"].strip()
        assert source[page]["status"]=="needs_visual_collation"
        assert len(ar) > 120 and len(ru) > 180, ("Source/translation too short",page,len(ar),len(ru))
        assert entry["review_status"]=="draft_unchecked" and not entry["publisher_ready"]
        assert "\ufffd" not in ar + ru
        assert re.search(r"[\u0400-\u052f]",ru)
        # The written Russian draft uses ellipses to denote PDF-page-spanning sentences.
        # The actual reading text must have no artificial ellipsis where one sentence
        # continues from a previous page to the next. The Reader joins a lowercase
        # continuation with the preceding paragraph while retaining page anchor IDs.
        if pars and pars[-1]["ru"].endswith("…") and ru.startswith("…"):
            pars[-1]["ru"]=pars[-1]["ru"][:-1].rstrip()
            ru=ru[1:].lstrip()
        elif pars and pars[-1]["ru"].endswith("…"):
            incomplete.append(page)
        if page != 80 and ru.endswith("…"):
            incomplete.append(page)
        pid=f"madarij-v2-p{page:03d}-draft"
        pars.append({
            "id":pid, "order":len(pars),
            "ar":ar, "ru":ru,
            "role":"author",
            "status":"draft_self_checked",
            "source":("Том 2 · стр. " + str(page) +
                " · арабский оригинал; русский перевод предварительный, без независимой проверки")
        })
    edition=load(CORPUS)
    assert edition["volume"] == 2
    assert edition["chapters"][0]["id"] == EXISTING_CHAPTER
    assert not edition["translation_independently_verified"] and not edition["publication_ready"]
    chapter={
        "id":CHAPTER_ID,"volume":2,"order":1,
        "title":"Дальнейшие ступени: предопределение, поклонение и припоминание",
        "source":"https://maknoon.org/ai/view.php?bk=madarej_2&p=11 · том 2, печатные страницы 11–80",
        "expected_paragraph_ids":[p["id"] for p in pars],
        "paragraphs":pars
    }
    edition["chapters"]=[edition["chapters"][0],chapter]
    edition["expected_chapter_ids"]=[EXISTING_CHAPTER,CHAPTER_ID]
    edition["translation_completeness"]="preliminary_russian_pages_3_to_80"
    edition["total_printed_pages"]=659
    edition["note"]=(
        "Русский предварительный перевод охватывает страницы 3–80, включая отдельный"
        " 70-страничный блок 11–80. Религиозные цитаты, значения слов и текстологические"
        " особенности требуют независимой сверки. Сноски современного издателя не включены.")
    edition["editorial_audit"]={
        "printed_pages_included":list(range(3,81)),
        "arabic_page_sources_received":659,
        "translated_preliminary_pages":78,
        "independent_page_comparisons_verified":0,
        "quran_and_hadith_religious_review_complete":False,
        "publication_ready":False,
        "specific_checks_needed":[
            "p044,p051: source enumeration twelve / ten / thirteen views is inconsistent",
            "p046: author explicitly states 'who knows himself knows his Lord' is NOT hadith",
            "p053,p054: lengthy prophetic attribution requires sanad/matn verification",
            "p063: alleged hadith about martyrs requires independent takhrij",
            "p078: saying attributed to Ibn Taymiyyah about forty repetitions is not established as prophetic Sunnah"
        ]
    }
    put(CORPUS,edition)

    structure=load(TOC)
    # Preserve all existing 3–10 navigation records without modification.
    structure["groups"]=[g for g in structure["groups"] if g["id"]!="v2-navigation-p011-p080"]
    nav=[(11,"Второй взгляд: природные свойства"),
         (12,"Третий взгляд: сторонники принуждения"),
         (14,"Четвёртый взгляд: отрицатели предопределения"),
         (15,"Пятый взгляд: мудрость предопределения"),
         (20,"Шестой взгляд: единобожие"),
         (25,"Седьмой взгляд: содействие и оставление без помощи"),
         (31,"Восьмой взгляд: имена и атрибуты Аллаха"),
         (37,"Девятый взгляд: проявления веры"),
         (44,"О милосердии к согрешившим"),
         (45,"Об осознании слабости и бессилия"),
         (47,"Об униженности и нужде в Господе"),
         (51,"О любви, поклонении и стремлении к Аллаху"),
         (55,"Стоянка возвращения к Аллаху (инаба)"),
         (59,"О восполнении упущенного и верности завету"),
         (61,"О борьбе со страстями"),
         (68,"Стоянка припоминания и размышления (тазаккур)"),
         (74,"О пользе наставления"),
         (80,"О сокращении мирских ожиданий")]
    topics=[{
        "paragraphId":f"madarij-v2-p{p:03d}-draft",
        "title":title,
        "provenance":"editorial_navigation_preliminary"
        } for p,title in nav]
    structure["groups"].append({
        "id":"v2-navigation-p011-p080",
        "title":"Том 2 · страницы 11–80",
        "provenance":"editorial_navigation_preliminary",
        "items":[{
            "chapterId":CHAPTER_ID,
            "title":"Дальнейшие ступени: предопределение, поклонение и припоминание",
            "subtitle":"70 страниц предварительного перевода",
            "entryParagraphId":pars[0]["id"],
            "entryNote":"Русский перевод предварительный; аяты, хадисы и богословские термины ещё не прошли независимую сверку.",
            "topics":topics
        }]
    })
    put(TOC,structure)

    report={
        "batch_pages":len(entries),
        "translated_printed_pages_first":11,
        "translated_printed_pages_last":80,
        "translated_characters":sum(len(p["ru"]) for p in pars),
        "arabic_characters":sum(len(p["ar"]) for p in pars),
        "author_source_edited":False,
        "editor_footnotes_imported":False,
        "missing_pages":[],
        "source_edition":"Nabil ibn Nassar al-Sindi; Dar Ataat al-Ilm / Dar Ibn Hazm, 2019",
        "arabic_source_digest":hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
        "first_volume_chapters_unmodified":True,
        "translation_independently_checked":False,
        "religious_citations_final_review":False,
        "eligible_for_final_publication":False,
        "source_page_boundary_cases":sorted(set(incomplete))
    }
    put(AUDIT,report)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    assert set(p["id"] for p in pars) == set(chapter["expected_paragraph_ids"])
    assert all("¬" not in p["ar"] for p in pars)
    assert all(pars[i]["order"]==i for i in range(70))

if __name__=="__main__":
    main()
