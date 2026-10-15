#!/usr/bin/env python3
"""Apply exactly four documented partial Arabic/Russian corrections.

No change to volume 1, all other page texts, the source OCR corpus,
or scholarly status flags. This is NOT a full 657-page review.
"""
import hashlib,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
TARGET=ROOT/"app/src/main/assets/volume2-preview.json"
INPUT=ROOT/"research/volume2-scholarly-audit/translation-revisions-448-463-507-622.json"
LOG=ROOT/"research/volume2-scholarly-audit/translation-collation-applied.json"
book=json.loads(TARGET.read_text(encoding="utf8"))
revs=json.loads(INPUT.read_text(encoding="utf8"))
assert book["volume"]==2 and len(book["chapters"])==9
assert not book["publication_ready"] and not book["translation_independently_verified"]
assert book["translation_completeness"]=="preliminary_russian_pages_3_to_659"
allpars={p["id"]:p for ch in book["chapters"] for p in ch["paragraphs"]}
assert len(allpars)==676
changes=[]
for data in revs["pages"]:
    n=data["printed_page"]
    p=allpars[f"madarij-v2-p{n:03d}-draft"]
    old=p["ru"]
    new=data["ru"]
    assert len(old)>=600 and len(new)>len(old),(n,len(old),len(new))
    assert old!=new
    p["ru"]=new
    changes.append({"page":n,"type":"russian_semantic_expansion","old_sha256":hashlib.sha256(old.encode()).hexdigest(),"new_sha256":hashlib.sha256(new.encode()).hexdigest(),"independent_scan_checked":False})
corr=revs["arabic_quran_repair"]
p=allpars[f"madarij-v2-p{corr['page']:03d}-draft"]
for q in corr["spans"]:
    before,after=q["old"],q["corrected"]
    assert p["ar"].count(before)==1,("source Quran OCR changed unexpectedly",q["reference"])
    p["ar"]=p["ar"].replace(before,after)
    changes.append({"page":corr["page"],"type":"restore_canonical_quran_quotation_from_clipped_OCR","surah_verse":q["reference"],"original_ocr_excerpt":before,"canonical_corrected_excerpt":after,"independent_scan_checked":False})
p["source"]+="; цитаты 16:128 и 2:153 восстановлены по арабскому Корану; скан печатного издания ещё требует проверки"
assert len(changes)==5 and {c["page"] for c in changes}=={448,463,507,622}
assert len(book["chapters"])==9
assert sum(len(ch["paragraphs"]) for ch in book["chapters"])==676
TARGET.write_text(json.dumps(book,ensure_ascii=False,indent=2)+"\n",encoding="utf8")
LOG.write_text(json.dumps({
 "status":"PARTIAL_CORRECTIONS__NOT_FULL_TRANSLATION_VERIFICATION",
 "changes":changes,
 "pages_touched":[448,463,507,622],
 "independently_verified_printed_pages":0,
 "publication_ready":False
},ensure_ascii=False,indent=2)+"\n",encoding="utf8")
print("APPLIED 5 fixes on 4 pages; all other paragraphs preserved; no false publication status")
