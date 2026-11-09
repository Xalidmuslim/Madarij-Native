# Exact468 corrective lines selected from historical parent payload. Unrelated
#461 metadata/checkpoint operations are excluded; path targets current CWD.
from pathlib import Path
import json,hashlib
P=Path('research/volume4');sha=lambda s:hashlib.sha256(s.encode()).hexdigest()
def rd(n):return json.loads((P/n).read_text())
def wr(n,x):(P/n).write_text(json.dumps(x,ensure_ascii=False,indent=2)+'\n')
t=rd('translation.json');revs=rd('self-review.json');u=next(u for u in t['paragraphs'] if 'v4-p468-03' in u['source_ids']);old=u['ru'];assert 'несправедливостью и справедливостью' in old;u['ru']=old.replace('несправедливостью и справедливостью','несправедливостью и притеснением');v=next(v for v in revs['reviews'] if v['id']==u['id']);v.update(ru_sha256=sha(u['ru']));v['notes']+=' Enlarged control468 inspected by parent and independent reviewer: قَسْط with fatha means injustice/oppression, distinct from قِسْط justice. Corrected Russian lexical error; AR unchanged, no conjectural emendation.'
l=rd('literary-revisions.json');l['revisions'].append({'id':u['id'],'before':old,'after':u['ru'],'reason':'Actual printed vowel قَسْط differs from قِسْط: corrected injustice/oppression, retained AR. Parent and reviewer inspected issue468-detail.png.'});wr('literary-revisions.json',l)
f=P/'opening.ru.md';f.write_text(f.read_text().replace(old,u['ru']))
wr('translation.json',t);wr('self-review.json',revs)
