from pathlib import Path
import json,hashlib,subprocess,sys,re
R=Path('/workspace/Madarij-Native');D=R/'research/volume4';REC=Path('/workspace/recovery');BASE='3d1e443b2dba9339255bbe875d1600301c609022';sha=lambda s:hashlib.sha256(s.encode()).hexdigest()
def base(n):return json.loads(subprocess.check_output(['git','-C',str(R),'show',BASE+':research/volume4/'+n]))
def choose(name,first,last):
 good=[]
 for f in REC.rglob(name):
  try:
   x=json.loads(f.read_text());pages=x.get('pages',[]);units=x.get('units',[])
   if [r['printed_page'] for r in pages]!=list(range(first,last+1)):continue
   if x.get('completed') is not True and not (x.get('completed') is None and all(u.get('draft_status','').startswith('self_reviewed') for u in units) and x.get('review_notes')):continue
   good.append((f,x))
  except (ValueError,KeyError):continue
 if not good:raise ValueError('Missing finished '+name)
 hashes={hashlib.sha256(json.dumps(x,ensure_ascii=False,sort_keys=True).encode()).hexdigest() for _,x in good}
 if len(hashes)!=1:raise ValueError('Conflicting finished copies '+name)
 return good[0]
def merge_override(page):
 for f in REC.rglob(f'merge{page}-{page+1}.json'):
  x=json.loads(f.read_text());x=x.get('unit',x)
  if 'source_ids' in x and 'ar' in x and 'ru' in x:return x
 raise ValueError('Missing merge '+str(page))
rows=[json.loads(l) for l in subprocess.check_output(['git','-C',str(R),'show',BASE+':research/volume4/source-pages.jsonl']).decode().splitlines()];t=base('translation.json');initial_units=len(t['paragraphs']);new=[];blocks=[]
f,b=choose('batch303-308.json',303,308);assert rows[-1]['printed_page']==303;assert rows[-1]['paragraphs'][:2]==b['pages'][0]['paragraphs'][:2];rows=rows[:-1]+b['pages'];new.extend(b['units']);blocks.append({'file':str(f),'pages':[303,308],'notes':b.get('review_notes',[])})
for a in range(309,557,10):
 z=min(a+9,556);f,b=choose(f'batch{a}-{z}.json',a,z);rows.extend(b['pages']);new.extend(b['units']);blocks.append({'file':str(f),'pages':[a,z],'review_notes':b.get('review_notes',[]),'boundary_notes':b.get('boundary_notes',[])})
for page in [318,348,368,388,398,428,458,468,488,508,528]:
 right=f'v4-p{page+1:03d}-01';i=next(i for i,u in enumerate(new[:-1]) if u['printed_pages'][-1]==page and new[i+1]['source_ids'][0]==right);l,r=new[i:i+2];ru=l['ru'].rstrip()+' '+r['ru'].lstrip()
 if page==318:
  assert l['ru'].endswith('поэтому');right_ru=r['ru'].replace('Его имена «Аллах», «Господь» и «Бог» именами','имена «Аллах», «Господь» и «Бог» не являются именами',1);assert right_ru!=r['ru'];ru=l['ru']+' '+right_ru
 elif page==348:
  assert r['ru'].startswith('к ним, пока');ru=l['ru']+r['ru'].replace('к ним, пока',', пока',1)
 elif page==398:
  assert r['ru'].startswith('на ковёр царя;');ru=l['ru']+' '+r['ru'].replace('на ковёр царя;','ковру царя;',1)
 elif page in [458,508,528]:
  o=merge_override(page);assert o['ar']==l['ar']+' '+r['ar'];assert o['source_ids']==l['source_ids']+r['source_ids'];ru=o['ru']
 l.update(source_ids=l['source_ids']+r['source_ids'],printed_pages=sorted(set(l['printed_pages']+r['printed_pages'])),ar=l['ar']+' '+r['ar'],ru=ru);new.pop(i+1)
source={q['id']:q for r in rows for q in r['paragraphs']};corrections=[]
for sid,replacements in [('v4-p331-04',[('{فَقَالَ','{قَالَ'),('{فقال','{قال')]),('v4-p404-06',[('مطاوعة','مطالعة')]),('v4-p510-01',[('يَاحَسْرَتَا','يَاحَسْرَتَى'),(' وَكُنْتَ مِنَ الْكَافِرِينَ','')])]:
 q=source[sid];old=q['ar']
 for a,b in replacements:q['ar']=q['ar'].replace(a,b)
 q['sha256']=sha(q['ar'])
 if old!=q['ar']:corrections.append({'id':sid,'before':old,'after':q['ar'],'evidence':'Reapplied exact parent correction documented in historical tool inputs, not a new translation.'})
for u in new:
 if 'v4-p331-04' in u['source_ids']:u['ru']=u['ru'].replace('{فَقَالَ','{قَالَ').replace('{فقال','{قال')
 if 'v4-p404-06' in u['source_ids']:u['ru']=u['ru'].replace('покорное согласие','постижение')
 if 'v4-p468-03' in u['source_ids']:u['ru']=u['ru'].replace('несправедливостью и справедливостью','несправедливостью и притеснением')
 if 'v4-p510-01' in u['source_ids']:u['ru']=u['ru'].replace('يَاحَسْرَتَا','يَاحَسْرَتَى').replace(' وَكُنْتَ مِنَ الْكَافِرِينَ','').replace('возгордилась и была из неверующих','возгордилась')
 u['ar']=' '.join(source[s]['ar'] for s in u['source_ids'])
 assert re.findall(r'\{([^{}]+)\}',u['ar'])==re.findall(r'\{([^{}]+)\}',u['ru']),u['id']
 if 'v4-p355-04' in u['source_ids'] or 'وعدم الغفلات' in u['ar'] or u['id'] in ['v4-p551-04-ru','v4-p554-02-ru']:u['draft_status']='self_reviewed_context_question_open'
 else:u['draft_status']='self_reviewed'
t['paragraphs'].extend(new);assert len(t['paragraphs'])==2137,(len(t['paragraphs']),2137);assert len(source)==2487,len(source);assert [r['printed_page'] for r in rows]==list(range(3,557));assert len(new)==902
t.update(status='complete_author_body_draft_recovered_from_historical_tool_inputs',author_body_translation_complete=True,publication_ready=False,translation_independently_verified=False)
rev=base('self-review.json')
for u in new:rev['reviews'].append({'id':u['id'],'source_ids':u['source_ids'],'ar_sha256':sha(u['ar']),'ru_sha256':sha(u['ru']),'semantic_comparison':'historically_performed_by_Codex; see recovered batch notes','russian_only_reading':'historically_performed_by_Codex; no new review asserted','final_back_comparison':'historically_performed_by_Codex; no new review asserted','independent_human_review':False,'context_question_open':u['draft_status']!='self_reviewed','review_date':'2026-10-30','recovery_provenance':'historical_tool_inputs_replay; old Git object unavailable'})
if '--apply' not in sys.argv:
 print('READY',len(rows),'pages',len(source),'fragments',len(t['paragraphs']),'units');sys.exit()
def save(n,x):(D/n).write_text(json.dumps(x,ensure_ascii=False,indent=2)+'\n')
(D/'source-pages.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in rows));save('translation.json',t);save('self-review.json',rev);c=base('source-corrections.json');c['corrections'].extend(corrections);save('source-corrections.json',c)
old=subprocess.check_output(['git','-C',str(R),'show',BASE+':research/volume4/opening.ru.md']).decode().rstrip()+'\n\n';old+=''.join('<!-- '+u['id']+'; страницы '+', '.join(map(str,u['printed_pages']))+'; '+u['draft_status']+' -->\n'+u['ru']+'\n\n' for u in new);(D/'opening.ru.md').write_text(old.rstrip()+'\n')
p=base('progress.json');p.update(source_pages_captured=[3,556],draft_complete_printed_range=[3,556],draft_complete_pages=554,partial_printed_page=None,partial_translated_through_source_id=None,next_source_id=None,continuation_pdf_leaf_required=None,next_fragment_opening=None,next_checkpoint_range=None,author_body_translation_complete=True,entire_volume_complete=False,app_integrated=False,last_full_page_with_three_Codex_passes=556,unsent_current_content='Recovered complete historical draft; GitHub save in progress. Original local Git commits unavailable.');p.pop('source_capture_partial_last_page',None);p.pop('partial_last_source_fragment_id',None);p['pages_with_three_Codex_passes']=list(range(3,557));save('progress.json',p)
save('RECOVERY_PROVENANCE.json',{'method':'historical_tool_inputs_replay','original_local_commit_unavailable':'38a3437','remote_baseline':BASE,'no_new_translation':True,'original_git_sha_not_recovered':True,'blocks':blocks,'historical_expected_counts':{'author_pages':554,'source_fragments':2487,'translation_units':2137},'independent_human_review':False,'new_semantic_review_asserted':False,'app_changed':False,'source_protected_volumes_unchanged':True})
print('APPLIED',len(rows),'pages',len(source),'fragments',len(t['paragraphs']),'units')
