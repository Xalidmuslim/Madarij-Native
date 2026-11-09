from pathlib import Path
import json,ast
root=Path('/workspace/recovery');dest=Path('/workspace/Madarij-Native/research/volume4/recovery/restored-history');copied=0
for d in root.iterdir():
 if not d.is_dir():continue
 for f in d.rglob('*'):
  if not f.is_file() or f.suffix not in ['.json','.py','.patch'] or '__pycache__' in f.parts:continue
  b=f.read_bytes()
  try:
   if f.suffix=='.json':json.loads(b)
   elif f.suffix=='.py':ast.parse(b.decode())
   elif not b.startswith(b'*** Begin Patch'):continue
  except (ValueError,UnicodeError,SyntaxError):continue
  out=dest/f.relative_to(root);out.parent.mkdir(parents=True,exist_ok=True)
  if not out.exists() or out.read_bytes()!=b:out.write_bytes(b);copied+=1
print('Updated recovery artifacts:',copied)
