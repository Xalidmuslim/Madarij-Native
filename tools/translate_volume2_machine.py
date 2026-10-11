#!/usr/bin/env python3
"""Offline, auditable MACHINE translation. NOT an approved Russian book text.

The 2019 printed Arabic source is stored in research/volume2-translation-review.
Each draft retains its Arabic source, original candidate IDs, page range and
review flags. The model is generic, not a specialist in classical Islamic prose.
Never mark these results as scholar-verified or publication-ready.
"""
import argparse
import hashlib
import json
import re
from pathlib import Path

SOURCE = "research/volume2-translation-review/translation_queue.jsonl"
MODEL = "Helsinki-NLP/opus-mt-ar-ru"
ARABIC = re.compile(r"[\u0621-\u064A]")
ENDING = re.compile(r"[.!؟؛]\s*(?:\([¬][^)]*\)\s*)*$")
REFERENCE = re.compile(r"(?:قال الله|قال تعالى|﴿|ﷺ|رسول الله|النبي|الحديث|البخاري|مسلم|الآية)")
FOOTNOTE = re.compile(r"\(¬[٠-٩0-9]+\)")
HEADING = re.compile(r"^(?:فصل|باب)\s*$")

def read_candidates():
    rows = [json.loads(s) for s in Path(SOURCE).read_text(encoding="utf-8").splitlines() if s.strip()]
    assert len(rows) == 3210, "Input source inventory unexpectedly changed"
    assert [r["printed_page"] for r in rows] == sorted(r["printed_page"] for r in rows)
    assert len({r["id"] for r in rows}) == len(rows)
    return rows

def prepare(rows, first=7):
    rows = [dict(r) for r in rows if first <= r["printed_page"] <= 659]
    by_page = {}
    for r in rows:
        by_page.setdefault(r["printed_page"], []).append(r)
    result = []
    for page in range(first, 660):
        for index, row in enumerate(by_page.get(page, [])):
            text = FOOTNOTE.sub("", row["arabic"]).strip()
            if not text:
                continue
            # A physical page turn is not a new paragraph: join if the source
            # sentence did not end and the new page has no section heading.
            if index == 0 and result and result[-1]["page_end"] == page - 1:
                prev = result[-1]
                if (not ENDING.search(prev["arabic"]) and
                    not HEADING.fullmatch(text) and
                    ARABIC.search(text)):
                    prev["arabic"] += " " + text
                    prev["page_end"] = page
                    prev["source_candidate_ids"].append(row["id"])
                    prev["boundary_annotation"] = "joined_across_pdf_page_unverified"
                    continue
            result.append({
                "id": row["id"], "page_start": page, "page_end": page,
                "source_candidate_ids": [row["id"]], "arabic": text,
                "boundary_annotation": "original_source_line_unverified",
            })
    return result

def piecewise(tokenizer, text, max_tokens=350):
    ids = tokenizer.encode(text, add_special_tokens=False)
    if len(ids) <= max_tokens:
        return [text]
    # Split on whitespace rather than silently truncating religious content.
    words = text.split()
    result, segment = [], []
    for word in words:
        candidate = " ".join(segment + [word])
        if segment and len(tokenizer.encode(candidate, add_special_tokens=False)) > max_tokens:
            result.append(" ".join(segment))
            segment = [word]
        else:
            segment.append(word)
    if segment:
        result.append(" ".join(segment))
    if any(len(tokenizer.encode(p, add_special_tokens=False)) > max_tokens for p in result):
        raise ValueError("Unbreakable token segment exceeds model context")
    assert " ".join(result) == " ".join(words)
    return result

def translate(model, tokenizer, texts, batch_size=6):
    import torch
    output = []
    for start in range(0, len(texts), batch_size):
        batch = texts[start:start + batch_size]
        data = tokenizer(batch, return_tensors="pt", padding=True)
        with torch.inference_mode():
            result = model.generate(**data, max_new_tokens=490, num_beams=3)
        output.extend(tokenizer.batch_decode(result, skip_special_tokens=True))
    return output

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--shard", type=int, default=0)
    parser.add_argument("--shards", type=int, default=8)
    parser.add_argument("--first-page", type=int, default=7)
    parser.add_argument("--last-page", type=int, default=659)
    parser.add_argument("--output", required=True)
    parser.add_argument("--batch-size", type=int, default=6)
    args = parser.parse_args()
    assert 0 <= args.shard < args.shards
    assert 7 <= args.first_page <= args.last_page <= 659
    source = prepare(read_candidates())
    # Partition by page of first source occurrence, never by raw array position.
    total_pages = args.last_page - args.first_page + 1
    block = (total_pages + args.shards - 1) // args.shards
    begin = args.first_page + args.shard * block
    end = min(args.last_page, begin + block - 1)
    selected = [s for s in source if begin <= s["page_start"] <= end]
    if not selected:
        raise RuntimeError("No source candidates in selected shard")
    print(f"Translating {len(selected)} source paragraphs, starting printed pages {begin}-{end}", flush=True)

    import torch
    from transformers import MarianMTModel, MarianTokenizer
    torch.set_num_threads(2)
    tokenizer = MarianTokenizer.from_pretrained(MODEL)
    model = MarianMTModel.from_pretrained(MODEL)
    model.eval()
    chunks, positions = [], []
    for i, row in enumerate(selected):
        if not ARABIC.search(row["arabic"]):
            continue
        for segment in piecewise(tokenizer, row["arabic"]):
            positions.append(i)
            chunks.append(segment)
    translated_parts = translate(model, tokenizer, chunks, args.batch_size)
    translations = [[] for _ in selected]
    for i, text in zip(positions, translated_parts):
        translations[i].append(text.strip())
    out_rows = []
    for row, parts in zip(selected, translations):
        ru = " ".join(parts).strip() if parts else "Конец раздела."
        cyrillic = len(re.findall(r"[\u0400-\u052f]", ru))
        risk = []
        if not ARABIC.search(row["arabic"]):
            risk.append("non_prose_decoration")
        if ARABIC.search(row["arabic"]) and cyrillic < 5:
            risk.append("machine_output_no_russian")
        if REFERENCE.search(row["arabic"]):
            risk.append("quran_hadith_or_scholarly_citation_check_required")
        if len(parts) > 1:
            risk.append("translated_in_chunks")
        if row["page_end"] > row["page_start"]:
            risk.append("joined_pdf_pages_need_human_boundary_review")
        out_rows.append({
            **row,
            "ru":ru,
            "translation_status":"machine_draft_unverified",
            "model": MODEL,
            "source_sha256":hashlib.sha256(row["arabic"].encode()).hexdigest(),
            "translation_checked_by_human":False,
            "independent_religious_review":False,
            "publishable":False,
            "qa_flags":risk
        })
    path = Path(args.output)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("".join(json.dumps(r, ensure_ascii=False)+"\n" for r in out_rows), encoding="utf-8")
    report = {
        "model":MODEL,"first_page":begin,"last_page":end,
        "rows":len(out_rows),
        "arabic_characters":sum(len(r["arabic"]) for r in out_rows),
        "russian_characters":sum(len(r["ru"]) for r in out_rows),
        "rows_without_russian":sum("machine_output_no_russian" in r["qa_flags"] for r in out_rows),
        "joined_page_spans":sum(r["page_end"] > r["page_start"] for r in out_rows),
        "status":"unreviewed_machine_draft_not_for_publication",
        "independently_verified":False
    }
    path.with_suffix(".audit.json").write_text(json.dumps(report, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False), flush=True)
    if report["rows_without_russian"]:
        raise RuntimeError("Empty/wrong-language translations detected")

if __name__ == "__main__":
    main()
