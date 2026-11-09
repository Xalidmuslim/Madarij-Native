import json,hashlib
pages={n:[] for n in range(389,399)};units=[]
def a(n,ar,ru,cont=None):
 p=pages[n];sid=f'v4-p{n:03d}-{len(p)+1:02d}';p.append(dict(id=sid,ar=ar,sha256=hashlib.sha256(ar.encode()).hexdigest(),witness='uploaded_2019_control_pdf',source_complete=True))
 if cont is not None:
  u=units[cont];u['source_ids'].append(sid);u['printed_pages'].append(n);u['ar']+=' '+ar;u['ru']+=' '+ru
 else: units.append(dict(id=sid+'-ru',source_ids=[sid],printed_pages=[n],ar=ar,ru=ru,draft_status='draft',print_collation='control_image_read',independent_human_review=False))
 save();return len(units)-1

def save():
 obj={'pages':[dict(printed_page=n,pdf_leaf=n,witness_status='control_print_visually_read',publisher_footnote_text_imported=False,paragraphs=pages[n]) for n in pages],'units':units,'boundary_notes':[],'review_notes':[],'completed':False}
 open('/workspace/scratch/volume4/batch389-398.json','w').write(json.dumps(obj,ensure_ascii=False,indent=2)+'\n')
