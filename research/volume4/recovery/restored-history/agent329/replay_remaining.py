from pathlib import Path
import json,hashlib,re,shutil
root=Path('/workspace/recovery/agent329')
original=root/'original';work=root/'replayed-scratch'
def run(text,label):
 text=text.replace('/workspace/scratch/volume4',str(work))
 exec(compile(text,label,'exec'),{})
for n in ['329','379','429','489']:
 build=original/f'build{n}.py'
 if n=='429':
  copy=work/'build429.py'
  copy.write_text(build.read_text().replace('/workspace/scratch/volume4',str(work)))
  run((original/'patch_build429.py').read_text(),str(original/'patch_build429.py'))
  run(copy.read_text(),str(copy))
 else:run(build.read_text(),str(build))
 final=original/f'finalize{n}.py';run(final.read_text(),str(final))
expected={'329-338':(44,36),'379-388':(42,36),'429-438':(33,28),'489-498':(45,37),'539-548':(49,44)}
entries=[]
for key,(fragments,units) in expected.items():
 f=work/f'batch{key}.json';d=json.loads(f.read_text());a,b=map(int,key.split('-'))
 assert [p['printed_page'] for p in d['pages']]==list(range(a,b+1))
 by={q['id']:q for p in d['pages'] for q in p['paragraphs']}
 assert len(by)==fragments and len(d['units'])==units
 for q in by.values():assert q['sha256']==hashlib.sha256(q['ar'].encode()).hexdigest(),q['id']
 ids=[s for u in d['units'] for s in u['source_ids']]
 assert len(ids)==len(set(ids))==len(by)
 for u in d['units']:
  assert u['ar']==' '.join(by[s]['ar'] for s in u['source_ids']),u['id']
  assert re.findall(r'\{[^}]*\}',u['ar'])==re.findall(r'\{[^}]*\}',u['ru']),u['id']
  assert u['draft_status']=='self_reviewed'
 if key!='329-338':assert d['completed'] is True
 dest=root/f.name
 if key=='539-548':assert dest.read_bytes()==f.read_bytes()
 else:shutil.copyfile(f,dest)
 entries.append({'file':dest.name,'sha256_now':hashlib.sha256(dest.read_bytes()).hexdigest(),'pages':10,'source_fragments':fragments,'translation_units':units,'completed':d.get('completed'),'completed_field_historically_present':'completed' in d,'historical_review_note_count':len(d['review_notes']),'validated':'full page range; self_reviewed; SHA-256 Arabic; exact unit/source assembly; unique complete source coverage; Quran ar/ru equality'})
manifest={'provenance':'historical_tool_inputs_replay','not_backup':True,'not_recovered_commit':True,'prior_commit_sha_verified':False,'historical_final_file_hash_available':False,'archive_form':'Python heredoc create/append bodies concatenated in historical order; subsequent data mutations and review_notes replayed in historical order. Shell wrappers, image-view calls and stdout-only loops are not executable archive content. No new translation or semantic reconstruction.','execution_path_substitution':{'from':'/workspace/scratch/volume4','to':str(work)},'historical_review_notes_preserved':True,'new_visual_or_translation_review_performed':False,'completion_metadata_added':False,'batches':entries,'historical_external_corrections_not_replayed':[{'source_id':'v4-p331-04','reason':'Parent historical message specified {فقال→{قال and parent-side audit/RU correction; exact parent tool payload is outside this agent history. Parent confirmed it is available at recovery/roottools/scripts/06_source331.original.py and will apply it separately.'}],'remaining_agent_payloads':[],'input_archives':[{'file':str(f.relative_to(root)),'sha256':hashlib.sha256(f.read_bytes()).hexdigest()} for f in sorted(original.glob('*.py'))]}
(root/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
print('PASS: five batches, 50 pages, 213 source fragments, 181 translation units; historical completed absent in 329 preserved.')
