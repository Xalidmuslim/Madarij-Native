#!/usr/bin/env python3
"""Prepare original Arabic body for printed pages 321-420 of selected edition."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]
rows=[json.loads(l) for l in (root/"research/volume2-source/arabic_pages.jsonl").read_text(encoding="utf8").splitlines() if l]
assert len(rows)==659 and [x["printed_page"] for x in rows]==list(range(1,660))
by_page={r["printed_page"]:r for r in rows}
dest=root/"research/volume2-batch-321-420/original-arabic"
dest.mkdir(parents=True,exist_ok=True)
for start in range(321,421,10):
    subset=[{"printed_page":p,"arabic":by_page[p]["arabic_body"],"status":by_page[p]["status"]} for p in range(start,start+10)]
    assert len(subset)==10 and all(x["arabic"].strip() for x in subset)
    target=dest/f"arabic-pages-{start:03d}-{start+9:03d}.json"
    target.write_text(json.dumps({"volume":2,"printed_pages":[start,start+9],"pages":subset,"edition":"Nabil ibn Nassar al-Sindi; Dar Ataat al-Ilm / Dar Ibn Hazm, 2019","translation_verified":False},ensure_ascii=False,indent=2)+"\n",encoding="utf8")
    print(target.name,sum(len(x["arabic"]) for x in subset))
