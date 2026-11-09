# Exact corrective lines selected from historical fix-wave2.py; filesystem path
# adapted to target CWD. The unrelated396/progress checkpoint is not included.
from pathlib import Path
import json,hashlib
P=Path('research/volume4');sha=lambda s:hashlib.sha256(s.encode()).hexdigest()
def read(n):return json.loads((P/n).read_text())
def save(n,v):(P/n).write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n')
s=[json.loads(l) for l in (P/'source-pages.jsonl').read_text().splitlines()];q=next(q for r in s for q in r['paragraphs'] if q['id']=='v4-p404-06');old=q['ar'];q['ar']=old.replace('مطاوعة','مطالعة');assert old!=q['ar'];q['sha256']=sha(q['ar']);(P/'source-pages.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in s))
t=read('translation.json');u=next(u for u in t['paragraphs'] if 'v4-p404-06' in u['source_ids']);oldru=u['ru'];u['ar']=u['ar'].replace('مطاوعة','مطالعة');u['ru']=u['ru'].replace('покорное согласие','постижение');revs=read('self-review.json');r=next(r for r in revs['reviews'] if r['id']==u['id']);r.update(ar_sha256=sha(u['ar']),ru_sha256=sha(u['ru']));r['notes']+=' Parent and independent checker viewed enlarged control404: مطالعة not مطاوعة. Russian corrected to постижение and compared with explanation407.'
c=read('source-corrections.json');c['corrections'].append({'id':'v4-p404-06','before':old,'after':q['ar'],'evidence':'Actual control404 enlarged issue404-detail.png visually inspected; مطالعة with lam, corroborated in both OpenITI witnesses and author explanation407. No conjecture.'});save('source-corrections.json',c)
save('translation.json',t);save('self-review.json',revs)
f=P/'opening.ru.md';f.write_text(f.read_text().replace(oldru,u['ru']))
