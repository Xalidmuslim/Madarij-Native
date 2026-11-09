import json,hashlib,pathlib,re
root=pathlib.Path('/workspace/recovery/agent349')
def meta(p):
 b=p.read_bytes();return {'path':str(p),'bytes':len(b),'sha256':hashlib.sha256(b).hexdigest()}
steps=[]
for p in sorted((root/'steps').glob('*.sh')):
 x=meta(p);x['status']='executed_successfully';x['original_target_root']='/workspace/scratch/volume4';x['redirected_target_root']=str(root/'output');steps.append(x)
outputs=[]
for p in sorted((root/'output').glob('*.json')):
 d=json.loads(p.read_text());x=meta(p)
 if 'pages' in d:
  src={z['id']:z for pg in d['pages'] for z in pg['paragraphs']}
  assert all(z['sha256']==hashlib.sha256(z['ar'].encode()).hexdigest() for z in src.values())
  assert sorted(src)==sorted(i for u in d['units'] for i in u['source_ids'])
  assert all(u['ar']==' '.join(src[i]['ar'] for i in u['source_ids']) for u in d['units'])
  assert all(re.findall(r'\{[^}]*\}',u['ar'])==re.findall(r'\{[^}]*\}',u['ru']) for u in d['units'])
  x.update(pages=len(d['pages']),source_fragments=len(src),units=len(d['units']),completed=d.get('completed'),structural_checks='pass')
 else:x.update(source_ids=d['source_ids'],units=1)
 outputs.append(x)
manifest={'method':'historical_tool_inputs_replay','recovery_date':'2026-11-09','agent':'v4_349_358','provenance':'Historical create/append/finalizer tool-input bodies copied from retained conversation context, with filesystem paths redirected. No new translation. No lost-output hash baseline exists, so byte identity to lost JSON is not independently proved.','steps':steps,'outputs':outputs,'dependency':{'merge508-509_input':meta(pathlib.Path('/workspace/recovery/agent339/replayed/batch499-508.json'))},'totals':{'pages':sum(x.get('pages',0) for x in outputs),'source_fragments':sum(x.get('source_fragments',0) for x in outputs),'batch_units':sum(x['units'] for x in outputs if 'pages' in x),'merges':2},'recovery_execution_notes':['Steps001–016 were already present and successful before this resumed turn; they were inspected and not recreated.','Step017 already existed complete; resumed by executing it.','First020 attempt failed Quran equality: recovery-copy512-03 Russian embedded Arabic had لعلهم where the Arabic payload had لعلكم. Corrected that single copied word in018 to match its Arabic counterpart, then replayed017–021 successfully. This exception is disclosed rather than claimed as independently located raw historical bytes.','An initial merge021 was produced before finalizer success; it was regenerated after successful020.','All hashes/coverage/AR source joins/Quran AR-RU equality verified structurally. Historical review notes and self_reviewed statuses are retained historical statements, not new image or semantic checks.'],'parent_owned_late_corrections_not_applied':[{'location':'404','description':'Parent later confirmed مطالعة rather than historical مطاوعة; RU покорное согласие to постижение, remove false variant note. Exact parent patch not replayed here.'},{'location':'510','description':'Historical payload retains unsupported canonical ending وكنت من الكافرين and its Russian meaning; parent separately restores exact removal and ياحسرتا to printed ياحسرتى.'},{'location':'source_complete','description':'Historical false values on cross-page fragments preserved; parent later clarified source_complete means fully transcribed page fragment and owns standardization.'}],'scope':{'repo_edited':False,'push_performed':False,'new_translation':False,'new_image_verification':False},'missing_payloads':[]}
(root/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2))
print(json.dumps(manifest['totals']))
