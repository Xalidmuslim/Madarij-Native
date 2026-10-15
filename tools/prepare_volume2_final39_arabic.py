#!/usr/bin/env python3
"""Extract final 39 printed Arabic pages, unedited original source corpus."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]
records=[json.loads(line) for line in (root/"research/volume2-source/arabic_pages.jsonl").read_text(encoding="utf8").splitlines() if line.strip()]
assert len(records)==659
assert [row["printed_page"] for row in records]==list(range(1,660))
index={row["printed_page"]:row for row in records}
out=root/"research/volume2-batch-621-659/original-arabic"
out.mkdir(parents=True,exist_ok=True)
for first,last in [(621,630),(631,640),(641,650),(651,659)]:
    pages=[{"printed_page":p,"arabic":index[p]["arabic_body"],"status":index[p]["status"]} for p in range(first,last+1)]
    assert all(x["arabic"] for x in pages)
    f=out/f"arabic-pages-{first:03d}-{last:03d}.json"
    f.write_text(json.dumps({"volume":2,"printed_pages":[first,last],"pages":pages,"visual_collation_complete":False},ensure_ascii=False,indent=2)+"\n",encoding="utf8")
    print(first,last,len(pages),sum(len(x["arabic"]) for x in pages))
