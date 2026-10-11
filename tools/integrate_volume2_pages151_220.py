#!/usr/bin/env python3
"""Add preliminary Russian pages 151–220 as an append-only third 70-page batch.

Arabic comes from the numbered 659-page 2019 original-text extraction, and
the Russian translation remains a draft. Preserve all previous chapter IDs,
source pages and reading position references. NOT a scholarly certification.
"""
from __future__ import annotations
import hashlib
import json
import re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/"research/volume2-source/arabic_pages.jsonl"
BATCH=ROOT/"research/volume2-batch-151-220"
CORPUS=ROOT/"app/src/main/assets/volume2-preview.json"
CONTENTS=ROOT/"app/src/main/assets/volume2-structure.json"
CHAPTER_ID="madarij-v2-p151-p220-translation-preview"
NOTE_RE=re.compile(r"\s*\(¬[٠-٩0-9]+\)")
EXPECTED_IDS=[
    "madarij-v2-p003-views-of-sin",
    "madarij-v2-p011-p080-translation-preview",
    "madarij-v2-p081-p150-translation-preview"
]

def read_json(path):
    return json.loads(path.read_text(encoding="utf-8"))
def write_json(path, data):
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

def main():
    sources=[json.loads(x) for x in SOURCE.read_text(encoding="utf-8").splitlines() if x.strip()]
    assert len(sources)==659 and [s["printed_page"] for s in sources]==list(range(1,660))
    original={s["printed_page"]:s for s in sources}
    files=sorted(BATCH.glob("russian-pages-[0-9]*.json"))
    assert len(files)==7, ("Expected 7 batches",len(files))
    drafts=[]
    for path in files:
        item=read_json(path)
        assert item["volume"]==2
        assert item["translation_status"]=="preliminary_author_text_draft_not_independently_checked"
        drafts+=item["pages"]
    assert len(drafts)==70
    assert [d["printed_page"] for d in drafts] == list(range(151,221))
    paragraphs=[]
    joined_continuations=[]
    for draft in drafts:
        page=draft["printed_page"]
        ar=NOTE_RE.sub("",original[page]["arabic_body"]).strip()
        ru=draft["ru"].strip()
        assert original[page]["status"]=="needs_visual_collation"
        assert len(ar)>=130 and len(ru)>=180,(page,len(ar),len(ru))
        assert "�" not in ar+ru
        assert draft["status"]=="draft_unchecked"
        assert bool(re.search(r"[\u0400-\u052f]",ru))
        if paragraphs and paragraphs[-1]["ru"].endswith("…") and ru.startswith("…"):
            paragraphs[-1]["ru"]=paragraphs[-1]["ru"][:-1].rstrip()
            ru=ru[1:].lstrip()
            joined_continuations.append(page)
        paragraphs.append({
            "id":f"madarij-v2-p{page:03d}-draft",
            "order":len(paragraphs),
            "ar":ar,
            "ru":ru,
            "role":"author",
            "status":"draft_self_checked",
            "source":f"Том 2 · стр. {page} · предварительный перевод; арабский текст и цитаты не прошли независимую сверку"
        })
    assert len(paragraphs)==70
    corpus=read_json(CORPUS)
    assert corpus["volume"]==2 and not corpus["publication_ready"]
    assert not corpus["translation_independently_verified"]
    assert [c["id"] for c in corpus["chapters"]]==EXPECTED_IDS
    assert len({p["id"] for c in corpus["chapters"] for p in c["paragraphs"]})==sum(len(c["paragraphs"]) for c in corpus["chapters"])
    corpus["chapters"].append({
        "id":CHAPTER_ID,
        "volume":2,
        "order":3,
        "title":"Смирение, страх, надежда и отрешённость от мирского",
        "source":"https://maknoon.org/ai/view.php?bk=madarej_2&p=151 · второй том · печатные страницы 151–220",
        "expected_paragraph_ids":[p["id"] for p in paragraphs],
        "paragraphs":paragraphs
    })
    corpus["expected_chapter_ids"]=EXPECTED_IDS+[CHAPTER_ID]
    corpus["translation_completeness"]="preliminary_russian_pages_3_to_220"
    corpus["note"]=(
        "Предварительный двуязычный русский перевод печатных страниц 3–220 тома 2."
        " Закладки и прежние тексты сохраняются. Русский смысл, аяты, хадисы,"
        " редакторские комментарии и некоторые повреждённые места арабской"
        " транскрипции нуждаются в независимой сверке перед публикацией."
    )
    prior=corpus.get("editorial_audit",{})
    corpus["editorial_audit"]={
        **prior,
        "printed_pages_included":list(range(3,221)),
        "arabic_page_sources_received":659,
        "translated_preliminary_pages":218,
        "independent_page_comparisons_verified":0,
        "quran_and_hadith_religious_review_complete":False,
        "publication_ready":False,
        "specific_checks_needed":list(dict.fromkeys(prior.get("specific_checks_needed",[])+[
            "p156: original 24:39 has duplicated/garbled words; use canonical Quran before approval",
            "p166: pasted Arabic Quranic fragment 2:213 is incomplete/out of context",
            "p171: author rejects continuous-sorrow narration as unauthenticated",
            "p193,p197: Arabic Quran references include corrupted verse spans",
            "p202–208: differing fiqh positions on prayer without khushu; author's preference is second opinion",
            "p218–219: Quranic quotations should be verified word by word against canonical text"
        ]))
    }
    write_json(CORPUS,corpus)

    toc=read_json(CONTENTS)
    assert len(toc["groups"])==3
    assert not any(g["id"]=="v2-navigation-p151-p220" for g in toc["groups"])
    sections=[
        (151,"Продолжение разбора музыкальных собраний"),
        (152,"Три правила оценки духовного слушания"),
        (158,"Терпение, благодарность и подмена поклонения"),
        (162,"Степени слушания"),
        (169,"Стоянка печали (хузн)"),
        (173,"Разбор трёх степеней печали"),
        (179,"Стоянка страха (хауф)"),
        (181,"Различия страха и благоговейного трепета"),
        (188,"Любовь, страх и надежда"),
        (189,"Стоянка сострадательного опасения (ишфак)"),
        (193,"Стоянка смирения (хушу)"),
        (201,"Молитва без должной сосредоточенности"),
        (206,"Мнения о действительности такой молитвы"),
        (209,"Стоянка смиренного покоя (ихбат)"),
        (218,"Стоянка отрешённости от мирского (зухд)")
    ]
    toc["groups"].append({
        "id":"v2-navigation-p151-p220",
        "title":"Том 2 · страницы 151–220",
        "provenance":"editorial_navigation_preliminary",
        "items":[{
            "chapterId":CHAPTER_ID,
            "title":"Смирение, страх, надежда и отрешённость от мирского",
            "subtitle":"70 страниц предварительного перевода",
            "entryParagraphId":paragraphs[0]["id"],
            "entryNote":"Перевод предварительный. Аяты и хадисы, арабская транскрипция и значения богословских терминов требуют независимой проверки.",
            "topics":[{
                "paragraphId":f"madarij-v2-p{p:03d}-draft",
                "title":title,"provenance":"editorial_navigation_preliminary"
            } for p,title in sections]
        }]
    })
    write_json(CONTENTS,toc)

    report={
        "volume":2,
        "batch_pages":70,
        "first_printed_page":151,
        "last_printed_page":220,
        "missing_pages":[],
        "arabic_characters":sum(len(p["ar"]) for p in paragraphs),
        "russian_characters":sum(len(p["ru"]) for p in paragraphs),
        "arabic_corpus_sha256":hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
        "previous_volume_1_unchanged":True,
        "previous_volume_2_chapter_ids_preserved":True,
        "merged_syntactic_continuations":joined_continuations,
        "source_transcription_needs_correction":[156,166,193,197,218],
        "independent_translation_collation":False,
        "religious_quote_review_complete":False,
        "ready_for_publication":False
    }
    write_json(BATCH/"review-report.json",report)
    print(json.dumps(report,ensure_ascii=False,indent=2))

if __name__=="__main__":
    main()
