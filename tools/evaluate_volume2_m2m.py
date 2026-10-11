#!/usr/bin/env python3
"""Second QUALITY PILOT only; does not alter the app.

MIT-licensed multilingual M2M100 model vs Arabic text of pages 7-9.
Outputs remain unreviewed and are never installed as book text automatically.
"""
import argparse
import json
import re
from pathlib import Path

from translate_volume2_machine import prepare, read_candidates, piecewise

def main():
    rows=[r for r in prepare(read_candidates()) if r["page_start"] in range(7,10)]
    import torch
    from transformers import M2M100ForConditionalGeneration,M2M100Tokenizer
    torch.set_num_threads(2)
    model_id="facebook/m2m100_418M"
    tokenizer=M2M100Tokenizer.from_pretrained(model_id,src_lang="ar")
    tokenizer.src_lang="ar"
    model=M2M100ForConditionalGeneration.from_pretrained(model_id)
    model.eval()
    out=[]
    for n,row in enumerate(rows):
        chunks=piecewise(tokenizer,row["arabic"],max_tokens=300)
        rparts=[]
        for p in chunks:
            inp=tokenizer(p,return_tensors="pt",truncation=False)
            with torch.inference_mode():
                g=model.generate(**inp,forced_bos_token_id=tokenizer.get_lang_id("ru"),max_new_tokens=450,num_beams=4)
            rparts+=tokenizer.batch_decode(g,skip_special_tokens=True)
        ru=" ".join(rparts).strip()
        assert ru and re.search(r"[\u0400-\u052F]",ru)
        out.append({**row,"ru":ru,"model":model_id,"translation_status":"machine_draft_unverified","publishable":False})
        print(f"{n+1}/{len(rows)} {row['id']} RU={ru[:180]}",flush=True)
    d=Path("volume2-pilot-m2m")
    d.mkdir(exist_ok=True)
    (d/"pilot.jsonl").write_text("".join(json.dumps(x,ensure_ascii=False)+"\n" for x in out),encoding="utf-8")
    (d/"audit.json").write_text(json.dumps({"model":model_id,"rows":len(out),"pages":[7,8,9],"reviewed":False,"can_publish":False},ensure_ascii=False,indent=2),encoding="utf-8")

if __name__=="__main__":
    main()
