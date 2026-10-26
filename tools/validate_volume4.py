#!/usr/bin/env python3
"""Validate volume4 coverage and protected data; never certifies literary quality."""
import argparse
import hashlib
import json
from pathlib import Path
import re
from collections import Counter
ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'research/volume4'

def sha(text):
    return hashlib.sha256(text.encode()).hexdigest()

def validate(require_complete=False):
    errors = []
    protection = json.loads((DATA / 'protected-volumes.json').read_text())
    for name, expected in protection['sha256'].items():
        if hashlib.sha256((ROOT / name).read_bytes()).hexdigest() != expected:
            errors.append(f'Protected earlier-volume file changed: {name}')
    rows = [json.loads(l) for l in (DATA / 'source-pages.jsonl').read_text().splitlines()]
    nums = [r['printed_page'] for r in rows]
    if not nums or nums != list(range(3, max(nums) + 1)):
        errors.append('Source pages missing, repeated or unordered')
    if any(n > 556 for n in nums):
        errors.append('Non-author pages imported')
    source, positions = {}, {}
    for r in rows:
        n = r['printed_page']
        if r['pdf_leaf'] != n:
            errors.append(f'Unexpected pagination: {n}')
        if not r['paragraphs'] or r.get('publisher_footnote_text_imported') is not False:
            errors.append(f'Empty page or mixed apparatus: {n}')
        for i, q in enumerate(r['paragraphs'], 1):
            if q['id'] != f'v4-p{n:03d}-{i:02d}' or q['id'] in source:
                errors.append(f'Duplicate or wrong source ID: {q["id"]}')
            if not q['ar'].strip() or q['sha256'] != sha(q['ar']):
                errors.append(f'Empty or altered source: {q["id"]}')
            positions[q['id']] = len(source)
            source[q['id']] = q
    t = json.loads((DATA / 'translation.json').read_text())
    reviews = {r['id']: r for r in json.loads((DATA / 'self-review.json').read_text())['reviews']}
    uses, ids, last, certified = Counter(), set(), -1, set()
    for u in t['paragraphs']:
        if u['id'] in ids:
            errors.append(f'Duplicate translation ID: {u["id"]}')
        ids.add(u['id'])
        ss = u['source_ids']
        if not ss or any(s not in source for s in ss):
            errors.append(f'Unknown source ID in {u["id"]}')
            continue
        pp = [positions[s] for s in ss]
        if pp != list(range(pp[0], pp[0] + len(pp))) or pp[0] <= last:
            errors.append(f'Nonconsecutive or reordered source: {u["id"]}')
        last = pp[-1]
        uses.update(ss)
        if u['printed_pages'] != sorted({int(s[4:7]) for s in ss}):
            errors.append(f'Wrong printed page IDs: {u["id"]}')
        joined = ' '.join(source[s]['ar'] for s in ss)
        if u['ar'] != joined or not u.get('ru', '').strip():
            errors.append(f'Wrong source pairing or empty translation: {u["id"]}')
        if re.findall(r'\{([^{}]+)\}', u['ar']) != re.findall(r'\{([^{}]+)\}', u['ru']):
            errors.append(f'Arabic quotation lost or altered: {u["id"]}')
        if u['draft_status'].startswith('self_reviewed'):
            rr = reviews.get(u['id'], {})
            if rr.get('ar_sha256') != sha(u['ar']) or rr.get('ru_sha256') != sha(u['ru']):
                errors.append(f'Missing or stale review: {u["id"]}')
            elif u['draft_status'] == 'self_reviewed' and not rr.get('context_question_open', True):
                certified.update(ss)
    if any(v > 1 for v in uses.values()):
        errors.append('Source fragment translated multiple times')
    untranslated = [s for s in source if not uses[s]]
    for s in untranslated:
        if source[s]['source_complete']:
            errors.append(f'Complete source fragment skipped: {s}')
    draft_pages = [r['printed_page'] for r in rows if all(uses[q['id']] == 1 for q in r['paragraphs'])]
    reviewed_pages = [r['printed_page'] for r in rows if all(q['id'] in certified for q in r['paragraphs'])]
    if require_complete and (nums != list(range(3, 557)) or reviewed_pages != nums or untranslated):
        errors.append('Entire author text is not translated and self-reviewed')
    return {'format_integrity_ok': not errors, 'errors': errors,
            'source_pages_captured': nums, 'draft_pages_complete': draft_pages,
            'self_reviewed_pages_without_open_context_questions': reviewed_pages,
            'untranslated_captured_fragments': untranslated,
            'remaining_author_pages_not_captured': [n for n in range(3, 557) if n not in nums],
            'total_author_pages': 554, 'independent_human_review': False,
            'entire_volume_complete': nums == list(range(3, 557)) and reviewed_pages == nums and not errors,
            'app_integration': 'not_started; incomplete research text not published in reader'}

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--require-complete', action='store_true')
    args = parser.parse_args()
    report = validate(args.require_complete)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    raise SystemExit(0 if report['format_integrity_ok'] else 1)
