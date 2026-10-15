#!/usr/bin/env python3
"""Machine-only rare-anchor collation triage. No rewriting, downloads or global sequence alignment."""
import argparse,bisect,collections,hashlib,json,re,statistics,unicodedata
from pathlib import Path

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def toks(text):
 text=re.sub(r'\([^)]*\)|\[[^]]*\]',' ',text)
 text=unicodedata.normalize('NFKD',text)
 text=''.join(c for c in text if not unicodedata.combining(c)).replace('ـ','').replace('ى','ي')
 return re.findall(r'[ء-ي]+',text)

def witness(path):
 raw=path.read_text(encoding='utf8');body=raw.split('#META#Header#End#',1)[-1]
 # Preserve *both* marker brackets, rather than assume the editions place markers identically.
 parts=re.split(r'(PageV\d+P\d+)',body);tokens=[];markers=[];marker_pos=[]
 for i,part in enumerate(parts):
  if i%2:markers.append(part);marker_pos.append(len(tokens))
  else:
   part=re.sub(r'^###.*$',' ',part,flags=re.M)
   part=re.sub(r'@[A-Za-z]+@',' ',part)
   tokens.extend(toks(part))
 index={};counts=collections.Counter()
 for i in range(len(tokens)-6):
  key=tuple(tokens[i:i+7]);counts[key]+=1
  if counts[key]<=3:index.setdefault(key,[]).append(i)
  elif counts[key]==4:index.pop(key,None)
 return dict(tokens=tokens,index=index,counts=counts,markers=markers,marker_pos=marker_pos,path=str(path),sha256=sha(path))

def loc(w,pos):
 i=bisect.bisect_right(w['marker_pos'],pos)
 return {'marker_before':w['markers'][i-1] if i else None,'marker_after':w['markers'][i] if i<len(w['markers']) else None,'token_index':pos}

def union_intervals(intervals):
 out=[]
 for a,b in sorted(intervals):
  if out and a<=out[-1][1]:out[-1][1]=max(b,out[-1][1])
  else:out.append([a,b])
 return out

def lis_pairs(pairs):
 # Reverse target positions at identical query starts so a chain cannot reuse that start.
 pairs=sorted(set(pairs),key=lambda p:(p[0],-p[1]));tails=[];tailids=[];parents=[]
 for i,(q,t) in enumerate(pairs):
  j=bisect.bisect_left(tails,t);parents.append(tailids[j-1] if j else -1)
  if j==len(tails):tails.append(t);tailids.append(i)
  else:tails[j]=t;tailids[j]=i
 if not tailids:return []
 out=[];i=tailids[-1]
 while i>=0:out.append(pairs[i]);i=parents[i]
 return out[::-1]

def align(q,w):
 n=len(q);ts=w['tokens'];N=len(ts)
 if n<7:
  occurrences=[i for i in range(N-n+1) if ts[i:i+n]==q] if n else []
  unique=len(occurrences)==1
  return {'status':'short_exact_unique' if unique else 'short_exact_ambiguous' if occurrences else 'short_missing','query_tokens':n,'rare_7gram_hits':0,'anchor_coverage':None,'extended_exact_token_coverage':1.0 if unique else None,'candidate_count':len(occurrences),'short_candidates':[loc(w,i) for i in occurrences[:12]],'flags':['SHORT_UNIT_NO_7GRAM_PROOF']+(['AMBIGUOUS_SHORT_TEXT'] if len(occurrences)>1 else []),'anchors':[]}
 pairs=[];available=0
 for i in range(n-6):
  key=tuple(q[i:i+7]);positions=w['index'].get(key,[])
  if positions:available+=1;pairs.extend((i,j) for j in positions)
 if not pairs:return {'status':'no_rare_anchor','query_tokens':n,'rare_7gram_hits':0,'anchor_coverage':0.0,'extended_exact_token_coverage':0.0,'flags':['NO_RARE_7GRAM_ANCHOR'],'anchors':[]}
 bins=collections.Counter((j-i)//96 for i,j in pairs);candidates=[]
 for center,_ in bins.most_common(6):
  selected=[(i,j) for i,j in pairs if abs((j-i)//96-center)<=1]
  chain=lis_pairs(selected)
  intervals=union_intervals((i,i+7) for i,j in chain);covered=sum(b-a for a,b in intervals)
  candidates.append((covered,len(chain),chain,intervals))
 covered,_,chain,intervals=max(candidates,key=lambda v:(v[0],v[1]));extended=[]
 for qi,ti in chain:
  l=0
  while qi-l>0 and ti-l>0 and q[qi-l-1]==ts[ti-l-1]:l+=1
  r=7
  while qi+r<n and ti+r<N and q[qi+r]==ts[ti+r]:r+=1
  extended.append((qi-l,qi+r))
 ext=union_intervals(extended);ec=sum(b-a for a,b in ext);coverage=covered/n;extendedcoverage=ec/n
 missing=[];last=0
 for a,b in ext:
  if a>last:missing.append([last,a])
  last=b
 if last<n:missing.append([last,n])
 flags=[]
 if coverage<.7:flags.append('LOW_RARE_ANCHOR_COVERAGE')
 if extendedcoverage<.8:flags.append('LOW_EXTENDED_EXACT_COVERAGE')
 if len(chain)<2:flags.append('SINGLE_ANCHOR_ONLY')
 if missing and max(b-a for a,b in missing)>=20:flags.append('LONG_UNANCHORED_SPAN')
 # No full alternate text. At most eight seven-token exact anchors, and twelve short gap excerpts.
 sample=[chain[i] for i in sorted(set(round(k*(len(chain)-1)/min(7,len(chain)-1)) for k in range(min(8,len(chain))))) ] if len(chain)>1 else chain
 anchors=[{'query_token_start':i,'alternative_token_start':j,'normalized_7gram':' '.join(q[i:i+7]),'locator':loc(w,j)} for i,j in sample]
 return {'status':'anchored_local_window','query_tokens':n,'rare_query_7grams_found':available,'rare_7gram_hits':len(pairs),'monotone_anchors':len(chain),'anchor_coverage':round(coverage,5),'extended_exact_token_coverage':round(extendedcoverage,5),'matched_query_token_intervals':ext,'unanchored_query_spans':missing,'gap_excerpts':[{'tokens':[a,b],'normalized_excerpt':' '.join(q[a:min(b,a+14)])} for a,b in sorted(missing,key=lambda s:s[1]-s[0],reverse=True)[:12]],'alternative_token_window':[max(0,min(j-i for i,j in chain)-64),min(N,max(j-i for i,j in chain)+n+64)],'alternative_start':loc(w,chain[0][1]),'alternative_end':loc(w,chain[-1][1]+6),'flags':flags,'anchors':anchors}

def main():
 ap=argparse.ArgumentParser(description=__doc__);ap.add_argument('--repo',type=Path,required=True);ap.add_argument('--source-dir',type=Path,required=True);ap.add_argument('--output',type=Path,default=Path('/tmp/full-alternative-alignment.jsonl'));ap.add_argument('--summary',type=Path,default=Path('/tmp/full-alternative-alignment-summary.json'));args=ap.parse_args()
 units=[];inputfiles=[]
 for volume,name in [(1,'corpus.json'),(2,'volume2-preview.json')]:
  p=args.repo/'app/src/main/assets'/name;d=json.loads(p.read_text());inputfiles.append({'volume':volume,'path':str(p),'sha256':sha(p)})
  for c in d['chapters']:
   for par in c['paragraphs']:
    if par.get('role')!='author':continue
    pages=par.get('source_pages')
    if not pages:
     m=re.search(r'стр\.\s*(\d+)(?:[–-](\d+))?',par.get('source',''));assert m,par['id'];pages=list(range(int(m[1]),int(m[2] or m[1])+1))
    units.append({'paragraph_id':par['id'],'volume':volume,'printed_pages_2019':pages,'ar_sha256':hashlib.sha256(par['ar'].encode()).hexdigest(),'query':toks(par['ar'])})
 summary={'scope':'MACHINE_ALIGNMENT_ONLY_NOT_SEMANTIC_OR_VISUAL_EDITION_VERIFICATION','inputs':inputfiles,'unit_counts':dict(collections.Counter(u['volume'] for u in units)),'witnesses':[],'thresholds':{'rare_ngram_length':7,'maximum_witness_occurrences':3,'offset_bin_tokens':96,'local_offset_neighbor_bins':1,'max_candidate_bins':6,'low_anchor_coverage':.7,'low_extended_coverage':.8},'warning':'No text replacement. Older witnesses1973/1996 differ from2019. PageV locators are preserved as BOTH surrounding markers: neither marker placement convention nor edition pagination is visually certified. Normalization erases distinctions. Anchor coverage is not textual correctness.'}
 with args.output.open('w',encoding='utf8') as out:
  out.write(json.dumps({'record_type':'method','summary':summary},ensure_ascii=False)+'\n')
  for version in ['JK000081','Shamela0008370']:
   path=args.source_dir/f'0751IbnQayyimJawziyya.MadarijSalikin.{version}-ara1';w=witness(path);stats=collections.Counter();flags=collections.Counter();pageunits=collections.defaultdict(list);low=[]
   for u in units:
    result=align(u['query'],w);record={'record_type':'paragraph','witness':version,'scope':summary['scope'],**{k:v for k,v in u.items() if k!='query'},**result};out.write(json.dumps(record,ensure_ascii=False)+'\n');stats[result['status']]+=1;flags.update(result['flags'])
    for page in u['printed_pages_2019']:pageunits[(u['volume'],page)].append(record)
    if result['flags']:low.append({'paragraph_id':u['paragraph_id'],'volume':u['volume'],'pages':u['printed_pages_2019'],'coverage':result['extended_exact_token_coverage'],'flags':result['flags']})
   for (volume,page),rs in sorted(pageunits.items()):
    # This aggregation uses complete reader units, not invented within-unit page cuts.
    known=[r for r in rs if r['extended_exact_token_coverage'] is not None];den=sum(r['query_tokens'] for r in known)
    page_record={'record_type':'page_unit_summary','witness':version,'volume':volume,'printed_page_2019':page,'paragraph_ids':[r['paragraph_id'] for r in rs],'crosspage_reader_units':[r['paragraph_id'] for r in rs if len(r['printed_pages_2019'])>1],'coverage_scope':'RELATED_FULL_READER_UNITS_NOT_PAGE_EXACT_IF_SPLIT_OR_CROSSPAGE','weighted_extended_coverage':round(sum(r['query_tokens']*r['extended_exact_token_coverage'] for r in known)/den,5) if den else None,'flags':sorted(set(f for r in rs for f in r['flags'])),'alternative_marker_brackets':[{'paragraph_id':r['paragraph_id'],'start':r.get('alternative_start'),'end':r.get('alternative_end')} for r in rs]};out.write(json.dumps(page_record,ensure_ascii=False)+'\n')
   ws={'version':version,'path':str(path),'sha256':w['sha256'],'tokens':len(w['tokens']),'page_markers':len(w['markers']),'rare_7gram_index_entries':len(w['index']),'paragraph_status_counts':dict(stats),'flag_counts':dict(flags),'flagged_units':low};summary['witnesses'].append(ws);print(version,dict(stats),dict(flags),flush=True)
 args.summary.write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n');print('OUTPUT',args.output,'SUMMARY',args.summary,flush=True)
if __name__=='__main__':main()
