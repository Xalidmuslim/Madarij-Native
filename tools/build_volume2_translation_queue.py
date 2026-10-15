#!/usr/bin/env python3
"""Prepare Arabic-to-Russian translation and religious-source review queues.

Logical paragraph boundaries across PDF pages MUST be checked by a human.
Everything is marked unpublished and untranslated until it is actually reviewed.
"""
import argparse
import json
import re
from pathlib import Path

QURAN = re.compile(r"﴿|قال (?:الله|تعالى)|\{[^}]+\}")
HADITH = re.compile(r"رسول الله|رسولُ الله|النبي|النَّبي|صحيح البخاري|صحيح مسلم|حديث")
AUTHOR_QUOTE = re.compile(r"قال ابن|قال شيخ الإسلام|قال الهروي|قلتُ|قال الإمام")

def build(source, destination):
    pages=[json.loads(x) for x in Path(source).read_text(encoding="utf-8").splitlines() if x.strip()]
    folder=Path(destination)
    folder.mkdir(parents=True,exist_ok=True)
    fragments=[]
    citations=[]
    for page in pages:
        n=page["printed_page"]
        if page.get("status") not in ("needs_visual_collation", "front_matter_image_only"):
            continue
        for ix,arabic in enumerate(x.strip() for x in page.get("arabic_body","").split("\n") if x.strip()):
            pid=f"madarij-v2-p{n:03d}-candidate-{ix+1:02d}"
            fragments.append({
                "id":pid,"volume":2,"printed_page":n,"line_in_page":ix+1,
                "arabic":arabic,"russian":None,"status":"untranslated",
                "page_scan":page.get("source_image"),"source_html":page.get("source_html"),
                "source_collation_status":"unchecked",
                "paragraph_boundary_status":"candidate_may_continue_on_next_page",
                "independent_translation_review":False,
                "religious_citation_review":False,"publishable":False
            })
            for kind,pat in (("quran_candidate",QURAN),("hadith_candidate",HADITH),
                             ("historical_quotation",AUTHOR_QUOTE)):
                if pat.search(arabic):
                    citations.append({"fragment_id":pid,"page":n,"category":kind,
                        "source_text_excerpt":arabic[:220],
                        "review_status":"needs_primary_source_and_context"})
    for filename,data in (("translation_queue.jsonl",fragments),
                          ("citation_review_queue.jsonl",citations)):
        (folder/filename).write_text("".join(
            json.dumps(item,ensure_ascii=False)+"\n" for item in data),encoding="utf-8")
    report={"printed_pages_in_source":len(pages),"untranslated_fragments":len(fragments),
            "potential_quotes_for_review":len(citations),
            "independently_reviewed_translations":0,"independently_verified_hadiths":0,
            "verified_paragraph_boundaries":0,"publishable":False}
    (folder/"translation_queue_audit.json").write_text(
        json.dumps(report,ensure_ascii=False,indent=2),encoding="utf-8")
    print(json.dumps(report,ensure_ascii=False))

if __name__=="__main__":
    p=argparse.ArgumentParser()
    p.add_argument("--source",required=True)
    p.add_argument("--output",required=True)
    a=p.parse_args()
    build(a.source,a.output)
