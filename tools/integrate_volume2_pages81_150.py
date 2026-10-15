#!/usr/bin/env python3
"""Integrate 81–150 preliminary bilingual volume-two translation into Android assets.

This appends a new stable chapter without altering volumes 1 or 2:3–80. The
Arabic text is extracted from the precisely numbered 659-page 2019 edition,
with contemporary scholarly footnotes excluded. Russian is an UNVERIFIED draft.
"""
from __future__ import annotations
import hashlib
import json
import re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/"research/volume2-source/arabic_pages.jsonl"
BATCH=ROOT/"research/volume2-batch-81-150"
CORPUS=ROOT/"app/src/main/assets/volume2-preview.json"
TOC=ROOT/"app/src/main/assets/volume2-structure.json"
AUDIT=BATCH/"review-report.json"
CHAPTER_ID="madarij-v2-p081-p150-translation-preview"
FOOTNOTE=re.compile(r"\s*\(¬[٠-٩0-9]+\)")

def load(path):
    return json.loads(path.read_text(encoding="utf8"))
def put(path,value):
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+"\n",encoding="utf8")

def main():
    source=[json.loads(line) for line in SOURCE.read_text(encoding="utf8").splitlines() if line.strip()]
    assert len(source)==659
    assert [r["printed_page"] for r in source]==list(range(1,660))
    bypage={r["printed_page"]:r for r in source}
    drafts=[]
    files=sorted(BATCH.glob("russian-pages-[0-9]*.json"))
    assert len(files)==7,("Expected 7 ten-page files",len(files))
    for path in files:
        batch=load(path)
        assert batch["volume"]==2
        assert batch["translation_status"]=="preliminary_author_text_draft_not_independently_checked"
        drafts+=batch["pages"]
    assert len(drafts)==70
    assert [p["printed_page"] for p in drafts]==list(range(81,151)), "Missing, extra or reordered source pages"

    paragraphs=[]
    cross_page=[]
    for p in drafts:
        page=p["printed_page"]
        origin=bypage[page]
        assert origin["status"]=="needs_visual_collation"
        ar=FOOTNOTE.sub("",origin["arabic_body"]).strip()
        ru=p["ru"].strip()
        assert p["review_status"]=="draft_unchecked" and p["publisher_ready"] is False
        assert len(ar)>110 and len(ru)>90,(page,len(ar),len(ru))
        assert re.search(r"[\u0400-\u052F]",ru)
        assert "\ufffd" not in (ar+ru)
        # A PDF line ending may interrupt an author sentence. Both fragments
        # remain at their original printed pages; the reader joins lower-case
        # sentence continuation in display without reassigning source pages.
        if paragraphs and paragraphs[-1]["ru"].endswith("…") and ru.startswith("…"):
            paragraphs[-1]["ru"]=paragraphs[-1]["ru"][:-1].rstrip()
            ru=ru[1:].lstrip()
            cross_page.append(page)
        paragraphs.append({
            "id":f"madarij-v2-p{page:03d}-draft",
            "order":len(paragraphs),
            "ar":ar,"ru":ru,
            "role":"author",
            "status":"draft_self_checked",
            "source":f"Том 2 · стр. {page} · арабский оригинал; русский перевод предварительный, без независимой проверки"
        })
    assert len(paragraphs)==70

    corpus=load(CORPUS)
    assert corpus["volume"]==2 and not corpus["publication_ready"]
    assert not corpus["translation_independently_verified"]
    original_ids=[c["id"] for c in corpus["chapters"]]
    assert original_ids==[
        "madarij-v2-p003-views-of-sin",
        "madarij-v2-p011-p080-translation-preview"
    ],original_ids
    for c in corpus["chapters"]:
        assert len(c["expected_paragraph_ids"])==len(c["paragraphs"])
    corpus["chapters"].append({
        "id":CHAPTER_ID,"volume":2,"order":2,
        "title":"Размышление, защита и духовное воспитание",
        "source":"https://maknoon.org/ai/view.php?bk=madarej_2&p=81 · том 2 · печатные страницы 81–150",
        "expected_paragraph_ids":[p["id"] for p in paragraphs],
        "paragraphs":paragraphs
    })
    corpus["expected_chapter_ids"]=[c["id"] for c in corpus["chapters"]]
    corpus["translation_completeness"]="preliminary_russian_pages_3_to_150"
    corpus["total_printed_pages"]=659
    corpus["note"]=(
        "Предварительный двуязычный перевод охватывает печатные страницы 3–150 "
        "второго тома. Страницы 81–150 добавлены отдельным пакетом, не затрагивая "
        "ранние страницы. Окончательная сверка смыслов, цитат, стихов и хадисов "
        "независимым специалистом не выполнена. Примечания современного издателя не импортированы.")
    old=corpus.get("editorial_audit",{})
    corpus["editorial_audit"]={
        **old,
        "printed_pages_included":list(range(3,151)),
        "arabic_page_sources_received":659,
        "translated_preliminary_pages":148,
        "independent_page_comparisons_verified":0,
        "quran_and_hadith_religious_review_complete":False,
        "publication_ready":False,
        "specific_checks_needed":list(dict.fromkeys(old.get("specific_checks_needed",[])+[
            "p082–083: compare hadith narratives of sunset and repairing shelter to primary sources",
            "p095–097: verify hadith references and avoid treating medieval sleep medicine as modern clinical advice",
            "p101–102: verify individual chains and wording in chapter on the rope of Allah",
            "p110–111, p118: verify complete hadith wording and theological context",
            "p134–135: compare Quran verses about hearing and acceptance",
            "p138–139: narrator status for alleged dialogue with Satan; DO NOT mark sahih",
            "p141–150: author's fiqh views on singing are his own claims; preserve differing juristic opinions outside the main author text"
        ]))
    }
    put(CORPUS,corpus)

    nav=[
        (81,"Сокращение долгих мирских надежд"),
        (83,"Размышление над Кораном"),
        (87,"Пять причин порчи сердца"),
        (89,"Излишнее общение и дурные товарищи"),
        (92,"Бесплодные мечтания"),
        (93,"Привязанность к кому-либо помимо Аллаха"),
        (95,"Излишества в пище"),
        (96,"Чрезмерный сон"),
        (99,"Итисам: защита у Аллаха и Его вервь"),
        (103,"Прибегание к защите Самого Аллаха"),
        (114,"Бегство к Аллаху"),
        (118,"От знания к непосредственной убеждённости"),
        (124,"Воспитание и упражнения души"),
        (131,"Сама: слушание и понимание"),
        (133,"Три вида слушания"),
        (139,"Второй вид слушания"),
        (141,"Рассуждения автора о дозволенности слушания"),
        (147,"Ответ на доводы о наслаждении звуком")
    ]
    toc=load(TOC)
    assert len(toc["groups"])==2 and not any(
        g["id"]=="v2-navigation-p081-p150" for g in toc["groups"]
    ), "Could overwrite earlier table of contents"
    toc["groups"].append({
        "id":"v2-navigation-p081-p150",
        "title":"Том 2 · страницы 81–150",
        "provenance":"editorial_navigation_preliminary",
        "items":[{
            "chapterId":CHAPTER_ID,
            "title":"Размышление, защита и духовное воспитание",
            "subtitle":"70 страниц предварительного перевода",
            "entryParagraphId":paragraphs[0]["id"],
            "entryNote":"Текст переведён предварительно. Религиозные цитаты и богословские термины нуждаются в независимой сверке.",
            "topics":[{
                "paragraphId":f"madarij-v2-p{page:03d}-draft",
                "title":title,"provenance":"editorial_navigation_preliminary"
            } for page,title in nav]
        }]
    })
    put(TOC,toc)
    report={
        "batch_pages":70,"first_printed_page":81,"last_printed_page":150,
        "translated_russian_characters":sum(len(p["ru"]) for p in paragraphs),
        "arabic_characters":sum(len(p["ar"]) for p in paragraphs),
        "missing_or_empty_pages":[],
        "early_v2_pages_unmodified":True,
        "v1_corpus_unmodified":True,
        "source_edition":"Nabil ibn Nassar al-Sindi; Dar Ataat al-Ilm / Dar Ibn Hazm, 2019",
        "source_pages_sha256":hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
        "paragraphs_started_mid_sentence":cross_page,
        "translation_independently_checked":False,
        "religious_citation_review_completed":False,
        "suitable_for_publication":False
    }
    put(AUDIT,report)
    print(json.dumps(report,ensure_ascii=False,indent=2))

if __name__=="__main__":
    main()
