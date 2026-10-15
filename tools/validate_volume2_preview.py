#!/usr/bin/env python3
"""Static source-alignment and editorial-integrity checks (NOT religious certification).

Companion to app/src/main/assets/volume2-preview.json. Every reader paragraph
is compared against the fixed 2019 Arabic edition. This catches missing,
transposed and out-of-edition Arabic passages; it cannot prove translation
accuracy or the scholarly grades of narrations.
"""
import difflib
import json
from pathlib import Path
import re
import unicodedata

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/"research/volume2-source/arabic_pages.jsonl"
PREVIEW=ROOT/"app/src/main/assets/volume2-preview.json"
CONTENTS=ROOT/"app/src/main/assets/volume2-structure.json"
QUEUE=ROOT/"research/volume2-translation-review/translation_queue.jsonl"

def normalize_ar(text):
    text=re.sub(r"\(¬[^)]+\)", " ", text)
    text=unicodedata.normalize("NFKD",text)
    text="".join(c for c in text if not unicodedata.combining(c))
    text=re.sub("[إأآٱ]", "ا", text).replace("ى","ي").replace("ة","ه")
    return re.findall(r"[ء-ي]+",text)

def matched_fraction(a,b):
    return sum(k.size for k in difflib.SequenceMatcher(None,a,b,autojunk=False).get_matching_blocks())/max(1,len(a))

def main():
    pages=[json.loads(line) for line in SOURCE.read_text(encoding="utf8").splitlines() if line.strip()]
    assert len(pages)==659
    assert [p["printed_page"] for p in pages]==list(range(1,660))
    indexed={p["printed_page"]:p for p in pages}
    v=json.loads(PREVIEW.read_text(encoding="utf8"))
    toc=json.loads(CONTENTS.read_text(encoding="utf8"))
    assert v["volume"]==2 and not v["translation_independently_verified"] and not v["publication_ready"]
    ids=set()
    count=0
    for ch in v["chapters"]:
        assert ch["volume"]==2
        pars=ch["paragraphs"]
        assert [p["id"] for p in pars] == ch["expected_paragraph_ids"]
        for n,p in enumerate(pars):
            assert p["order"]==n and p["role"]=="author" and p["status"]=="draft_self_checked"
            assert p["id"] not in ids
            ids.add(p["id"])
            assert p["ru"].strip() and p["ar"].strip()
            assert "\ufffd" not in p["ru"] and not any(x.endswith("-") for x in p["ru"].splitlines())
            # Printed pages are explicit in the source tag; the range is not
            # automatically treated as a paragraph boundary.
            pages_match=re.search(r"стр\.\s*(\d+)(?:[–-](\d+))?",p["source"])
            assert pages_match,(p["id"],"missing page provenance")
            start=int(pages_match.group(1))
            finish=int(pages_match.group(2) or start)
            assert 1<=start<=finish<=659
            arabic="\n".join(indexed[k]["arabic_body"] for k in range(start,finish+1))
            ratio=matched_fraction(normalize_ar(p["ar"]),normalize_ar(arabic))
            assert ratio>=.80, (p["id"],"Arabic source differs or is clipped",round(ratio,3))
            count+=1
    topics=[t["paragraphId"] for g in toc["groups"] for c in g["items"] for t in c["topics"]]
    assert topics and len(topics)==len(set(topics)) and set(topics).issubset(ids)
    # Contents contains navigational headings, not a redundant entry for every paragraph.
    assert len([json.loads(l) for l in QUEUE.read_text().splitlines() if l.strip()])==3210
    print("PASS: 659 source pages; %s current reader paragraphs compared to Arabic text;" % count)
    print("PASS: source IDs, TOC links, paragraph formatting, unverified-status flags")
    print("NOT CERTIFIED: Russian semantic accuracy, page scan collation, Quran/hadith grading")
if __name__=="__main__":
    main()
