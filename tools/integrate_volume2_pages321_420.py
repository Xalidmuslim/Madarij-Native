#!/usr/bin/env python3
"""Append printed pages 321–420 to the existing Russian/Arabic Android reader.

Page presence/source anchoring is machine-checked, not editorially certified.
No first-volume assets are changed by this script. Text is a preliminary draft.
"""
from pathlib import Path
import json,re,hashlib
ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/"research/volume2-source/arabic_pages.jsonl"
BATCH=ROOT/"research/volume2-batch-321-420"
READER=ROOT/"app/src/main/assets/volume2-preview.json"
TOC=ROOT/"app/src/main/assets/volume2-structure.json"
CHAPTER="madarij-v2-p321-p420-translation-preview"
FOOTNOTE=re.compile(r"\s*\(¬[٠-٩0-9]+\)")
def load(path):
    return json.loads(path.read_text(encoding="utf8"))
def save(path,val):
    path.write_text(json.dumps(val,ensure_ascii=False,indent=2)+"\n",encoding="utf8")
src=[json.loads(x) for x in SOURCE.read_text(encoding="utf8").splitlines() if x.strip()]
assert len(src)==659 and [x["printed_page"] for x in src]==list(range(1,660))
original={x["printed_page"]:x for x in src}
batches=sorted(BATCH.glob("russian-pages-[0-9]*.json"))
assert len(batches)==10, len(batches)
drafts=[]
for path in batches:
    part=load(path)
    assert part["volume"]==2 and part["translation_status"]=="preliminary_author_text_draft_not_independently_checked"
    drafts+=part["pages"]
assert len(drafts)==100
assert [d["printed_page"] for d in drafts]==list(range(321,421))
texts=[]
joined=[]
for d in drafts:
    page=d["printed_page"]
    ar=FOOTNOTE.sub("",original[page]["arabic_body"]).strip()
    ru=d["ru"].strip()
    assert len(ar)>100 and len(ru)>200,(page,len(ar),len(ru))
    assert d["review_status"]=="draft_unchecked" and d["publisher_ready"] is False
    assert original[page]["status"]=="needs_visual_collation"
    assert "\ufffd" not in ar+ru
    if texts and texts[-1]["ru"].endswith("…") and ru.startswith("…"):
        texts[-1]["ru"]=texts[-1]["ru"][:-1].rstrip()
        ru=ru[1:].lstrip()
        joined.append(page)
    texts.append({
        "id":f"madarij-v2-p{page:03d}-draft",
        "order":len(texts),
        "ar":ar,"ru":ru,
        "role":"author","status":"draft_self_checked",
        "source":f"Том 2 · печатная страница {page}. Черновой перевод, независимая сверка религиозных цитат ещё не завершена"
    })
assert len(texts)==100
prev=[
    "madarij-v2-p003-views-of-sin",
    "madarij-v2-p011-p080-translation-preview",
    "madarij-v2-p081-p150-translation-preview",
    "madarij-v2-p151-p220-translation-preview",
    "madarij-v2-p221-p320-translation-preview"
]
corpus=load(READER)
assert corpus["volume"]==2
assert corpus["expected_chapter_ids"]==prev
assert [c["id"] for c in corpus["chapters"]]==prev
assert not corpus["publication_ready"] and not corpus["translation_independently_verified"]
for c in corpus["chapters"]:
    assert len(c["expected_paragraph_ids"])==len(c["paragraphs"])
corpus["chapters"].append({
 "id":CHAPTER,"volume":2,"order":5,
 "title":"Искренность, прямота и упование",
 "source":"https://maknoon.org/ai/view.php?bk=madarej_2&p=321 · том 2, стр. 321–420",
 "expected_paragraph_ids":[p["id"] for p in texts],
 "paragraphs":texts
})
corpus["expected_chapter_ids"]=prev+[CHAPTER]
corpus["translation_completeness"]="preliminary_russian_pages_3_to_420"
corpus["note"]=(
    "Предварительный перевод второго тома охватывает печатные страницы 3–420."
    " Арабские тексты хранятся рядом с русскими. Перевод не прошёл независимую"
    " постраничную сверку, проверку религиозных цитат и профессиональную"
    " богословскую редактуру; это тестовый читатель, не публикационное издание."
)
old=corpus.get("editorial_audit",{})
corpus["editorial_audit"]={
 **old,"printed_pages_included":list(range(3,421)),
 "arabic_page_sources_received":659,
 "translated_preliminary_pages":418,
 "independent_page_comparisons_verified":0,
 "quran_and_hadith_religious_review_complete":False,
 "publication_ready":False,
 "specific_checks_needed":list(dict.fromkeys(old.get("specific_checks_needed",[])+[
    "p321–330: verify Qur'an renderings and differing isnad grades for narrations on Paradise and Hell",
    "p337–340: accurately attribute theological claims and disputed consensus opinions",
    "p344–350: verify hadith text on ikhlas and alleged divine saying on ikhlas",
    "p370: source attributes Thawban report to Sahih Muslim; check actual collection",
    "p383: verify hadith of leaving home, cross-page continuity",
    "p388: reported Ibrahim–Jibril narrative requires authentication",
    "p411–414: reports on begging must distinguish illicit solicitation from seeking lawful aid",
    "p417: author's historical military narrative is not a contemporary directive"
 ]))
}
save(READER,corpus)
toc=load(TOC)
assert len(toc["groups"])==5
sections=[
 (321,"Поклонение и ожидание награды"),
 (331,"Путь любви, Рай и довольство Аллаха"),
 (335,"Намерение, показуха и польза публичного доброго дела"),
 (337,"Принципы понимания Божественных атрибутов"),
 (344,"Искренность (ихляс)"),
 (351,"Три степени искренности"),
 (358,"Воспитание души и очищение служения"),
 (361,"Соотношение знания и духовного состояния"),
 (368,"Прямота и постоянство (истикама)"),
 (373,"Умеренность и следование Сунне"),
 (381,"Упование на Аллаха (таваккуль)"),
 (385,"Определения и условия упования"),
 (391,"Ступени упования и истинное доверие"),
 (392,"Причинность и предопределение"),
 (395,"Единобожие сердца и довольство"),
 (399,"Ошибки в понимании таваккуля"),
 (403,"Упование и обращение к Покровителю"),
 (411,"Просьба о помощи, нужда и независимость сердца"),
 (415,"Использование причин во время путешествий"),
 (419,"Очищение таваккуля от внутренних недостатков")
]
toc["groups"].append({
 "id":"v2-navigation-p321-p420",
 "title":"Том 2 · страницы 321–420",
 "provenance":"editorial_navigation_preliminary",
 "items":[{
     "chapterId":CHAPTER,
     "title":"Искренность, прямота и упование",
     "subtitle":"100 печатных страниц: арабский оригинал и предварительный русский перевод",
     "entryParagraphId":texts[0]["id"],
     "entryNote":"Независимая сверка аятов, хадисов и перевода ещё необходима. Перевод тестовый.",
     "topics":[{"paragraphId":f"madarij-v2-p{n:03d}-draft","title":title,"provenance":"editorial_navigation_preliminary"} for n,title in sections]
 }]
})
save(TOC,toc)
report={
 "translated_pages":100,"first_printed_page":321,"last_printed_page":420,
 "russian_characters":sum(len(x["ru"]) for x in texts),
 "arabic_characters":sum(len(x["ar"]) for x in texts),
 "missing_pages":[],"previous_chapters_untouched":True,
 "volume1_untouched":True,
 "cross_page_boundary_joins":joined,
 "original_arabic_source_sha256":hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
 "independent_page_checks_passed":0,
 "religious_citations_verified":False,
 "publication_ready":False
}
save(BATCH/"review-report.json",report)
print(json.dumps(report,ensure_ascii=False,indent=2))
