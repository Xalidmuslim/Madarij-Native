"""Check collation scope and data integrity, without building an APK."""
import hashlib
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
baseline = json.loads((HERE / 'BASELINE.json').read_text())
book_path = 'app/src/main/assets/corpus.json'
current = json.loads((ROOT / book_path).read_text())
original = json.loads(subprocess.check_output(
    ['git', 'show', f'{baseline["base_commit"]}:{book_path}'], cwd=ROOT, text=True))
logs = [row for path in HERE.glob('V1_P*_CHANGES.json')
        for row in json.loads(path.read_text())]
changed_ids = {row['paragraph_id'] for row in logs}
original_units = {p['id']: p for c in original['chapters'] for p in c['paragraphs']}
current_units = {p['id']: p for c in current['chapters'] for p in c['paragraphs']}
expected_ru = {pid: p['ru'] for pid, p in original_units.items()}
for row in logs:
    pid = row['paragraph_id']
    assert expected_ru[pid].count(row['before']) == 1, (pid, 'ambiguous change record')
    expected_ru[pid] = expected_ru[pid].replace(row['before'], row['after'])
assert all(p['ru'] == expected_ru[pid] for pid, p in current_units.items()), 'Unlogged Russian change'
assert not current['edition']['publication_ready']
assert not current['edition']['translation_independently_verified']
assert current['expected_chapter_ids'] == [c['id'] for c in current['chapters']]
ids = []
for chapter in current['chapters']:
    assert chapter['expected_paragraph_ids'] == [p['id'] for p in chapter['paragraphs']]
    for paragraph in chapter['paragraphs']:
        ids.append(paragraph['id'])
        if paragraph['role'] == 'author':
            assert paragraph['ar'].strip() and paragraph['ru'].strip()
        if paragraph['id'] in changed_ids:
            if 'translation_sha256' in paragraph:
                assert hashlib.sha256(paragraph['ru'].encode()).hexdigest() == paragraph['translation_sha256']
assert len(ids) == len(set(ids))
# Removing the only permitted changed fields must reproduce the complete baseline.
for book in (original, current):
    for chapter in book['chapters']:
        for paragraph in chapter['paragraphs']:
            if paragraph['id'] in changed_ids:
                paragraph.pop('ru')
                paragraph.pop('translation_sha256', None)
assert original == current, 'Nontranslation data changed'
for name, digest in baseline['files'].items():
    if name != book_path:
        assert hashlib.sha256((ROOT / name).read_bytes()).hexdigest() == digest, name
paths = subprocess.check_output(['git', 'diff', baseline['base_commit'], '--name-only'], cwd=ROOT, text=True).splitlines()
assert all(p == book_path or p.startswith('research/final-translation-collation/') for p in paths), paths
print(f'PASS: {len(ids)} v1 units; {len(changed_ids)} edited text units; all source text, IDs, metadata, navigation and volume II preserved')
