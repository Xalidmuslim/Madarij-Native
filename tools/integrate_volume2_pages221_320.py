#!/usr/bin/env python3
"""Integrate original Arabic plus Russian draft for printed pages 221–320.

Follows exactly the 2019 selected edition's page anchors. No author source
text or first-volume content is changed. Scholar/visual reviews are pending.
"""
from pathlib import Path
import json,re,hashlib
root=Path(__file__).resolve().parents[1]
src=root/"research/volume2-source/arabic_pages.jsonl"
batch=root/"research/volume2-batch-221-320"
target=root/"app/src/main/assets/volume2-preview.json"
tocpath=root/"app/src/main/assets/volume2-structure.json"
CHAPTER="madarij-v2-p221-p320-translation-preview"
FOOTNOTE=re.compile(r"\s*\(¬[٠-٩0-9]+\)")
jload=lambda p:json.loads(p.read_text(encoding="utf-8"))
def save(p,obj):
    p.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
source=[json.loads(s) for s in src.read_text(encoding="utf-8").splitlines() if s]
assert len(source)==659
assert [s["printed_page"] for s in source]==list(range(1,660))
by_page={s["printed_page"]:s for s in source}
files=sorted(batch.glob("russian-pages-[0-9]*.json"))
assert len(files)==10,("Expected 10 files of 10 pages",len(files))
entries=[]
for file in files:
    item=jload(file)
    assert item["volume"]==2
    assert item["translation_status"]=="preliminary_author_text_draft_not_independently_checked"
    entries+=item["pages"]
assert len(entries)==100
assert [e["printed_page"] for e in entries]==list(range(221,321))
paragraphs=[]
linked=[]
for e in entries:
    n=e["printed_page"]
    arabic=FOOTNOTE.sub("",by_page[n]["arabic_body"]).strip()
    russian=e["ru"].strip()
    assert len(arabic)>=95,(n,"short Arabic",len(arabic))
    assert len(russian)>150,(n,"short Russian",len(russian))
    assert e["review_status"]=="draft_unchecked" and e["publisher_ready"] is False
    assert by_page[n]["status"]=="needs_visual_collation"
    assert "\ufffd" not in arabic+russian
    if paragraphs and paragraphs[-1]["ru"].endswith("…") and russian.startswith("…"):
        paragraphs[-1]["ru"]=paragraphs[-1]["ru"].rstrip("…").rstrip()
        russian=russian.lstrip("…").lstrip()
        linked.append(n)
    paragraphs.append({
        "id":f"madarij-v2-p{n:03d}-draft",
        "order":len(paragraphs),
        "ar":arabic,
        "ru":russian,
        "role":"author",
        "status":"draft_self_checked",
        "source":f"Том 2, печатная стр. {n}; русский перевод предварительный; проверка цитат не завершена"
    })
assert len(paragraphs)==100
corpus=jload(target)
prior=[
    "madarij-v2-p003-views-of-sin",
    "madarij-v2-p011-p080-translation-preview",
    "madarij-v2-p081-p150-translation-preview",
    "madarij-v2-p151-p220-translation-preview"
]
assert corpus["volume"]==2
assert [x["id"] for x in corpus["chapters"]]==prior
assert corpus["expected_chapter_ids"]==prior
assert not corpus["translation_independently_verified"] and not corpus["publication_ready"]
corpus["chapters"].append({
    "id":CHAPTER,
    "volume":2,"order":4,
    "title":"Отрешённость, осторожность, надежда и наблюдение за сердцем",
    "source":"https://maknoon.org/ai/view.php?bk=madarej_2&p=221; том 2, страницы 221–320",
    "expected_paragraph_ids":[p["id"] for p in paragraphs],
    "paragraphs":paragraphs
})
corpus["expected_chapter_ids"]=prior+[CHAPTER]
corpus["translation_completeness"]="preliminary_russian_pages_3_to_320"
corpus["total_printed_pages"]=659
corpus["note"]=(
 "Двуязычный предварительный русский перевод охватывает печатные страницы 3–320."
 " Независимая сверка точности перевода, арабской транскрипции, коранических цитат"
 " и хадисов не завершена. Первый том и предыдущие главы не изменены."
)
old=corpus.get("editorial_audit",{})
corpus["editorial_audit"]={
 **old,
 "printed_pages_included":list(range(3,321)),
 "arabic_page_sources_received":659,
 "translated_preliminary_pages":318,
 "independent_page_comparisons_verified":0,
 "quran_and_hadith_religious_review_complete":False,
 "publication_ready":False,
 "specific_checks_needed":list(dict.fromkeys(old.get("specific_checks_needed",[])+[
    "p224: attribution of zuhd saying as raised prophetic hadith needs validation",
    "p234: source gives truncated verse annotation for Quran 74:4",
    "p267: variant wording of divine hadith should be checked with primary source",
    "p277–279: many Quran and hadith prayers; verify all references and statuses",
    "p293–294: author presents controversial or rejected fiqh options, not endorsed positions",
    "p297–299: precise Arabic grammatical analysis of 57:27 and different opinions",
    "p319: extracted Arabic Quran verse 22:30 is truncated"
 ]))
}
save(target,corpus)

toc=jload(tocpath)
assert len(toc["groups"])==4
assert not any(g["id"]=="v2-navigation-p221-p320" for g in toc["groups"])
sections=[
 (221,"Объяснения отрешённости от мирского"),
 (225,"Что означает оставление дозволенного"),
 (232,"Отрешённость от самой отрешённости"),
 (234,"Осторожное воздержание (вара)"),
 (241,"Три степени вара"),
 (247,"Связь духовных стоянок и их плоды"),
 (249,"Полное посвящение себя Аллаху (табаттуль)"),
 (259,"Надежда (раджа)"),
 (261,"Соединение надежды и страха"),
 (267,"Достоинство надежды и мольбы"),
 (281,"Плоды надежды"),
 (284,"Три степени надежды"),
 (290,"Деятельное стремление (рагба)"),
 (292,"Правильные религиозные облегчения"),
 (297,"Заботливое соблюдение знания и дел (риая)"),
 (305,"Наблюдение Аллаха за сердцем (муракаба)"),
 (308,"Степени духовного наблюдения"),
 (319,"Возвеличивание неприкосновенных установлений")
]
toc["groups"].append({
 "id":"v2-navigation-p221-p320",
 "title":"Том 2 · страницы 221–320",
 "provenance":"editorial_navigation_preliminary",
 "items":[{
   "chapterId":CHAPTER,
   "title":"Отрешённость, осторожность, надежда и наблюдение за сердцем",
   "subtitle":"100 печатных страниц предварительного русского перевода",
   "entryParagraphId":paragraphs[0]["id"],
   "entryNote":"Рабочий перевод. Достоверность религиозных цитат и соответствие арабским сканам требуют независимой сверки.",
   "topics":[{
      "paragraphId":f"madarij-v2-p{n:03d}-draft",
      "title":name,
      "provenance":"editorial_navigation_preliminary"
   } for n,name in sections]
 }]
})
save(tocpath,toc)
report={
 "translated_pages":100,
 "first_printed_page":221,
 "last_printed_page":320,
 "russian_characters":sum(len(p["ru"]) for p in paragraphs),
 "arabic_characters":sum(len(p["ar"]) for p in paragraphs),
 "missing_pages":[],
 "prior_volume2_chapter_ids_unchanged":True,
 "first_volume_corpus_unmodified":True,
 "source_edition":"Nabil ibn Nassar al-Sindi; Dar Ataat al-Ilm / Dar Ibn Hazm, 2019",
 "arabic_source_sha256":hashlib.sha256(src.read_bytes()).hexdigest(),
 "cross_page_boundary_joins":linked,
 "independently_reviewed_pages":0,
 "verified_religious_citations":False,
 "publication_ready":False
}
save(batch/"review-report.json",report)
print(json.dumps(report,ensure_ascii=False,indent=2))
