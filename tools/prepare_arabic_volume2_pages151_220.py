#!/usr/bin/env python3
"""Extract exactly printed Arabic pages 151–220 from the selected 2019 edition.

No modern editor footnotes should be introduced into the author's prose.
The extraction is for fidelity-oriented translation; it is NOT an approval
of the interpretation or of source OCR/transcription.
"""
import json
from pathlib import Path

root=Path(__file__).resolve().parents[1]
path=root/"research/volume2-source/arabic_pages.jsonl"
items=[json.loads(x) for x in path.read_text(encoding="utf-8").splitlines() if x.strip()]
assert len(items)==659
assert [x["printed_page"] for x in items]==list(range(1,660))
lookup={x["printed_page"]:x for x in items}
out=root/"research/volume2-batch-151-220/original-arabic"
out.mkdir(parents=True,exist_ok=True)
for first in range(151,221,10):
    obj={"volume":2,"printed_pages":[first,first+9],
        "source":"Nabil ibn Nassar al-Sindi, Dar Ataat al-Ilm / Dar Ibn Hazm, 2019",
        "pages":[{"printed_page":p,"arabic":lookup[p]["arabic_body"],
            "editor_notes_excluded":True,
            "source_status":lookup[p]["status"]} for p in range(first,first+10)]}
    for item in obj["pages"]:
        if len(item["arabic"])<=250:
            print("SOURCE WARNING: unusually short printed page",item["printed_page"],
                  "characters",len(item["arabic"]))
    dest=out/f"arabic-pages-{first:03d}-{first+9:03d}.json"
    dest.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(dest, dest.stat().st_size)
