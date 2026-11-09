import builtins,runpy,sys
from pathlib import Path
root=Path('/workspace/recovery/agent309')
original_open=builtins.open
def recovery_open(file,*args,**kwargs):
 if isinstance(file,(str,Path)) and str(file).startswith('/workspace/scratch/volume4/batch'):
  file=root/Path(file).name
 return original_open(file,*args,**kwargs)
builtins.open=recovery_open
for script in sys.argv[1:]:runpy.run_path(script,run_name='__main__')
