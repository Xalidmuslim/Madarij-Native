#!/usr/bin/env python3
"""Prepare pages 421-520 of chosen volume-2 2019 Arabic original for translation."""
import json
from pathlib import Path
r=Path(__file__).resolve().parents[1]
rows=[json.loads(x) for x in (r/"research/volume2-source/arabic_pages.jsonl").read_text(encoding="utf8").splitlines() if x.strip()]
assert len(rows)==659 and [p["printed_page"] for p in rows]==list(range(1,660))
pages={p["printed_page"]:p for p in rows}
out=r/"research/volume2-batch-421-520/original-arabic"
out.mkdir(parents=True,exist_ok=True)
for first in range(421,521,10):
    a=[{"printed_page":p,"arabic":pages[p]["arabic_body"],"source_status":pages[p]["status"]} for p in range(first,first+10)]
    assert all(x["arabic"].strip() for x in a)
    file=out/f"arabic-pages-{first:03d}-{first+9:03d}.json"
    file.write_text(json.dumps({"volume":2,"printed_pages":[first,first+9],"edition":"Nabil ibn Nassar al-Sindi, Dar Ataat al-Ilm / Dar Ibn Hazm, 2019","pages":a,"visual_collation_complete":False},ensure_ascii=False,indent=2)+"\n",encoding="utf8")
    print(file,len(a),sum(len(x["arabic"]) for x in a))
