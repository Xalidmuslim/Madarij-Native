#!/usr/bin/env python3
"""Apply 10 page-specific faithful-translation expansions with original-text SHA guards.

Source: al-Sindi 2019 *digital Arabic transcription*. This is not a scan-level
collation or independent scholarly and hadith review.
"""
import hashlib,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
VIEW=ROOT/"app/src/main/assets/volume2-preview.json"
RESEARCH=ROOT/"research/volume2-scholarly-audit"
FILES=[
 "faithfulness-corrections-271-407-478-479-497-498.json",
 "faithfulness-corrections-440-444-446-468.json"
]
ORIGINAL_HASHES={
 271:"3f2569881c894947be995d280b44d5ace1cb22346779010f18d6ef864dee80e9",
 407:"cfd226b53a749002401cce36a56f3db572bc8fb691ee98c67f1fdb7d5cea867a",
 440:"159b3d841510a46f5cee801723947c3916b5178762f5a37e653b4a38d9ce0bf0",
 444:"820270e679e6d8cb8dcd65ab6011cfcfaa9c5425f52793f5ee39ea7476073599",
 446:"1479cc11ea419a84210fcac67974ca860450a08702afe189ea008bf5a21688ee",
 468:"5cc9a4927b6b9d3ac3e7690252ac6c1b7714679e443a7437078e890fc7e7d9ad",
 478:"f32264c1f49573b0caa1378dc7b96f61f98efedaef38d84ec31bc3e8a135e4a2",
 479:"7e4af07718e8d6f1b3352e4ac47c000db237747e2f4415ca5a6bb8136e23f39e",
 497:"07635de0a43ba5f6266244f26a7796999bc5afbfdad70e2f34e2ff50ff9f99cb",
 498:"28468a64ff057cac580a6ecf7b5a29b8b7ee4f9f69b28ce718d1539f99464fe4"
}
b=json.loads(VIEW.read_text(encoding="utf8"))
assert b["volume"]==2 and b["translation_completeness"]=="preliminary_russian_pages_3_to_659"
assert not b["publication_ready"] and not b["translation_independently_verified"]
old_count=sum(len(c["paragraphs"]) for c in b["chapters"])
assert len(b["chapters"])==9 and old_count==676
idx={p["id"]:p for c in b["chapters"] for p in c["paragraphs"]}
changes=[]
for fname in FILES:
 d=json.loads((RESEARCH/fname).read_text(encoding="utf8"))
 assert d["publication_ready"] is False
 for entry in d["pages"]:
  n=entry["printed_page"]
  p=idx[f"madarij-v2-p{n:03d}-draft"]
  actual=hashlib.sha256(p["ru"].encode("utf8")).hexdigest()
  assert actual==ORIGINAL_HASHES[n],(n,"source changed; refusing to overwrite",actual)
  ar_before=p["ar"]
  ru_before=p["ru"]
  ru_new=entry["ru"].strip()
  assert len(ru_new)>len(ru_before)*1.15 and len(ru_new)>=900,(n,len(ru_before),len(ru_new))
  assert ru_new!=ru_before
  p["ru"]=ru_new
  assert p["ar"]==ar_before and p["role"]=="author" and p["status"]=="draft_self_checked"
  changes.append({"printed_page":n,
      "old_russian_characters":len(ru_before),
      "new_russian_characters":len(ru_new),
      "original_russian_sha256":actual,
      "expanded_russian_sha256":hashlib.sha256(ru_new.encode("utf8")).hexdigest(),
      "arabic_remains_byte_identical":True,
      "checked_against":"2019 edition electronic Arabic transcription",
      "independent_visual_print_scan_review":False,
      "isnad_review_completed":False})
assert set(x["printed_page"] for x in changes)==set(ORIGINAL_HASHES)
assert sum(len(c["paragraphs"]) for c in b["chapters"])==old_count
VIEW.write_text(json.dumps(b,ensure_ascii=False,indent=2)+"\n",encoding="utf8")
report={"review_scope":"TEN_PAGE_DETAILED_CORRECTIONS__NOT_FULL_657_PAGE_CERTIFICATION",
 "pages_corrected":len(changes),
 "corrected_page_numbers":sorted(ORIGINAL_HASHES),
 "russian_characters_added":sum(x["new_russian_characters"]-x["old_russian_characters"] for x in changes),
 "original_arabic_corpus_changed":False,
 "first_volume_changed":False,
 "all_657_pages_independently_verified":False,
 "publication_ready":False,
 "changes":sorted(changes,key=lambda x:x["printed_page"])}
(RESEARCH/"faithfulness-corrections-v159-result.json").write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n",encoding="utf8")
print(json.dumps({k:v for k,v in report.items() if k!="changes"},ensure_ascii=False))
