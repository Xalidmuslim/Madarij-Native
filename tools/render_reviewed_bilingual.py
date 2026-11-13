#!/usr/bin/env python3
"""Create a standalone, offline side-by-side Arabic/Russian comparison reader."""
from __future__ import annotations
import html
import json
from pathlib import Path
from tools.build_reviewed_bilingual import ROOT, OUTPUT

HTML_OUT = ROOT / "research/editorial/volume1/working-bilingual-compare.html"


def render(book: dict, output: Path = HTML_OUT, manifests: list[dict] | None = None) -> Path:
    manifests = manifests or []
    edited = {}
    reviewed = set()
    for b in manifests:
        start, end = b["scope"]["first_section"], b["scope"]["last_section"]
        reviewed.update(range(start, end + 1))
        for e in b["replacements"]:
            edited[e["paragraphId"]] = (start, end)

    esc = lambda t: html.escape(str(t), quote=True)
    chunks = [
        '<!doctype html><html lang="ru"><head><meta charset="utf-8">',
        '<meta name="viewport" content="width=device-width, initial-scale=1">',
        '<title>Степени идущих · Том 1 · Арабский оригинал и русский перевод</title>',
        """<style>
:root{color-scheme:light;--bg:#f5f2eb;--paper:#fffefa;--ink:#25303b;--line:#d9dad3;--accent:#486479}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--ink);font-family:system-ui,-apple-system,sans-serif}
header{padding:26px max(18px,calc((100vw - 1160px)/2));background:#e9e6de;border-bottom:1px solid var(--line)}
h1{font-size:clamp(21px,3vw,31px);margin:0 0 9px}p{margin:8px 0}small{font-size:12px}
.wrap{max-width:1160px;margin:auto;padding:16px}
.summary{border:1px solid var(--line);background:var(--paper);padding:14px 18px;border-radius:12px;margin-bottom:18px;line-height:1.65}
nav{display:flex;gap:7px;flex-wrap:wrap;margin:12px 0 20px}
nav a{color:var(--accent);background:var(--paper);border:1px solid var(--line);padding:5px 9px;border-radius:7px;text-decoration:none;font-size:12px}
details{background:var(--paper);border:1px solid var(--line);border-radius:11px;margin-bottom:13px;overflow:hidden}
summary{cursor:pointer;padding:16px;font-weight:650;background:#f9f8f4}
.status{display:inline-block;font-size:11px;font-weight:500;color:#425968;border:1px solid #a8b6bb;border-radius:20px;padding:2px 7px;margin-left:7px}
.comparison{padding:10px 16px 18px}.headings,.pair{display:grid;grid-template-columns:1fr 1fr;gap:16px}
.headings{font-size:12px;letter-spacing:.03em;font-weight:650;color:#556679;margin-bottom:9px}
.pair{padding:12px 0;border-top:1px solid #e8e5df}
.pair.edited{border-left:4px solid #83a4a9;padding-left:9px}
.meta{grid-column:1/-1;font-size:11px;color:#697783;font-family:ui-monospace,monospace}
.ar,.ru{white-space:pre-wrap;overflow-wrap:anywhere;font-size:15px;line-height:1.9}
.ar{font-family:"Noto Naskh Arabic","Amiri","Times New Roman",serif;font-size:18px;direction:rtl;text-align:right}
.ru{font-family:Georgia,"Times New Roman",serif}
.note{font-size:13px;color:#6b7380;font-style:italic}
@media(max-width:680px){.headings,.pair{grid-template-columns:1fr}.headings span:nth-child(2){display:none}.ar{border-bottom:1px dashed var(--line);padding-bottom:12px}}
@media print{details{break-inside:avoid}details:not([open]){display:none}header{padding:12px}}
</style>""",
        '</head><body>',
        '<header><h1>Степени идущих · Том 1</h1>',
        '<p>Арабский оригинал и русский литературный перевод · сравнительный корпус</p>',
        '<small>Структура по навигационным разделам приложения, не по авторским главам.</small></header>',
        '<main class="wrap"><div class="summary">',
        '<strong>Редакционная версия.</strong> ' +
        f'Вычитано разделов: {len(reviewed)}; редакторских партий: {len(manifests)}. '
        'далее приведён рабочий перевод без новой вычитки. Аяты и хадисы специально не переписывались. '
        'Независимая сверка с печатным арабским изданием и богословская рецензия не завершены.',
        '</div>',
        '<nav aria-label="Переход к разделу">'
    ]
    for i, ch in enumerate(book["chapters"], 1):
        chunks.append(f'<a href="#part-{i}">{i:02d}</a>')
    chunks.append('</nav>')
    total = 0
    for i, ch in enumerate(book["chapters"], 1):
        total += len(ch["paragraphs"])
        in_review = i in reviewed
        chunks.append(f'<details id="part-{i}" {"open" if i <= 2 else ""}>')
        chunks.append(f'<summary>{i:02d}. {esc(ch["title"])} '
                      f'<span class="status">{"Литературно просмотрено" if in_review else "Черновой перевод"}</span></summary>')
        chunks.append('<div class="comparison"><div class="headings"><span>Арабский оригинал</span>'
                      '<span>Русский перевод</span></div>')
        for p in ch["paragraphs"]:
            pid = p["id"]
            is_edited = pid in edited
            chunks.append(f'<article class="pair{" edited" if is_edited else ""}">'
                          f'<div class="meta">{esc(pid)}'
                          f'{" · Исправлено в литературной редакции" if is_edited else ""}'
                          f'{" · Примечание редактора" if p["role"]=="editor_note" else ""}</div>')
            chunks.append(f'<div class="ar" lang="ar" dir="rtl">{esc(p["ar"]) if p["ar"].strip() else "<span class=note>Нет отдельного арабского текста: редакторское примечание</span>"}</div>')
            chunks.append(f'<div class="ru" lang="ru" dir="ltr">{esc(p["ru"])}</div></article>')
        chunks.append('</div></details>')
    chunks.append('<p class="note">Все 90 навигационных разделов и '
                  f'{total} текстовых блоков первого тома сохранены.</p></main></body></html>')
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(chunks), encoding="utf-8")
    return output


if __name__ == "__main__":
    book = json.loads(OUTPUT.read_text(encoding="utf-8"))
    folder = ROOT / "research/editorial/volume1"
    manifests = [json.loads(p.read_text(encoding="utf-8"))
                 for p in sorted(folder.glob("batch[0-9][0-9]_pages*.json"))]
    result = render(book, manifests=manifests)
    print(f"PASS: comparison HTML contains {len(book['chapters'])} sections: {result}")
