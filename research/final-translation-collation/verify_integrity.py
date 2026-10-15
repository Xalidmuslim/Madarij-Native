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
logs = [row for path in sorted(HERE.glob('V1_*CHANGES.json'), key=lambda p: ('RECHECK' in p.name, p.name))
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
    if name not in (book_path, 'app/src/main/assets/volume2-preview.json'):
        assert hashlib.sha256((ROOT / name).read_bytes()).hexdigest() == digest, name
# Test logic is frozen; only individually justified string expectations may change.
expectation_path = HERE / 'TEST_EXPECTATION_UPDATES.json'
expectation_rows = json.loads(expectation_path.read_text()) if expectation_path.exists() else []
expected_tests = {}
for row in expectation_rows:
    name = row['test_file']
    assert name.startswith('tools/test_volume2') and name.endswith('.py')
    if name not in expected_tests:
        expected_tests[name] = subprocess.check_output(['git', 'show', f'{baseline["base_commit"]}:{name}'], cwd=ROOT, text=True)
    assert expected_tests[name].count(row['old']) == 1
    expected_tests[name] = expected_tests[name].replace(row['old'], row['new'])
for name, text in expected_tests.items():
    assert (ROOT / name).read_text() == text, ('Unlogged test change', name)

paths = subprocess.check_output(['git', 'diff', baseline['base_commit'], '--name-only'], cwd=ROOT, text=True).splitlines()
assert all(p in (book_path, 'app/src/main/assets/content-manifest.json', 'app/src/main/assets/volume2-preview.json') or p.startswith('research/final-translation-collation/') or p in expected_tests or p in {x['path'] for row in json.loads((HERE / 'V2_SYNC_MAP.json').read_text())['reader_mapping'] for x in row['redundant_ru_locations']} for p in paths), paths
print(f'PASS: {len(ids)} v1 units; {len(changed_ids)} edited text units; all source text, IDs, metadata, navigation preserved; volume II Russian changes audited separately')

manifest_path = 'app/src/main/assets/content-manifest.json'
manifest = json.loads((ROOT / manifest_path).read_text())
old_manifest = json.loads(subprocess.check_output(['git', 'show', f'{baseline["base_commit"]}:{manifest_path}'], cwd=ROOT, text=True))
assert manifest['corpus_sha256'] == hashlib.sha256((ROOT / book_path).read_bytes()).hexdigest()
full_book = json.loads((ROOT / book_path).read_text())
assert manifest['author_ru_words'] == sum(len(p['ru'].split()) for c in full_book['chapters'] for p in c['paragraphs'] if p['role']=='author')
for item in (manifest, old_manifest):
    item.pop('corpus_sha256')
    item.pop('author_ru_words')
assert manifest == old_manifest, 'Unexpected manifest mutation'

freeze = json.loads((HERE / 'VOLUME1_COMPLETED_PASS.json').read_text())
assert hashlib.sha256((ROOT / book_path).read_bytes()).hexdigest() == freeze['corpus_sha256'], 'Volume I changed during volume II work'
assert hashlib.sha256((ROOT / 'app/src/main/assets/content-manifest.json').read_bytes()).hexdigest() == freeze['content_manifest_sha256']
if list(HERE.glob('V2_P*_CHANGES.json')):
    subprocess.run(['python3', str(HERE / 'verify_volume2_integrity.py')], cwd=ROOT, check=True)
else:
    assert hashlib.sha256((ROOT / 'app/src/main/assets/volume2-preview.json').read_bytes()).hexdigest() == freeze['volume2_sha256']
