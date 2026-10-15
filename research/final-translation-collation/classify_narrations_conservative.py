import json,re,hashlib,collections
from pathlib import Path
repo=Path(__file__).resolve().parents[2]
sources={1:repo/'app/src/main/assets/corpus.json',2:repo/'app/src/main/assets/volume2-preview.json'}
paras={}
for v,path in sources.items():
 for c in json.loads(path.read_text())['chapters']:
  for p in c['paragraphs']:paras[(v,p['id'])]=p
candidate_path=repo/'research/final-translation-collation/REPORTED_SPEECH_CANDIDATES.jsonl'
candidates=[json.loads(s) for s in candidate_path.read_text().splitlines()]
def norm(s):return re.sub('[\u064b-\u065f\u0670\u0640]','',s)
rules=[
 ('technical_term','quoted_source_title',r'(?:صحيح|مسند|سنن|الموطأ|الترمذي|البخاري|مسلم|المنازل|الرعاية)'),
]
def suggest(quote,pre):
 q=norm(quote);n=norm(pre[-280:])
 # Classification remains explicitly a machine suggestion. Short names/titles first.
 if len(q)<90 and re.match(r'^(?:صحيح|مسند|سنن|الموطأ|الترمذي|البخاري|مسلم|المنازل|رعاية|الرعاية)',q):return 'technical_term','quoted_source_title','quoted title shape',q
 if len(q)<65 and re.search(r'(?:اسمه|اسم|لفظ|لفظة|كلمة|الضمير|معنى|قوله تعالى)\s*$',n):return 'technical_term','quoted_term_or_divine_name','explicit lexical introduction',n[-100:]
 if re.search(r'(?:إسرائيلي|اسرائيلي|بني إسرائيل|بني اسرائيل)',n):return 'israiliyat',None,'explicit Israiliyat context',n[-180:]
 if re.search(r'(?:أثر إلهي|اثر الهي|عن ربه|يقول الله|قال الله|أوحى الله|اوحي الله)',n):return 'unresolved','divine_report_or_qudsi_candidate','divine-report wording does not establish prophetic source',n[-180:]
 if re.search(r'(?:صاحب.{0,20}المنازل|قال الهروي)',n):return 'Harawi',None,'explicit Manazil/Harawi introduction',n[-180:]
 if re.search(r'(?:قال رسول الله|قال النبي|عنه.{0,45}صلى الله عليه وسلم|مرفوعا|النبي.{0,50}(?:قال|أنه قال|انه قال))',n):return 'prophetic',None,'explicit prophetic attribution in local context',n[-180:]
 if re.search(r'قال (?:بعض السلف|بعض العارفين|بعض الحكماء|الجنيد|ذو النون|السري|الفضيل|سهل بن عبد الله|يحيى بن معاذ|إبراهيم الخواص|ابن تيمية|شيخنا)',n):return 'salaf',None,'explicit named/generic predecessor introduction',n[-180:]
 if re.search(r'قال (?:ابن عباس|ابن مسعود|أبو بكر|ابو بكر|عمر بن الخطاب|علي بن أبي طالب|علي بن ابي طالب|معاذ بن جبل|أبو هريرة|ابو هريرة|عبادة بن الصامت)',n):return 'companion',None,'explicit listed companion introduction; identity still unverified',n[-180:]
 return 'unresolved',None,'insufficient unambiguous local attribution',n[-180:]
rows=[]
for c in candidates:
 p=paras.get((c['volume'],c['paragraph_id']));ar=p['ar'] if p else '';start,end=c['arabic_offsets'];span=ar[start:end];valid=c['arabic_quote'] in span
 pre=ar[max(0,start-400):start];post=ar[end:end+220];kind,sub,rule,evidence=suggest(c['arabic_quote'],pre)
 rows.append(dict(id=c['id'],volume=c['volume'],paragraph_id=c['paragraph_id'],printed_pages=c['printed_pages'],detection_origin='existing_reported_speech_candidates',arabic_quote=c['arabic_quote'],arabic_offsets=c['arabic_offsets'],offset_scope='full delimited span, Python Unicode code-point offsets',exact_span=span,exact_preceding_context=pre,exact_following_context=post,offset_validated=valid,suggested_type=kind,suggested_subtype=sub,suggestion_rule=rule,suggestion_evidence=evidence,verified_type=None,verification_status='automatic_suggestion_only_not_source_verified',hadith_grade=None,source_attribution_verified=False,arabic_paragraph_sha256=hashlib.sha256(ar.encode()).hexdigest()))
# Supplement existing inventory with currently present straight/guillemet/curly quotes.
existing_spans={(r['volume'],r['paragraph_id'],*r['arabic_offsets']) for r in rows}
fresh_count=0
for (v,pid),p in paras.items():
 if p.get('role','author')!='author':continue
 ar=p.get('ar','');pages=p.get('source_pages')
 if not pages:
  pm=re.search(r'стр\.\s*(\d+)(?:[–—-](\d+))?',p.get('source',''));pages=list(range(int(pm[1]),int(pm[2] or pm[1])+1)) if pm else []
 for i,m in enumerate(re.finditer(r'«[^»]+»|“[^”]+”|"[^"\n]+"',ar),1):
  start,end=m.span()
  if (v,pid,start,end) in existing_spans:continue
  quote=m[0][1:-1];pre=ar[max(0,start-400):start];post=ar[end:end+220];kind,sub,rule,evidence=suggest(quote,pre);fresh_count+=1
  rows.append(dict(id=f'v{v}-{pid}-freshquote{i}',volume=v,paragraph_id=pid,printed_pages=pages,detection_origin='fresh_current_delimited_quote',arabic_quote=quote,arabic_offsets=[start,end],offset_scope='full delimited span, Python Unicode code-point offsets',exact_span=m[0],exact_preceding_context=pre,exact_following_context=post,offset_validated=True,suggested_type=kind,suggested_subtype=sub,suggestion_rule=rule,suggestion_evidence=evidence,verified_type=None,verification_status='automatic_suggestion_only_not_source_verified',hadith_grade=None,source_attribution_verified=False,arabic_paragraph_sha256=hashlib.sha256(ar.encode()).hexdigest()))
# Corpus curly-brace passages are a separate detection channel. Their completeness/reference accuracy is unverified.
for (v,pid),p in paras.items():
 if p.get('role','author')!='author':continue
 ar=p.get('ar','');pages=p.get('source_pages')
 if not pages:
  m=re.search(r'стр\.\s*(\d+)(?:[–—-](\d+))?',p.get('source',''));pages=list(range(int(m[1]),int(m[2] or m[1])+1)) if m else []
 for i,m in enumerate(re.finditer(r'\{([^{}]*)\}',ar),1):
  start,end=m.span();post=ar[end:end+220]
  rows.append(dict(id=f'v{v}-{pid}-quran-brace{i}',volume=v,paragraph_id=pid,printed_pages=pages,detection_origin='curly_brace_quran_convention',arabic_quote=m[1],arabic_offsets=[start,end],offset_scope='full delimited span, Python Unicode code-point offsets',exact_span=m[0],exact_preceding_context=ar[max(0,start-400):start],exact_following_context=post,offset_validated=True,suggested_type='Quran',suggested_subtype=None,suggestion_rule='corpus curly-brace convention; not independent text verification',suggestion_evidence=m[0],nearby_reference_candidates=re.findall(r'\[[^\[\]]{1,100}\]',post),verified_type=None,verification_status='automatic_suggestion_only_not_source_verified',hadith_grade=None,source_attribution_verified=False,arabic_paragraph_sha256=hashlib.sha256(ar.encode()).hexdigest()))
# Keep literal source-title/attribution markers, never primary-source verification.
source_pattern=re.compile(r'\b(?:صحيح(?: البخاري| مسلم| ابن حبان| ابن خزيمة)?|الصحيحين|الصحيح|مسند(?: الإمام)?(?: أحمد| أحمد بن حنبل)?|سنن(?: أبي داود| الترمذي| النسائي| ابن ماجه)?|الترمذي|البخاري|مسلم|الموطأ|أثر إلهي|اثر الهي|أثر إسرائيلي|اثر اسرائيلي|صاحب [«\"]?المنازل)\b')
for row in rows:
 ar=paras[(row['volume'],row['paragraph_id'])]['ar'];start,end=row['arabic_offsets'];lo=max(0,start-400);hi=min(len(ar),end+220);raw=ar[lo:hi]
 normalized=[];mapping=[]
 for offset,ch in enumerate(raw):
  if re.match('[\u064b-\u065f\u0670\u0640]',ch):continue
  normalized.append(ch);mapping.append(offset)
 normalized=''.join(normalized);mentions=[]
 for sm in source_pattern.finditer(normalized):
  sa=lo+mapping[sm.start()];sb=lo+mapping[sm.end()-1]+1
  mentions.append(dict(exact_text=ar[sa:sb],arabic_offsets=[sa,sb],role='literal_nearby_source_or_attribution_marker',link_to_quote='unverified_nearby_context_only',primary_source_verified=False))
 row['source_mentions']=mentions
 row['source_field_status']='literal_corpus_mentions_only_not_primary_source_verified'
out=Path(__file__).with_name('CITATION_REGISTRY.jsonl');out.write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in rows))
summary=dict(total=len(rows),existing_candidates=len(candidates),fresh_delimited_quotes=fresh_count,quran_braces=sum(r['detection_origin']=='curly_brace_quran_convention' for r in rows),suggested_counts=dict(collections.Counter(r['suggested_type'] for r in rows)),invalid_offsets=[r['id'] for r in rows if not r['offset_validated']],input_candidates_sha256=hashlib.sha256(candidate_path.read_bytes()).hexdigest(),source_sha256={str(v):hashlib.sha256(p.read_bytes()).hexdigest() for v,p in sources.items()})
Path(__file__).with_name('CITATION_REGISTRY_SUMMARY.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2));print(json.dumps(summary,ensure_ascii=False,indent=2))
