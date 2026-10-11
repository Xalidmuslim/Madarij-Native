#!/usr/bin/env python3
"""Build an actionable 657-page human review register, never a proof of accuracy."""
import collections,json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
book=json.loads((ROOT/"app/src/main/assets/volume2-preview.json").read_text())
qrefs=json.loads((ROOT/"research/volume2-scholarly-audit/quran-reference-screen.json").read_text())
issues=json.loads((ROOT/"research/volume2-scholarly-audit/issues.json").read_text())
assert len(book["chapters"])==9 and not book["publication_ready"]
map_pg=collections.defaultdict(list)
for ch in book["chapters"]:
 for p in ch["paragraphs"]:
  m=re.search(r"стр\.\s*(\d+)(?:[–-](\d+))?",p["source"])
  assert m,p["id"]
  a=int(m.group(1));b=int(m.group(2) or a)
  for i in range(max(a,3),min(b,659)+1):map_pg[i].append(p)
assert set(map_pg)==set(range(3,660))
qpg=collections.defaultdict(list)
for item in qrefs:qpg[item["page"]].append(item)
ipg=collections.defaultdict(list)
for item in issues:ipg[item["page"]].append(item)
pages=[]
for n in range(3,660):
 ps=map_pg[n]; ids={p["id"] for p in ps}
 qs=[x for x in qpg[n] if x["id"] in ids]
 rus=[]
 for p in ps:rus.extend([tuple(map(int,m)) for m in re.findall(r"\((\d{1,3}):(\d{1,3})\)",p["ru"])])
 missing=[x["ref"] for x in qs if x.get("sura") and x.get("start") and not any(vs==x["sura"] and (x["start"]<=vv<=x["end"]) for vs,vv in rus)]
 ed=[x for x in ipg[n] if x["type"]=="EDITORIAL_COMMENT_IN_AUTHOR_TEXT"]
 mismatches=[x for x in ipg[n] if x["type"] in ("CHECK_QUOTE_MISMATCH","INVALID_REFERENCE")]
 ar=sum(len(p["ar"]) for p in ps); ru=sum(len(p["ru"]) for p in ps)
 flags=[]
 if missing:flags.append("QURAN_REFERENCE_NOT_EXPLICIT_IN_RUSSIAN")
 if ed:flags.append("POSSIBLE_EDITORIAL_INSERTION_IN_AUTHOR_PROSE")
 if mismatches:flags.append("QURAN_QUOTATION_NEEDS_VISUAL_COLLATION")
 if ru/ar<.95:flags.append("SHORT_TRANSLATION_POSSIBLY_ABRIDGED")
 if n in (448,463,507,622):flags.append("TARGETED_CORRECTION_THIS_VERSION")
 pages.append({"printed_page":n,"paragraph_ids":sorted(ids),"arabic_characters":ar,"russian_characters":ru,
    "russian_arabic_character_ratio":round(ru/ar,3),
    "arabic_quran_refs":[x["ref"] for x in qs],
    "quran_refs_not_explicit_in_russian":sorted(set(missing)),
    "automated_mismatch_indicators":len(mismatches),
    "editorial_intrusion_indicators":len(ed),
    "flags":flags,
    "verified_against_print_scan":False,
    "translation_independently_checked":False,
    "all_quran_quotes_verified":False,
    "all_hadith_isnads_checked":False,
    "expert_signed_off":False,
    "ready_to_publish":False})
count=collections.Counter(flag for page in pages for flag in page["flags"])
summary={"status":"FULL_PAGE_COVERAGE_TRIAGE_NOT_FULL_TRANSLATION_VERIFICATION",
 "printed_pages_registered":len(pages),
 "pages_with_automatic_flags":sum(bool(x["flags"]) for x in pages),
 "flag_counts":dict(count),
 "fully_independently_verified_pages":0,
 "unflagged_page_does_not_equal_verified":True,
 "note":"Literal references, length ratios, and automated Quran checks are leads for human review, not proof of error.",
 "pages":pages}
out=ROOT/"research/volume2-scholarly-audit/page-by-page-reconciliation-register.json"
out.write_text(json.dumps(summary,ensure_ascii=False,indent=2)+"\n")
print(json.dumps({k:v for k,v in summary.items() if k!="pages"},ensure_ascii=False))
