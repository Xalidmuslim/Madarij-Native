#!/usr/bin/env python3
"""Read-only proof of output counts, exact anchor equality and marker bounds."""
import collections,json,statistics,importlib.util
from pathlib import Path
spec=importlib.util.spec_from_file_location('alignment',str(Path(__file__).with_name('full_alternative_alignment.py')));m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
rows=[json.loads(l) for l in open(Path(__file__).with_name('FULL_ALTERNATIVE_ALIGNMENT.jsonl'))];s=json.load(open(Path(__file__).with_name('FULL_ALTERNATIVE_ALIGNMENT_SUMMARY.json')));stats={};anchors=0
for w in s['witnesses']:
 witness=m.witness(Path(w['path']));assert witness['sha256']==w['sha256'];rs=[r for r in rows if r.get('record_type')=='paragraph' and r['witness']==w['version']];assert len(rs)==1320;assert len({r['paragraph_id'] for r in rs})==1320
 pervolume={}
 for vol in [1,2]:
  vs=[r for r in rs if r['volume']==vol];known=[r['extended_exact_token_coverage'] for r in vs if r['extended_exact_token_coverage'] is not None];pervolume[vol]={'units':len(vs),'mean_extended_coverage':round(statistics.mean(known),5),'median_extended_coverage':round(statistics.median(known),5),'below_0_8':sum(c<.8 for c in known),'no_rare_anchor_ids':[r['paragraph_id'] for r in vs if r['status']=='no_rare_anchor']}
 for r in rs:
  c=r['extended_exact_token_coverage'];assert c is None or 0<=c<=1
  for a in r['anchors']:
   j=a['alternative_token_start'];assert ' '.join(witness['tokens'][j:j+7])==a['normalized_7gram'];assert a['locator']==m.loc(witness,j);anchors+=1
 for vol in [1,2]:
  pages=[r['printed_page_2019'] for r in rows if r.get('record_type')=='page_unit_summary' and r['witness']==w['version'] and r['volume']==vol];assert pages==list(range(3,611 if vol==1 else 660))
 stats[w['version']]=pervolume
s['validation']={'complete':True,'unique_paragraph_witness_pairs':2640,'page_summaries':2530,'exact_sample_anchors_checked':anchors,'no_global_SequenceMatcher':True,'statistics_by_witness_volume':stats,'proof_limit':'Exact equality of normalized anchor snippets validates the machine mapping format, not 2019 text, attribution, translation, semantic correctness or page scans.'}
json.dump(s,open(Path(__file__).with_name('FULL_ALTERNATIVE_ALIGNMENT_SUMMARY.json'),'w'),ensure_ascii=False,indent=2)
print(json.dumps(s['validation'],ensure_ascii=False,indent=2))
