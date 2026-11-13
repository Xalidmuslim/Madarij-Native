#!/usr/bin/env python3
"""Offline Russian translation before/after comparisons; no separate Arabic column."""
from __future__ import annotations

import difflib
import html
import json
import re
from collections import defaultdict
from pathlib import Path

from tools.build_reviewed_bilingual import MANIFEST_DIR, ROOT, SOURCE, reviewed_corpus

OUTPUT_HTML = ROOT / "research/editorial/volume1/russian-before-after.html"
OUTPUT_JSON = ROOT / "research/editorial/volume1/russian-before-after.json"


def collect_changes(source: Path = SOURCE, manifest_dir: Path = MANIFEST_DIR) -> dict:
    original = json.loads(source.read_text(encoding="utf-8"))
    revised, report = reviewed_corpus(source, manifest_dir)
    before = {p["id"]: p for ch in original["chapters"] for p in ch["paragraphs"]}
    grouped = defaultdict(list)
    scope_by_id = {}
    for number, path in enumerate(sorted(manifest_dir.glob("batch[0-9][0-9]_pages*.json")), 1):
        manifest = json.loads(path.read_text(encoding="utf-8"))
        for entry in manifest["replacements"]:
            grouped[entry["paragraphId"]].append(entry["reason"])
        for idx in range(manifest["scope"]["first_section"], manifest["scope"]["last_section"] + 1):
            if idx in scope_by_id:
                raise ValueError("Overlapping editorial batches")
            scope_by_id[idx] = number

    records = []
    for idx, chapter in enumerate(revised["chapters"], 1):
        for paragraph in chapter["paragraphs"]:
            pid = paragraph["id"]
            old = before[pid]
            if old["ru"] == paragraph["ru"]:
                continue
            if idx not in scope_by_id or not grouped[pid]:
                raise ValueError("Unmanifested editorial change: " + pid)
            records.append({
                "batch": scope_by_id[idx],
                "section": idx,
                "sectionTitle": chapter["title"],
                "paragraphId": pid,
                "pages": paragraph.get("source_pages", []),
                "role": paragraph.get("role", "author"),
                "oldRussian": old["ru"],
                "newRussian": paragraph["ru"],
                "reasons": grouped[pid],
            })
    if len(records) != sum(b["edited_paragraphs"] for b in report["batches"]):
        raise ValueError("Edited paragraph count disagrees with review report")
    return {
        "format": "madarij-ru-before-after",
        "volume": 1,
        "reviewedSections": max(scope_by_id),
        "totalSections": len(revised["chapters"]),
        "totalParagraphs": sum(len(c["paragraphs"]) for c in revised["chapters"]),
        "corrections": sum(b["replacements"] for b in report["batches"]),
        "editedParagraphs": len(records),
        "notIndependentlyVerified": True,
        "changes": records
    }


def highlighted(old: str, new: str) -> tuple[str, str]:
    """Word-level highlighting; never modify or normalize the underlying texts."""
    a = re.findall(r"\s+|[^\s]+", old)
    b = re.findall(r"\s+|[^\s]+", new)
    matcher = difflib.SequenceMatcher(None, a, b, autojunk=False)
    left, right = [], []
    for kind, a1, a2, b1, b2 in matcher.get_opcodes():
        aa = html.escape("".join(a[a1:a2]))
        bb = html.escape("".join(b[b1:b2]))
        left.append(aa if kind == "equal" else
                    ('<mark class="removed">' + aa + '</mark>' if aa else ""))
        right.append(bb if kind == "equal" else
                     ('<mark class="added">' + bb + '</mark>' if bb else ""))
    return "".join(left), "".join(right)


def render(report: dict, output: Path = OUTPUT_HTML) -> Path:
    changes = report["changes"]
    latest = max(row["batch"] for row in changes)
    esc = lambda value: html.escape(str(value), quote=True)
    page = [
        '<!doctype html><html lang="ru"><head><meta charset="utf-8">',
        '<meta name="viewport" content="width=device-width,initial-scale=1">',
        '<title>Степени идущих — русский перевод: было / стало</title>',
        """<style>
:root{color-scheme:light;--bg:#f2efe8;--paper:#fffefa;--text:#27333a;--muted:#66747a;--border:#d7d8d1}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font-family:system-ui,-apple-system,sans-serif}
header{padding:24px max(16px,calc((100vw - 1160px)/2));background:#e8e4db;border-bottom:1px solid var(--border)}
h1{font-size:clamp(22px,3vw,30px);margin:0 0 10px}header p{margin:5px 0;line-height:1.6}
.wrap{max-width:1160px;margin:auto;padding:16px}.intro{background:var(--paper);border:1px solid var(--border);border-radius:12px;padding:14px 16px;line-height:1.6}
nav{display:flex;gap:8px;flex-wrap:wrap;padding:16px 0}nav a{border:1px solid var(--border);background:var(--paper);padding:8px 12px;border-radius:8px;text-decoration:none;color:#354e60}
details.batch{background:var(--paper);border:1px solid var(--border);border-radius:12px;margin-bottom:18px;overflow:hidden}
summary{font-weight:650;padding:16px;cursor:pointer;background:#f6f4ed;list-style-position:inside}
article{border-top:1px solid var(--border);padding:16px}.meta{font-size:12px;color:var(--muted);margin:0 0 12px}
.columns{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:15px}
.pane{border:1px solid var(--border);background:#fffefa;border-radius:10px;overflow:hidden;min-width:0}
.pane h3{font-size:13px;letter-spacing:.025em;margin:0;padding:9px 14px;background:#f4f2eb;border-bottom:1px solid var(--border)}
.txt{font-family:Georgia,'Times New Roman',serif;white-space:pre-wrap;overflow-wrap:anywhere;line-height:1.85;padding:15px;font-size:16px}
mark{padding:1px 1px;border-radius:2px;color:inherit}.removed{background:#f4deda}.added{background:#dcece2}
.reason{font-size:13px;line-height:1.6;color:#58666d;margin:12px 0 0}
.small{color:var(--muted);font-size:12px}
@media(max-width:760px){.columns{grid-template-columns:1fr}.wrap{padding:10px}article{padding:12px}.txt{font-size:16px;padding:13px}}
@media print{details.batch{break-inside:auto}.pane{break-inside:avoid}nav{display:none}}
</style>""",
        '</head><body><header><h1>Степени идущих · Том 1</h1>',
        '<p>Сравнение русского перевода: <strong>прежний текст → исправленный текст</strong>.</p>',
        f'<p class="small">Проверено разделов: {report["reviewedSections"]} из {report["totalSections"]}; '
        f'изменено абзацев: {report["editedParagraphs"]}; точечных исправлений: {report["corrections"]}.</p>',
        '</header><main class="wrap"><div class="intro">'
        'Слева показан прежний русский текст, справа — исправленный. '
        'Выделены изменённые слова и выражения. Показаны только абзацы с редакторскими правками; '
        'все остальные абзацы оставлены без изменений. '
        'Аяты и хадисы не переписывались ради художественного стиля. '
        'Проверка с печатным изданием и независимая богословская рецензия ещё не завершены.'
        '</div><nav aria-label="Переход к партиям">'
    ]
    grouped = defaultdict(list)
    for entry in changes:
        grouped[entry["batch"]].append(entry)
    for batch in sorted(grouped, reverse=True):
        records = grouped[batch]
        lo = min(r["section"] for r in records)
        hi = max(r["section"] for r in records)
        page.append(f'<a href="#batch-{batch}">Партия {batch} · разделы {lo}–{hi}</a>')
    page.append('</nav>')
    for batch in sorted(grouped, reverse=True):
        rows = grouped[batch]
        lo, hi = min(x["section"] for x in rows), max(x["section"] for x in rows)
        page.append(f'<details class="batch" id="batch-{batch}" {"open" if batch == latest else ""}>'
                    f'<summary>Партия {batch} · разделы {lo}–{hi} · {len(rows)} изменённых блоков</summary>')
        for row in rows:
            left, right = highlighted(row["oldRussian"], row["newRussian"])
            page.append('<article>')
            page.append(f'<p class="meta">Раздел {row["section"]} · страницы {esc(", ".join(map(str,row["pages"])))} '
                        f'· {esc(row["paragraphId"])}</p>')
            page.append('<div class="columns"><div class="pane"><h3>БЫЛО — русский перевод</h3>'
                        f'<div class="txt">{left}</div></div>'
                        '<div class="pane"><h3>СТАЛО — русский перевод</h3>'
                        f'<div class="txt">{right}</div></div></div>')
            page.append('<p class="reason"><strong>Почему изменено:</strong> ' +
                        esc("; ".join(dict.fromkeys(row["reasons"]))) + '</p></article>')
        page.append('</details>')
    page.append('</main></body></html>')
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(page), encoding="utf-8")
    return output


def build(source: Path = SOURCE, manifest_dir: Path = MANIFEST_DIR,
          html_output: Path = OUTPUT_HTML, json_output: Path = OUTPUT_JSON) -> dict:
    report = collect_changes(source, manifest_dir)
    json_output.parent.mkdir(parents=True, exist_ok=True)
    json_output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    render(report, html_output)
    print(f"PASS: Russian before/after, {report['editedParagraphs']} edited paragraphs, "
          f"{report['corrections']} corrections, {report['reviewedSections']} sections")
    return report


if __name__ == "__main__":
    build()
