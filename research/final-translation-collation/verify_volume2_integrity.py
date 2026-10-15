"""Reconstruct all volume-II translation edits and mirror sync from the frozen baseline."""
import hashlib
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
base = json.loads((HERE / 'BASELINE.json').read_text())['base_commit']
def original(path):
    return json.loads(subprocess.check_output(['git', 'show', f'{base}:{path}'], cwd=ROOT, text=True))
def parent_at(obj, pointer):
    keys = pointer.strip('/').split('/')
    for key in keys[:-1]:
        obj = obj[int(key)] if isinstance(obj, list) else obj[key]
    return obj, int(keys[-1]) if isinstance(obj, list) else keys[-1]
path = 'app/src/main/assets/volume2-preview.json'
expected = original(path)
current = json.loads((ROOT / path).read_text())
units = {p['id']: p for c in expected['chapters'] for p in c['paragraphs']}
logs = [row for f in sorted(HERE.glob('V2_P*_CHANGES.json')) for row in json.loads(f.read_text())]
for row in logs:
    p = units[row['paragraph_id']]
    assert p['ru'].count(row['before']) == 1, p['id']
    assert row['arabic_anchor'] in p['ar']
    p['ru'] = p['ru'].replace(row['before'], row['after'])
assert current == expected, 'Unlogged change to second-volume reader/source/structure'
assert len(units) == 676 and not current['publication_ready'] and not current['translation_independently_verified']
syncpath = HERE / 'V2_SYNC_CHANGES.jsonl'
sync = [json.loads(line) for line in syncpath.read_text().splitlines()] if syncpath.exists() else []
mirrors = {}
for row in sync:
    obj = mirrors.setdefault(row['path'], original(row['path']))
    parent, key = parent_at(obj, row['pointer'])
    assert parent[key] == row['before'], (row['path'], row['pointer'])
    parent[key] = row['after']
for name, obj in mirrors.items():
    assert json.loads((ROOT / name).read_text()) == obj, name
print(f'PASS: v2 {len(units)} stable units; {len(logs)} logged edits; {len(sync)} local mirror updates; Arabic and metadata unchanged')
