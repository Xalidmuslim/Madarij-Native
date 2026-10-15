#!/usr/bin/env python3
"""Integrate 100 provisional Russian pages 521–620 in the existing native reader.

Mechanical verification here cannot certify accurate translation of Arabic
or the isnad and the religious grading of cited narrations.
"""
from pathlib import Path
import json,re,hashlib
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"research/volume2-source/arabic_pages.jsonl"
BATCH=ROOT/"research/volume2-batch-521-620"
VIEW=ROOT/"app/src/main/assets/volume2-preview.json"
TOC=ROOT/"app/src/main/assets/volume2-structure.json"
CHAPTER="madarij-v2-p521-p620-translation-preview"
FN=re.compile(r"\s*\(¬[٠-٩0-9]+\)")
def load(p):
    return json.loads(p.read_text(encoding="utf8"))
def write(p,obj):
    p.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+"\n",encoding="utf8")
arabic=[json.loads(s) for s in SRC.read_text(encoding="utf8").splitlines() if s.strip()]
assert len(arabic)==659 and [x["printed_page"] for x in arabic]==list(range(1,660))
source={x["printed_page"]:x for x in arabic}

ORIGINAL=BATCH/"original-arabic"
ORIGINAL.mkdir(parents=True,exist_ok=True)
for first in range(521,621,10):
    original_pages=[
        {"printed_page":p,"arabic":source[p]["arabic_body"],"source_status":source[p]["status"]}
        for p in range(first,first+10)
    ]
    write_path=ORIGINAL/f"arabic-pages-{first:03d}-{first+9:03d}.json"
    write_path.write_text(json.dumps({
        "volume":2,"printed_pages":[first,first+9],
        "edition":"Nabil ibn Nassar al-Sindi, Dar Ataat al-Ilm / Dar Ibn Hazm, 2019",
        "visual_collation_complete":False,
        "pages":original_pages
    },ensure_ascii=False,indent=2)+"\n",encoding="utf8")
files=sorted(BATCH.glob("russian-pages-[0-9]*.json"))
assert len(files)==10,("Expected 10 x 10-page translations",len(files))
translated=[]
for f in files:
    d=load(f)
    assert d["volume"]==2 and d["translation_status"]=="preliminary_author_text_draft_not_independently_checked"
    assert len(d["pages"])==10
    translated+=d["pages"]
assert len(translated)==100
assert [p["printed_page"] for p in translated]==list(range(521,621))
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
 "madarij-v2-p321-p420-translation-preview",
 "madarij-v2-p421-p520-translation-preview"
]
assert v["volume"]==2
assert [x["id"] for x in v["chapters"]]==pre
assert v["expected_chapter_ids"]==pre
assert not v["translation_independently_verified"] and not v["publication_ready"]
v["chapters"].append({
  "id":CHAPTER,"volume":2,"order":7,
  "title":"Довольство, благодарность и стыдливость",
  "source":"https://maknoon.org/ai/view.php?bk=madarej_2&p=521; том 2, печатные стр. 521–620",
  "expected_paragraph_ids":[x["id"] for x in paragraphs],
  "paragraphs":paragraphs
})
v["expected_chapter_ids"]=pre+[CHAPTER]
v["translation_completeness"]="preliminary_russian_pages_3_to_620"
v["total_printed_pages"]=659
v["note"]=(
 "Тестовый русский перевод второго тома охватывает печатные страницы 3–620 из 659."
 " Независимое постраничное сравнение русского текста со сканами арабского издания,"
 " проверка цитат Корана и иснадов хадисов и итоговая редактура не завершены."
 " Эти предварительные тексты нельзя представлять как окончательно выверенное издание."
)
old=v.get("editorial_audit",{})
v["editorial_audit"]={
 **old,
 "printed_pages_included":list(range(3,621)),
 "arabic_page_sources_received":659,
 "translated_preliminary_pages":618,
 "independent_page_comparisons_verified":0,
 "quran_and_hadith_religious_review_complete":False,
 "publication_ready":False,
 "specific_checks_needed":list(dict.fromkeys(old.get("specific_checks_needed",[])+[
   "p521–540: distinguish divine decree and human accountability; verify all hadith quotations",
   "p545–551: Israili and devotional stories are not sahih prophetic hadith",
   "p553: verify long Quran/Sunnah supplication exactly and cross-check isnad",
   "p559–560: supernatural reports and historical attributions need verification",
   "p566–577: permissibility of zakat, lawful aid and rights must not be obscured by anti-begging admonitions",
   "p583–585: describe fana as debated subjective state, not an obligation",
   "p586–588: verify Quran quotations and prophetic dua before release",
   "p592, p597: uncertain qudsi report attribution and transmission history",
   "p611–620: original Quran and prophetic statements on haya need Arabic canonical verification",
   "p617–618: narratives about Adam and angels are not automatically authenticated"
 ]))
}
write(VIEW,v)
t=load(TOC)
assert len(t["groups"])==7
sections=[
(521,"Довольство предопределением и отказ от оправдания греха"),
(525,"Равное доверие Господу при благе и испытании"),
(541,"Довольство Аллаха и высокая награда верующим"),
(552,"Мольба о довольстве и правильное отношение к дарам"),
(561,"Дела сердца и нравственная зрелость"),
(565,"Прекращение бесплодных споров и обращение за помощью"),
(571,"Хадисы об умеренности просьб"),
(577,"Настойчивая мольба и довольство"),
(582,"Третья степень рида и различение духовных состояний"),
(586,"Благодарность (шукр)"),
(589,"Пять оснований настоящей благодарности"),
(593,"Отличие хвалы (хамд) и благодарности (шукр)"),
(601,"Благодарность как обязанность перед Дарителем"),
(604,"Благодарность в испытаниях"),
(611,"Стыдливость перед Аллахом (хая)"),
(616,"Десять видов благоговейной стыдливости")
]
t["groups"].append({
  "id":"v2-navigation-p521-p620",
  "title":"Том 2 · страницы 521–620",
  "provenance":"editorial_navigation_preliminary",
  "items":[{
    "chapterId":CHAPTER,
    "title":"Довольство, благодарность и стыдливость",
    "subtitle":"100 страниц предварительного русского перевода и арабского оригинала",
    "entryParagraphId":paragraphs[0]["id"],
    "entryNote":"Тестовый перевод: религиозные цитаты и соответствие арабским сканам не проверены независимо",
    "topics":[{"paragraphId":f"madarij-v2-p{n:03d}-draft","title":title,"provenance":"editorial_navigation_preliminary"} for n,title in sections]
  }]
})
write(TOC,t)
report={
 "translated_pages":100,
 "first_printed_page":521,
 "last_printed_page":620,
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
