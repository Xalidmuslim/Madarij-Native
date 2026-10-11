#!/usr/bin/env python3
"""Integrate 100 provisional Russian pages 421–520 in the existing native reader.

Mechanical verification here cannot certify accurate translation of Arabic
or the isnad and the religious grading of cited narrations.
"""
from pathlib import Path
import json,re,hashlib
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"research/volume2-source/arabic_pages.jsonl"
BATCH=ROOT/"research/volume2-batch-421-520"
VIEW=ROOT/"app/src/main/assets/volume2-preview.json"
TOC=ROOT/"app/src/main/assets/volume2-structure.json"
CHAPTER="madarij-v2-p421-p520-translation-preview"
FN=re.compile(r"\s*\(¬[٠-٩0-9]+\)")
def load(p):
    return json.loads(p.read_text(encoding="utf8"))
def write(p,obj):
    p.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+"\n",encoding="utf8")
arabic=[json.loads(s) for s in SRC.read_text(encoding="utf8").splitlines() if s.strip()]
assert len(arabic)==659 and [x["printed_page"] for x in arabic]==list(range(1,660))
source={x["printed_page"]:x for x in arabic}
files=sorted(BATCH.glob("russian-pages-[0-9]*.json"))
assert len(files)==10,("Expected 10 x 10-page translations",len(files))
translated=[]
for f in files:
    d=load(f)
    assert d["volume"]==2 and d["translation_status"]=="preliminary_author_text_draft_not_independently_checked"
    assert len(d["pages"])==10
    translated+=d["pages"]
assert len(translated)==100
assert [p["printed_page"] for p in translated]==list(range(421,521))
paragraphs=[]
for d in translated:
    p=d["printed_page"]
    ar=FN.sub("",source[p]["arabic_body"]).strip()
    ru=d["ru"].strip()
    assert len(ar)>=100 and len(ru)>=250,(p,len(ar),len(ru))
    assert "\ufffd" not in ar+ru
    assert source[p]["status"]=="needs_visual_collation"
    assert d["review_status"]=="draft_unchecked" and d["publisher_ready"] is False
    paragraphs.append({
      "id":f"madarij-v2-p{p:03d}-draft",
      "order":len(paragraphs),
      "ar":ar,"ru":ru,
      "role":"author","status":"draft_self_checked",
      "source":f"Том 2 · печатная стр. {p}; черновой перевод, независимая религиозная и текстологическая сверка не завершена"
    })
v=load(VIEW)
pre=[
 "madarij-v2-p003-views-of-sin",
 "madarij-v2-p011-p080-translation-preview",
 "madarij-v2-p081-p150-translation-preview",
 "madarij-v2-p151-p220-translation-preview",
 "madarij-v2-p221-p320-translation-preview",
 "madarij-v2-p321-p420-translation-preview"
]
assert v["volume"]==2
assert [x["id"] for x in v["chapters"]]==pre
assert v["expected_chapter_ids"]==pre
assert not v["translation_independently_verified"] and not v["publication_ready"]
v["chapters"].append({
  "id":CHAPTER,"volume":2,"order":6,
  "title":"Покорность, терпение, довольство и предопределение",
  "source":"https://maknoon.org/ai/view.php?bk=madarej_2&p=421; том 2, печатные стр. 421–520",
  "expected_paragraph_ids":[x["id"] for x in paragraphs],
  "paragraphs":paragraphs
})
v["expected_chapter_ids"]=pre+[CHAPTER]
v["translation_completeness"]="preliminary_russian_pages_3_to_520"
v["total_printed_pages"]=659
v["note"]=(
 "Тестовый русский перевод второго тома охватывает печатные страницы 3–520 из 659."
 " Независимое постраничное сравнение русского текста со сканами арабского издания,"
 " проверка цитат Корана и иснадов хадисов и итоговая редактура не завершены."
 " Эти предварительные тексты нельзя представлять как окончательно выверенное издание."
)
old=v.get("editorial_audit",{})
v["editorial_audit"]={
 **old,
 "printed_pages_included":list(range(3,521)),
 "arabic_page_sources_received":659,
 "translated_preliminary_pages":518,
 "independent_page_comparisons_verified":0,
 "quran_and_hadith_religious_review_complete":False,
 "publication_ready":False,
 "specific_checks_needed":list(dict.fromkeys(old.get("specific_checks_needed",[])+[
   "p421–425: distinguish tawakkul, tafwid; verify Quran translations and citations",
   "p433–434: reported statements of Ibn Masud and similar narrations need authentication",
   "p443–444: fanaa description is quoted Sufi doctrine, not universal religious consensus",
   "p445–450: 16 Quranic uses of sabr need exact verse-by-verse collation",
   "p455–460: poetry and early Sufi reports need attribution",
   "p461: complaint to God vs complaint about God with Quran Yusuf 12:86",
   "p467: hadith regarding iman and sinful acts must be correctly contextualized",
   "p476: purported divine saying on accepting decree is an Israili report, not established hadith",
   "p487–489: variant Quran 89:27-30 quotation in source is corrupted, verify canonical text",
   "p501–510: distinguish divine legislative approval from universal decree and specific scholars' positions",
   "p514–518: historical jihad and punishment passages are scholarly exposition, not modern violence guidance"
 ]))
}
write(VIEW,v)
t=load(TOC)
assert len(t["groups"])==6
sections=[
(421,"Продолжение упования и полного вверения дел"),
(430,"Уверенность в Аллахе (сика)"),
(436,"Покорность Аллаху (таслим)"),
(441,"Знание, духовное состояние и предписание"),
(445,"Терпение (сабр) и шестнадцать коранических оснований"),
(451,"Три основных вида терпения"),
(461,"Жалоба Аллаху и прекрасное терпение"),
(465,"Терпение ради Аллаха и при Его помощи"),
(469,"Терпение в испытаниях и надежда на облегчение"),
(476,"Довольство (рида)"),
(481,"Пути достижения довольства"),
(487,"Успокоенная душа и возвращение к Господу"),
(492,"Довольство Аллахом как Господом"),
(501,"Различие довольства Господом и предопределённым"),
(508,"Божественная воля, любовь и нравственный выбор"),
(511,"Причины, последствия и Божественная мудрость")
]
t["groups"].append({
  "id":"v2-navigation-p421-p520",
  "title":"Том 2 · страницы 421–520",
  "provenance":"editorial_navigation_preliminary",
  "items":[{
    "chapterId":CHAPTER,
    "title":"Покорность, терпение, довольство и предопределение",
    "subtitle":"100 страниц предварительного русского перевода и арабского оригинала",
    "entryParagraphId":paragraphs[0]["id"],
    "entryNote":"Тестовый перевод: религиозные цитаты и соответствие арабским сканам не проверены независимо",
    "topics":[{"paragraphId":f"madarij-v2-p{n:03d}-draft","title":title,"provenance":"editorial_navigation_preliminary"} for n,title in sections]
  }]
})
write(TOC,t)
report={
 "translated_pages":100,
 "first_printed_page":421,
 "last_printed_page":520,
 "russian_characters":sum(len(x["ru"]) for x in paragraphs),
 "arabic_characters":sum(len(x["ar"]) for x in paragraphs),
 "missing_pages":[],
 "prior_chapters_preserved":True,
 "first_volume_unchanged":True,
 "arabic_corpus_sha256":hashlib.sha256(SRC.read_bytes()).hexdigest(),
 "independent_page_checks_passed":0,
 "religious_citations_verified":False,
 "publication_ready":False
}
write(BATCH/"review-report.json",report)
print(json.dumps(report,ensure_ascii=False,indent=2))
