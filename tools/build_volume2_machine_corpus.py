#!/usr/bin/env python3
"""Assemble UNREVIEWED 659-page machine draft as optional Android book assets.

Pages 3-6 have a separate editor-written draft already in app assets. Do not
overwrite them. Pages 7-659 are machine-only drafts, kept distinct and labelled.
Output cannot claim authorial equivalence, religious verification or publication.
"""
import argparse
import hashlib
import json
from pathlib import Path
from translate_volume2_machine import read_candidates, prepare

SRC = "research/volume2-source/source_audit.json"
EDITION = "Nabil bin Nassar al-Sindi; Dar Ataat al-Ilm / Dar Ibn Hazm (2019)"

def dump(path, content):
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    Path(path).write_text(json.dumps(content, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

def run(folder, corpus, structure):
    info = json.loads(Path(SRC).read_text(encoding="utf-8"))
    assert info["pages_retrieved"] == 659 and not info["missing_pages"]
    rows = []
    for file in sorted(Path(folder).glob("shard-*.jsonl")):
        rows += [json.loads(line) for line in file.read_text(encoding="utf-8").splitlines() if line.strip()]
    expected = prepare(read_candidates(), first=7)
    assert len(rows) == len(expected), ("Missing or extra segments",len(rows),len(expected))
    lookup = {r["id"]:r for r in rows}
    assert len(lookup) == len(rows)
    assert set(lookup) == {x["id"] for x in expected}, "Missing translated candidate IDs"
    for e in expected:
        actual=lookup[e["id"]]
        assert actual["page_start"] == e["page_start"]
        assert actual["page_end"] == e["page_end"]
        assert actual["arabic"] == e["arabic"]
        assert actual["source_sha256"] == hashlib.sha256(e["arabic"].encode()).hexdigest()
        assert actual["model"] == "Helsinki-NLP/opus-mt-ar-ru"
        assert actual["translation_status"] == "machine_draft_unverified"
        assert not actual["translation_checked_by_human"] and not actual["publishable"]
        assert actual["ru"] and any("\u0400" <= ch <= "\u052f" for ch in actual["ru"])
    ordered=[lookup[e["id"]] for e in expected]
    assert ordered[0]["page_start"] == 7 and ordered[-1]["page_end"] <= 659
    page_set={p for r in ordered for p in range(r["page_start"],r["page_end"]+1)}
    assert page_set == set(range(7, 660)), ("Untranslated pages", sorted(set(range(7,660))-page_set))
    groups = []
    chapters = []
    for start in range(7, 660, 20):
        end = min(659,start+19)
        chapter_rows = [r for r in ordered if start <= r["page_start"] <= end]
        assert chapter_rows
        cid=f"madarij-v2-machine-p{start:03d}-{end:03d}"
        paras=[]
        for i,r in enumerate(chapter_rows):
            paras.append({
                "id":r["id"], "order":i,
                "ar":r["arabic"], "ru":r["ru"],
                "role":"author", "status":"draft_unchecked",
                "source":("Том 2 · страницы " + str(r["page_start"]) +
                    ("–"+str(r["page_end"]) if r["page_end"]>r["page_start"] else "") +
                    " · МАШИННЫЙ ПЕРЕВОД БЕЗ БОГОСЛОВСКОЙ СВЕРКИ")
            })
        title=f"Страницы {start}–{end}"
        chapters.append({
            "id":cid, "volume":2, "order":len(chapters), "title":title,
            "source":f"https://maknoon.org/ai/view.php?bk=madarej_2&p={start}",
            "expected_paragraph_ids":[p["id"] for p in paras],
            "paragraphs":paras
        })
        gindex = (start-7)//100
        while len(groups) <= gindex:
            low=7+100*len(groups)
            hi=min(659,low+99)
            groups.append({"id":f"v2-machine-navigation-{len(groups)+1:02d}",
                "title":f"Том 2 · страницы {low}–{hi} · машинный черновик",
                "provenance":"UNVERIFIED_MACHINE_TRANSLATION",
                "items":[]})
        groups[gindex]["items"].append({
            "chapterId":cid,"title":title,
            "subtitle":"Перевод автоматический; не проверен по смыслу и религиозным источникам.",
            "topics":[],
            "entryParagraphId":paras[0]["id"]
        })
    # Existing volume-two preview must remain separate from machine output.
    c={
        "book":"مدارج السالكين — Степени идущих",
        "volume":2,"source_edition":EDITION,"source_printed_pages":659,
        "translation_coverage":"machine_draft_pages_7_to_659",
        "translation_independently_verified":False,
        "publication_ready":False,
        "translation_model":"Helsinki-NLP/opus-mt-ar-ru",
        "quality_warning":"Грубый автоматический перевод классического арабского текста. Возможны смысловые и религиозные ошибки. Не использовать для вынесения религиозных решений.",
        "edition":{"draft":True,"author_text_rights":"public_domain",
                   "translation_rights":"own_draft_not_independently_checked",
                   "structure_verified":False,"rights_status":"pending_editorial_review"},
        "expected_chapter_ids":[c["id"] for c in chapters],
        "chapters":chapters
    }
    dump(corpus,c)
    dump(structure,{"groups":groups})
    report={"pages_3_to_6":"editor-written preliminary preview",
        "pages_7_to_659":"machine-generated unverified translation",
        "machine_segments":len(ordered),"machine_chapters":len(chapters),
        "pages_present":len(page_set),"source_pages_full_volume":659,
        "translation_validated":"structure, completeness and language only",
        "source_visual_collation_verified":False,
        "meaning_accuracy_verified":False,
        "quran_hadith_verified":False,
        "ready_for_publication":False}
    dump(Path(corpus).with_name("volume2-machine-audit.json"),report)
    print(json.dumps(report,ensure_ascii=False))

if __name__=="__main__":
    ap=argparse.ArgumentParser()
    ap.add_argument("--input-dir",required=True)
    ap.add_argument("--corpus",default="app/src/main/assets/volume2-machine-draft.json")
    ap.add_argument("--structure",default="app/src/main/assets/volume2-machine-structure.json")
    a=ap.parse_args()
    run(a.input_dir,a.corpus,a.structure)
