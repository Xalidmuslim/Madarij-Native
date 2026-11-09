"""Replay exact historical Python payloads; only redirect historical scratch paths."""
from pathlib import Path
import hashlib,json
root=Path(__file__).resolve().parent
original=root/'original'
out=root/'replayed'
out.mkdir(exist_ok=True)
prefix=(original/'common-prefix-389.original.py').read_text()
results=[]
for first,last in [(549,556),(339,348),(389,398),(439,448),(499,508)]:
 body=original/f'batch{first}-{last}.body.original.py'
 final=original/f'batch{first}-{last}.finalize.original.py'
 if not body.exists():continue
 if first==339:
  code=body.read_text()
 else:
  code=prefix.replace('range(389,399)',f'range({first},{last+1})').replace('batch389-398',f'batch{first}-{last}')+'\n'+body.read_text()
 code=code.replace('/workspace/scratch/volume4',str(out))
 exec(compile(code,str(body),'exec'),{})
 if final.exists():exec(compile(final.read_text().replace('/workspace/scratch/volume4',str(out)),str(final),'exec'),{})
 path=out/f'batch{first}-{last}.json'
 x=json.loads(path.read_text())
 results.append({'file':str(path.relative_to(root)),'pages':len(x['pages']),'units':len(x['units']),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'historical_body':str(body.relative_to(root)),'historical_finalize':str(final.relative_to(root)) if final.exists() else None})
manifest={'provenance':'historical_tool_inputs_replay','not_a_backup':True,'not_original_git_commit':True,'replay_only_change':'Redirect /workspace/scratch/volume4 to recovery/agent339/replayed; generated prefix range/output file names reconstructed by the exact historical transformations.','outputs':results,'missing_bodies':[f'batch{a}-{b}' for a,b in [(339,348),(389,398),(439,448),(499,508),(549,556)] if not (original/f'batch{a}-{b}.body.original.py').exists()]}
(root/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
