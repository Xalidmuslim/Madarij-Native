from pathlib import Path
import json,hashlib,re,shutil
root=Path('/workspace/recovery/agent329')
for n in ['539']:
 for step in ['build','finalize']:
  original=root/'original'/f'{step}{n}.py'
  source=original.read_text()
  execution=source.replace('/workspace/scratch/volume4',str(root/'replayed-scratch'))
  exec(compile(execution,str(original), 'exec'),{})
for f in (root/'replayed-scratch').glob('batch*.json'):
 d=json.loads(f.read_text());by={q['id']:q for p in d['pages'] for q in p['paragraphs']}
 for q in by.values():assert q['sha256']==hashlib.sha256(q['ar'].encode()).hexdigest()
 for u in d['units']:
  assert u['ar']==' '.join(by[s]['ar'] for s in u['source_ids'])
  assert re.findall(r'\{[^}]*\}',u['ar'])==re.findall(r'\{[^}]*\}',u['ru'])
 ids=[s for u in d['units'] for s in u['source_ids']];assert len(ids)==len(set(ids))==len(by)
 shutil.copyfile(f,root/f.name)
manifest={'provenance':'historical_tool_inputs_replay','not_backup':True,'not_recovered_commit':True,'prior_commit_sha_verified':False,'historical_final_file_hash_available':False,'execution_path_substitution':{'from':'/workspace/scratch/volume4','to':str(root/'replayed-scratch')},'historical_review_notes_preserved':True,'new_visual_or_translation_review_performed':False,'batches':[],'remaining_historical_payloads_visible_in_context':['329-338','379-388','429-438','489-498']}
for f in root.glob('batch*.json'):
 d=json.loads(f.read_text());manifest['batches'].append({'file':f.name,'sha256_now':hashlib.sha256(f.read_bytes()).hexdigest(),'pages':len(d['pages']),'fragments':sum(len(p['paragraphs']) for p in d['pages']),'units':len(d['units']),'completed':d.get('completed'),'validation':'sha256_ar, source_ids exact assembly and coverage, Quran ar/ru equality'})
(root/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(manifest,ensure_ascii=False,indent=2))
