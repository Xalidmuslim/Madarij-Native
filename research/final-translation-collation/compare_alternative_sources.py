"""Research alignment, not independent semantic or printed-edition verification."""
import re,json,unicodedata,difflib,hashlib
from pathlib import Path
import argparse
import os
ap = argparse.ArgumentParser(description='Compare v1 pages 3-42 with locally downloaded, pinned OpenITI witnesses; no download or source replacement.')
ap.add_argument('--source-dir', required=True, type=Path)
args = ap.parse_args()
root = Path(__file__).resolve().parents[2]
os.chdir(root)
out = root / 'research/final-translation-collation'
book = json.loads(Path('app/src/main/assets/corpus.json').read_text())
pars=[p for c in book['chapters'] for p in c['paragraphs'] if p['role']=='author' and min(p['source_pages'])<=42]
def toks(s):
 s=re.sub(r'\([^)]*\)|\[[^]]*\]',' ',s)
 s=unicodedata.normalize('NFKD',s)
 s=''.join(c for c in s if not unicodedata.combining(c)).replace('ـ','').replace('ٱ','ا')
 return re.findall('[ء-ي]+',s)
a=[];ids=[]
for p in pars:
 t=toks(p['ar']);a+=t;ids+=[p['id']]*len(t)
results=[]
for version in ['JK000081','Shamela0008370']:
 path=args.source_dir / ('0751IbnQayyimJawziyya.MadarijSalikin.'+version+'-ara1');raw=path.read_text();body=raw.split('#META#Header#End#',1)[1];body=re.sub(r'PageV\d+P\d+',' ',body);b=toks(body)
 # Locate the end by the closing sentence of corpus p42, preserving other edition pagination.
 tail=a[-4:];end=next(i+len(tail) for i in range(len(b)-len(tail)+1) if b[i:i+len(tail)]==tail);b=b[:end]
 sm=difflib.SequenceMatcher(None,a,b,autojunk=False);diff=[]
 for tag,i,j,k,l in sm.get_opcodes():
  if tag!='equal':diff.append({'operation':tag,'paragraph_ids':list(dict.fromkeys(ids[max(0,i-1):min(len(ids),j+1)])),'corpus_tokens':' '.join(a[i:j]),'alternative_tokens':' '.join(b[k:l]),'left_context':' '.join(a[max(0,i-8):i]),'right_context':' '.join(a[j:j+8]),'review_status':'pending_editorial_review_not_an_instruction_to_replace_2019_text'})
 result={'version':version,'source_commit':'61b0c7891699554aeff8dffdb8b1a2a59d2d22ab','sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'corpus_pages':[3,42],'corpus_tokens':len(a),'alternative_tokens':len(b),'matching_tokens':sum(m.size for m in sm.get_matching_blocks()),'normalization':'Strip brackets/parentheses, diacritics and non-Arabic markers; NFKD. This loses distinctions and cannot establish edition fidelity.','differences':diff,'visual_comparison':False,'complete_semantic_comparison':False}
 (out/('V1_P003_042_'+version+'_DIFFERENCES.json')).write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(version,len(diff),'differences')
