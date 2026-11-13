#!/usr/bin/env python3
"""Build offline Russian before/after and separate complete Arabic/Russian HTML."""
from __future__ import annotations

import difflib
import html
import json
import re
from collections import defaultdict
from pathlib import Path

from tools.build_reviewed_bilingual_volume2 import (
    ROOT, SOURCE, MANIFEST_DIR, OUTPUT, MANIFEST_PATTERN, reviewed_corpus
)

RU_JSON = MANIFEST_DIR / "russian-before-after.json"
RU_HTML = MANIFEST_DIR / "russian-before-after.html"
AR_HTML = MANIFEST_DIR / "working-bilingual-compare.html"
STYLE = """
<style>
:root{color-scheme:light;--bg:#f3f1eb;--paper:#fffdf8;--ink:#263036;--line:#d5d9d8;--muted:#5e6870}
*{box-sizing:border-box}body{background:var(--bg);color:var(--ink);font-family:system-ui,-apple-system,sans-serif;margin:0}
header{background:#e8e5de;padding:22px max(16px,calc((100vw - 1120px)/2));border-bottom:1px solid var(--line)}
h1{font-size:24px;margin:0 0 8px}header p{margin:5px 0;line-height:1.5}
main{max-width:1120px;margin:auto;padding:16px}
details{background:var(--paper);border:1px solid var(--line);border-radius:11px;margin:12px 0;overflow:hidden}
summary{background:#f8f6f0;padding:14px;cursor:pointer;font-weight:650}
article{padding:15px;border-top:1px solid var(--line)}
.pair{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:14px}
.pane{border:1px solid var(--line);border-radius:8px;min-width:0;background:white}
.pane h3{font:600 13px system-ui;margin:0;padding:9px;border-bottom:1px solid var(--line);background:#f8f7f3}
.txt{font:16px/1.8 Georgia,'Times New Roman',serif;padding:13px;white-space:pre-wrap;overflow-wrap:anywhere}
.meta,.reason{font:13px/1.6 system-ui;color:var(--muted);margin:5px 0 10px}
.ar{direction:rtl;text-align:right;font-size:19px;font-family:'Noto Naskh Arabic','Amiri',serif}
mark.removed{background:#f5ded9}mark.added{background:#dbeddf}mark{color:inherit}
@media(max-width:730px){main{padding:9px}.pair{grid-template-columns:1fr}.txt{font-size:16px}}
</style>
"""


def highlight(old: str, new: str) -> tuple[str, str]:
    a = re.findall(r"\s+|[^\s]+", old)
    b = re.findall(r"\s+|[^\s]+", new)
    before, after = [], []
    for op, a1, a2, b1, b2 in difflib.SequenceMatcher(
        None, a, b, autojunk=False
    ).get_opcodes():
        left = html.escape("".join(a[a1:a2]))
        right = html.escape("".join(b[b1:b2]))
        before.append(left if op == "equal" else '<mark class="removed">' + left + '</mark>')
        after.append(right if op == "equal" else '<mark class="added">' + right + '</mark>')
    return "".join(before), "".join(after)


def collect_changes(source: Path = SOURCE, manifest_dir: Path = MANIFEST_DIR) -> dict:
    before = json.loads(source.read_text(encoding="utf-8"))
    after, report = reviewed_corpus(source, manifest_dir)
    prev_by_id = {
        p["id"]: p for chapter in before["chapters"] for p in chapter["paragraphs"]
    }
    reasons = defaultdict(list)
    batches = {}
    for number, path in enumerate(sorted(manifest_dir.glob(MANIFEST_PATTERN)), 1):
        manifest = json.loads(path.read_text(encoding="utf-8"))
        for pid in manifest["scope"]["paragraph_ids"]:
            if pid in batches:
                raise ValueError("Overlapping reviewed IDs: " + pid)
            batches[pid] = number
        for edit in manifest["replacements"]:
            reasons[edit["paragraphId"]].append(edit["reason"])
    records = []
    for chapter in after["chapters"]:
        for paragraph in chapter["paragraphs"]:
            pid = paragraph["id"]
            old_ru = prev_by_id[pid]["ru"]
            if paragraph["ru"] == old_ru:
                continue
            if pid not in batches or not reasons[pid]:
                raise ValueError("Unmanifested edit: " + pid)
            records.append({
                "batch": batches[pid],
                "chapterId": chapter["id"],
                "chapterTitle": chapter["title"],
                "paragraphId": pid,
                "source": paragraph["source"],
                "oldRussian": old_ru,
                "newRussian": paragraph["ru"],
                "reasons": reasons[pid],
            })
    if len(records) != report["edited_paragraphs"]:
        raise ValueError("Review report and changed paragraph count differ")
    return {
        "format": "madarij-volume2-russian-before-after",
        "totalChapters": report["total_chapters"],
        "totalParagraphs": report["total_paragraphs"],
        "reviewedParagraphs": report["reviewed_paragraphs"],
        "corrections": report["replacements"],
        "editedParagraphs": len(records),
        "literaryReviewOnly": True,
        "notIndependentlyVerified": True,
        "changes": records,
    }


def render_ru(data: dict, path: Path = RU_HTML) -> None:
    esc = html.escape
    head = (
        '<!doctype html><html lang="ru"><head><meta charset="utf-8">'
        '<meta name="viewport" content="width=device-width,initial-scale=1">'
        '<title>Степени идущих — том 2 — русский перевод: было / стало</title>'
        + STYLE
        + '</head><body><header><h1>Степени идущих · Том 2 · Было / стало</h1>'
        + f'<p>Просмотрено блоков: {data["reviewedParagraphs"]}. '
        + f'Исправлений: {data["corrections"]}; изменённых блоков: {data["editedParagraphs"]}.</p>'
        + '<p>Только русский перевод. Арабский оригинал, аяты, хадисы и все цитаты сохранены.</p>'
        + '<p>Научная и богословская сверка не проводилась.</p></header><main>'
    )
    chunks = [head]
    groups = defaultdict(list)
    for item in data["changes"]:
        groups[item["batch"]].append(item)
    for num in sorted(groups, reverse=True):
        chunks.append(f'<details open><summary>Партия {num} · {len(groups[num])} изменённых блоков</summary>')
        for item in groups[num]:
            left, right = highlight(item["oldRussian"], item["newRussian"])
            chunks.append(
                '<article><p class="meta">' + esc(item["paragraphId"]) + ' · '
                + esc(item["source"]) + '</p><div class="pair">'
                + '<div class="pane"><h3>БЫЛО</h3><div class="txt">' + left + '</div></div>'
                + '<div class="pane"><h3>СТАЛО</h3><div class="txt">' + right + '</div></div>'
                + '</div><p class="reason">Причины: '
                + esc("; ".join(dict.fromkeys(item["reasons"]))) + '</p></article>'
            )
        chunks.append('</details>')
    chunks.append('</main></body></html>')
    path.write_text("\n".join(chunks), encoding="utf-8")


def render_bilingual(book: dict, path: Path = AR_HTML) -> None:
    esc = html.escape
    parts = [
        '<!doctype html><html lang="ru"><head><meta charset="utf-8">'
        '<meta name="viewport" content="width=device-width,initial-scale=1">'
        '<title>Степени идущих — том 2 — арабский оригинал и русский перевод</title>'
        + STYLE
        + '</head><body><header><h1>Степени идущих · Том 2</h1>'
        + '<p>Арабский оригинал и русский рабочий перевод · раздельные колонки.</p>'
        + '<p>Исходный арабский текст не изменялся. Независимая сверка не завершена.</p>'
        + '</header><main>'
    ]
    for i, chapter in enumerate(book["chapters"], 1):
        parts.append('<details ' + ('open' if i == 1 else '') + '><summary>'
                     + str(i) + '. ' + esc(chapter["title"]) + '</summary>')
        for paragraph in chapter["paragraphs"]:
            parts.append(
                '<article class="bilingual-pair"><p class="meta">'
                + esc(paragraph["id"]) + ' · ' + esc(paragraph["source"]) + '</p>'
                + '<div class="pair">'
                + '<div class="pane"><h3>Арабский оригинал</h3><div class="txt ar" lang="ar">'
                + esc(paragraph["ar"]) + '</div></div>'
                + '<div class="pane"><h3>Русский перевод</h3><div class="txt" lang="ru">'
                + esc(paragraph["ru"]) + '</div></div></div></article>'
            )
        parts.append("</details>")
    parts.append("</main></body></html>")
    path.write_text("\n".join(parts), encoding="utf-8")


def build(source: Path = SOURCE, manifest_dir: Path = MANIFEST_DIR,
          output_json: Path = RU_JSON, output_html: Path = RU_HTML,
          bilingual_html: Path = AR_HTML) -> dict:
    output_json.parent.mkdir(parents=True, exist_ok=True)
    report = collect_changes(source, manifest_dir)
    output_json.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n",
                           encoding="utf-8")
    render_ru(report, output_html)
    book, _ = reviewed_corpus(source, manifest_dir)
    render_bilingual(book, bilingual_html)
    print(f"PASS: Volume 2 literary export: {report['corrections']} replacements, "
          f"{report['editedParagraphs']} changed text blocks; 676 bilingual pairs")
    return report


if __name__ == "__main__":
    build()
