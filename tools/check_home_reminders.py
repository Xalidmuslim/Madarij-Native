#!/usr/bin/env python3
"""Validate exact reminder quotations and traceability against bundled corpus.json."""
import json
import pathlib
import re

root = pathlib.Path(__file__).resolve().parents[1]
corpus = json.loads((root / "app/src/main/assets/corpus.json").read_text())
source = (root / "app/src/main/java/ru/madarij/nativeapp/KnowledgeReminders.kt").read_text()
pattern = re.compile(r'KnowledgeReminder\(("((?:\\.|[^"\\])*)"),\s*("((?:\\.|[^"\\])*)"),\s*("((?:\\.|[^"\\])*)"),\s*(\d+)\)')
found = [(json.loads(a),json.loads(b),json.loads(c),int(page)) for a,_,b,_,c,_,page in pattern.findall(source)]
assert 20 <= len(found) <= 30, f"Expected 20-30 reminders, got {len(found)}"
assert len({x[2] for x in found}) == len(found), "Duplicate paragraph ids"
lookup = {(ch["id"],p["id"]):p for ch in corpus["chapters"] for p in ch["paragraphs"]}
for quote,chapter,pid,page in found:
    p=lookup[(chapter,pid)]
    assert p["role"] == "author", pid
    assert quote in p["ru"], f"Quote not verbatim: {pid}"
    assert page in p["source_pages"], f"Wrong source page: {pid}"
print(f"Verified {len(found)} verbatim author-role quotations and page refs")
