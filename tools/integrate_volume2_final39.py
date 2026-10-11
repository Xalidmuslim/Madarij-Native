#!/usr/bin/env python3
"""Append final 39 preliminary page translations to volume 2, not a textological review."""
import json,re,hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"research/volume2-source/arabic_pages.jsonl"
BATCH=ROOT/"research/volume2-batch-621-659"
VIEW=ROOT/"app/src/main/assets/volume2-preview.json"
TOC=ROOT/"app/src/main/assets/volume2-structure.json"
CHAPTER="madarij-v2-p621-p659-translation-preview"
FN=re.compile(r"\s*\(¬[٠-٩0-9]+\)")
def read(p):
    return json.loads(p.read_text(encoding="utf-8"))
def write(p,d):
    p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
src=[json.loads(x) for x in SRC.read_text(encoding="utf-8").splitlines() if x.strip()]
assert [x["printed_page"] for x in src]==list(range(1,660))
arabic={x["printed_page"]:x for x in src}
files=sorted(BATCH.glob("russian-pages-*.json"))
assert len(files)==4 and [p.name for p in files]==[
 "russian-pages-621-630.json","russian-pages-631-640.json",
 "russian-pages-641-650.json","russian-pages-651-659.json"
]
parts=[]
for f in files:
    d=read(f)
    assert d["volume"]==2 and d["translation_status"]=="preliminary_author_text_draft_not_independently_checked"
    parts.extend(d["pages"])
assert len(parts)==39 and [x["printed_page"] for x in parts]==list(range(621,660))
pairs=[]
for i,d in enumerate(parts):
    pg=d["printed_page"]
    ar=FN.sub("",arabic[pg]["arabic_body"]).strip()
    ru=d["ru"].strip()
    assert arabic[pg]["status"]=="needs_visual_collation"
    assert d["review_status"]=="draft_unchecked" and d["publisher_ready"] is False
    assert len(ar)>100 and len(ru)>200 and "\ufffd" not in ar+ru,(pg,len(ar),len(ru))
    pairs.append({
       "id":f"madarij-v2-p{pg:03d}-draft",
       "order":i,
       "ar":ar,"ru":ru,
       "role":"author","status":"draft_self_checked",
       "source":f"Том 2 · печатная стр. {pg}; предварительный русский текст, независимая религиозная и текстологическая сверка не завершена"
    })
v=read(VIEW)
pre=[
 "madarij-v2-p003-views-of-sin",
 "madarij-v2-p011-p080-translation-preview",
 "madarij-v2-p081-p150-translation-preview",
 "madarij-v2-p151-p220-translation-preview",
 "madarij-v2-p221-p320-translation-preview",
 "madarij-v2-p321-p420-translation-preview",
 "madarij-v2-p421-p520-translation-preview",
 "madarij-v2-p521-p620-translation-preview"
]
assert v["volume"]==2
assert v["expected_chapter_ids"]==pre and [c["id"] for c in v["chapters"]]==pre
assert not v["translation_independently_verified"] and not v["publication_ready"]
before={c["id"]:(len(c["paragraphs"]),c["paragraphs"][0]["id"],c["paragraphs"][-1]["id"]) for c in v["chapters"]}
v["chapters"].append({
 "id":CHAPTER,"volume":2,"order":8,
 "title":"Стыдливость, правдивость и завершение второго тома",
 "source":"https://maknoon.org/ai/view.php?bk=madarej_2&p=621; том 2, печатные стр. 621–659",
 "expected_paragraph_ids":[x["id"] for x in pairs],
 "paragraphs":pairs
})
v["expected_chapter_ids"]=pre+[CHAPTER]
v["translation_completeness"]="preliminary_russian_pages_3_to_659"
v["total_printed_pages"]=659
v["note"]=(
 "Все 657 печатных страниц основного текста второго тома (3–659) доступны в черновом русском переводе "
 "рядом с арабским оригиналом. Независимая проверка русских фрагментов со сканами арабского текста, "
 "проверка Корана и хадисов, богословская редактура и окончательная публикационная корректура НЕ завершены. "
 "Служебные страницы 1–2 арабского издания не включены в перевод авторского текста."
)
audit=v.get("editorial_audit",{})
v["editorial_audit"]={
 **audit,"printed_pages_included":list(range(3,660)),
 "arabic_page_sources_received":659,"translated_preliminary_pages":657,
 "independent_page_comparisons_verified":0,
 "quran_and_hadith_religious_review_complete":False,
 "publication_ready":False,
 "specific_checks_needed":list(dict.fromkeys(audit.get("specific_checks_needed",[])+[
   "p622–624: Quran 16:128, 2:153 Arabic source is clipped; verify canonical mushaf and hadith citations",
   "p627–630: verify all Quranic quotations and paragraphs on siddiqiyyah",
   "p638–639: distinguish three historical interpretations of Quran 2:94, not universal consensus",
   "p648: verify prophetic reports about ease, travel fasting and permissible dispensations",
   "p653: verse 76:29–30 and 81:28–29 need verified canonical Quran wording",
   "p654: attributed ecstatic quotations not verified, not prescriptive creed",
   "p657–659: Quran 50:16 competing theological exegeses, careful scholar attribution and isnad checks",
   "p659: finalize volume ending punctuation and manual Arabic visual verification"
 ]))
}
write(VIEW,v)
toc=read(TOC)
assert len(toc["groups"])==8
topics=[
(621,"Три степени стыдливости (хая)"),
(622,"Общая и особая близость Аллаха"),
(627,"Правдивость (сыдк) и её достоинство"),
(629,"Правдивость слов, дел и сердечных состояний"),
(633,"Признаки правдивости и наставления учёных"),
(638,"Толкование аята 2:94"),
(642,"Ступени правдивости — правдивое намерение"),
(646,"Жизнь ради служения и религиозные облегчения"),
(648,"Правдивость и следование Сунне"),
(651,"Человеческая воля, ответственность и предопределение"),
(655,"Знание как мерило духовных переживаний"),
(657,"Близость Аллаха и смысл Корана 50:16")
]
toc["groups"].append({
 "id":"v2-navigation-p621-p659",
 "title":"Том 2 · страницы 621–659",
 "provenance":"editorial_navigation_preliminary",
 "items":[{
  "chapterId":CHAPTER,
  "title":"Стыдливость, правдивость и завершение второго тома",
  "subtitle":"Последние 39 печатных страниц · предварительный перевод с арабским текстом",
  "entryParagraphId":pairs[0]["id"],
  "entryNote":"Содержимое черновое. Цитаты Корана, хадисы и подробный перевод требуют независимой проверки.",
  "topics":[{"paragraphId":f"madarij-v2-p{pg:03d}-draft","title":title,"provenance":"editorial_navigation_preliminary"} for pg,title in topics]
 }]
})
write(TOC,toc)
check={c["id"]:(len(c["paragraphs"]),c["paragraphs"][0]["id"],c["paragraphs"][-1]["id"]) for c in v["chapters"][:-1]}
assert before==check
report={
 "translated_pages":39,"first_printed_page":621,"last_printed_page":659,
 "russian_characters":sum(len(p["ru"]) for p in pairs),
 "arabic_characters":sum(len(p["ar"]) for p in pairs),
 "missing_pages":[],
 "all_657_body_pages_in_reader":True,
 "all_659_original_arabic_pages_in_source_corpus":True,
 "prior_chapters_preserved":True,"volume1_unchanged":True,
 "source_sha256":hashlib.sha256(SRC.read_bytes()).hexdigest(),
 "independent_page_checks_passed":0,
 "religious_citations_verified":False,
 "publication_ready":False
}
write(BATCH/"review-report.json",report)
print(json.dumps(report,ensure_ascii=False,indent=2))
