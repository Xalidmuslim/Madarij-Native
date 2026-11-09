import hashlib,json,re,subprocess
from pathlib import Path
root=Path('/workspace/recovery/agent319');out=root/'artifacts'
ranges=[(319,328,32,25),(369,378,37,29),(419,428,34,28),(479,488,39,32),(529,538,57,49)]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
checks=[]
for start,end,ns,nu in ranges:
 p=out/f'batch{start}-{end}.json';d=json.loads(p.read_text());src={s['id']:s for pg in d['pages'] for s in pg['paragraphs']}
 assert [pg['printed_page'] for pg in d['pages']]==list(range(start,end+1))
 assert len(src)==ns and len(d['units'])==nu
 assert d['completed'] is True
 assert len(d['review_notes'])==3 and all(n['performed'] for n in d['review_notes'])
 assert list(src)==[s for u in d['units'] for s in u['source_ids']]
 for s in src.values():assert s['sha256']==hashlib.sha256(s['ar'].encode()).hexdigest()
 for u in d['units']:
  assert u['ar']==' '.join(src[i]['ar'] for i in u['source_ids'])
  assert re.findall(r'\{[^}]*\}',u['ar'])==re.findall(r'\{[^}]*\}',u['ru'])
  assert u['draft_status']=='self_reviewed' and u['independent_human_review'] is False
 checks.append({'file':str(p.relative_to(root)),'pages':[start,end],'source_fragments':ns,'translation_units':nu,'sha256':sha(p),'checks':'completed, sequential pages, counts, source hashes, source coverage, joined Arabic, complete Quran brace identity, historical review status'})
mp=out/'merge528-529.json';m=json.loads(mp.read_text());a=json.loads((root/'upstream528_snapshot.json').read_text())['unit'];b=json.loads((out/'batch529-538.json').read_text())['units'][0];u=m['unit']
assert m['completed'] is True and u['ar']==a['ar']+' '+b['ar'] and u['source_ids']==['v4-p528-04','v4-p529-01']
assert re.findall(r'\{[^}]*\}',u['ar'])==re.findall(r'\{[^}]*\}',u['ru'])
assert m['source_snapshot_sha256']=={s:hashlib.sha256(v.encode()).hexdigest() for s,v in [('v4-p528-04',a['ar']),('v4-p529-01',b['ar'])]}
checks.append({'file':str(mp.relative_to(root)),'source_ids':u['source_ids'],'sha256':sha(mp),'checks':'exact dependency joins, complete Arabic-only brace equality, dependency SHA256, completed'})
before={p.name:sha(p) for p in out.glob('*.json')}
for n in [319,369,419,479,529]:subprocess.run(['python',str(root/'scripts'/f'replay{n}.py')],check=True)
subprocess.run(['python',str(root/'scripts'/'replaymerge528.py')],check=True)
after={p.name:sha(p) for p in out.glob('*.json')}
assert before==after
manifest={
 'provenance':'replay_from_historical_tool_inputs',
 'agent':'v4_319_328',
 'recovery_root':str(root),
 'not_a_found_backup':True,
 'method':'Literal historical add(...) Arabic/Russian data and correction/review/finalize payloads were transcribed from the agent conversation context and replayed deterministically. No new translation or PDF/OCR reconstruction was used.',
 'wrapper_adaptations':['Output paths redirected from /workspace/scratch/volume4 to recovery/agent319/artifacts.','Sequential historical heredocs coalesced into one replay script per batch; identical add() definition and imports retained once; redundant JSON reloads and read-only print/image commands omitted.','Historical merge528-529 dependency528-04 reconstructed from its literal historical tool OUTPUT snapshot because the original batch519-528 file was absent in this workspace; no other part of that neighboring batch is claimed recovered.'],
 'historical_corrections_in_order':{
  '319-328':['Initial319–321 data and boundary note','322–325 data','326–328 data','Intermediate-cause term substitutions and Russian corrections + semantic/Russian review records','Restore inadvertently replaced substrings in непосредственно/посредством','Final reverse-review record;self_reviewed;completed:true'],
  '369-378':['Initial369–371 data and boundary note','372–374 data','375–376 data','377–378 data','Arabic عاذ/بينه corrections + Russian373 + semantic review record','Russian369 clause subject,376 companions/comparative,372 syntax + Russian review record','Final reverse-review record;self_reviewed;completed:true'],
  '419-428':['Initial419–421 data','422–424 data','425–428 data and boundary note','421 indication syntax,425 named prayer,419 naming + semantic review record','Russian idiom/comma/superlative;427 likeness;428 logic nouns + Russian review record','422 source وعه,review count28;final reverse-review record;self_reviewed;completed:true'],
  '479-488':['Initial479–481 data','482–484 data','485–487 data','488 data and boundary note','481 بكماله,485 واتضح,482 Russian referent + semantic review record','480/483 idioms,485 list comma + Russian review record','Final reverse-review record;self_reviewed;completed:true'],
  '529-538':['Initial529–531 data and boundary note','532–534 data including immediate negation join correction','535–537 data','538 data','Coordinator530-03 explicit ‘илال term correction','531 source فنفى,533 term + semantic review record','Russian manifestation/pronoun/list/hostility changes + Russian review record','Final reverse-review record;self_reviewed;completed:true'],
  'merge528-529':['Literal528-04 historical output snapshot +529-01','Exact Arabic single-space join and full Russian Quran meaning outside Arabic braces','Historical three review notes,dependency hashes;self_reviewed;completed:true']
 },
 'artifacts':checks,
 'scripts':[{'file':str(p.relative_to(root)),'sha256':sha(p)} for p in sorted((root/'scripts').glob('*.py'))],
 'dependency_snapshot':{'file':'upstream528_snapshot.json','sha256':sha(root/'upstream528_snapshot.json'),'provenance':'historical_tool_output_literal_snapshot'},
 'replay_determinism_verified':True,
 'limits':['Original pre-reset final-file byte hashes were not available, so current hashes establish recovered artifact identity and deterministic replay, not independent comparison against a found old backup.','Historical self_reviewed/image-inspection statements are preserved historical metadata; recovery performed structural/deterministic checks only and does not claim new image or human/theological verification.','No repository edits, commit, push or publication performed by this recovery agent.']
}
(root/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({'batches':len(ranges),'pages':50,'source_fragments':199,'translation_units':163,'merge_complete':True,'deterministic':before==after,'files':[{k:x[k] for k in ['file','sha256']} for x in checks]},ensure_ascii=False,indent=2))
