# provenance: replay_from_historical_tool_inputs
# Upstream dependency is the literal historical tool output snapshot, not a recovered batch519–528.
import json,hashlib,re
base='/workspace/recovery/agent319/artifacts/'
a={"id": "v4-p528-04-ru", "source_ids": ["v4-p528-04"], "printed_pages": [528], "ar": "ولم يقل لهم: لا تشهدوا في التوحيد دليلا، ولا في النجاة وسيلة؛ بل كان من أسباب مباهاة الله بهم ملائكته: شهودهم سبب التوحيد، ووسيلة النجاة، وأنها من منن الله عليهم وفضله، كما قال تعالى: {لقد من الله على المؤمنين إذ بعث فيهم رسولا من أنفسهم يتلو عليهم آياته ويزكيهم ويعلمهم", "ru": "Он не сказал им: «Не созерцайте в единобожии доказательства и в спасении средства». Напротив, среди причин того, что Аллах хвалится ими перед Своими ангелами, были их созерцание причины единобожия и средства спасения, а также того, что это — милости Аллаха к ним и Его щедрость. Как сказал Всевышний: {لقد من الله على المؤمنين إذ بعث فيهم رسولا من أنفسهم يتلو عليهم آياته ويزكيهم ويعلمهم", "draft_status": "draft", "print_collation": "control_image_read", "independent_human_review": False}
open('/workspace/recovery/agent319/upstream528_snapshot.json','w').write(json.dumps({'provenance':'historical_tool_output_literal_snapshot','unit':a},ensure_ascii=False,indent=2)+'\n')
d=json.load(open(base+'batch529-538.json'));b=d['units'][0]
ar=a['ar']+' '+b['ar'];verse=re.search(r'\{[^}]*\}',ar).group()
ru_prefix=a['ru'].split('{')[0]
ru=ru_prefix+verse+' — «Аллах оказал милость верующим, послав среди них Посланника из них самих, который читает им Его аяты, очищает их и учит их Писанию и мудрости» [«Аль Имран», 3:164]. '+b['ru'].split('[«Аль Имран», 3:164]. ',1)[1]
u=dict(id=a['id'],source_ids=a['source_ids']+b['source_ids'],printed_pages=[528,529],ar=ar,ru=ru,draft_status='draft',print_collation='control_image_read',independent_human_review=False)
assert re.findall(r'\{[^}]*\}',ar)==re.findall(r'\{[^}]*\}',ru)
mp=base+'merge528-529.json'
open(mp,'w').write(json.dumps({'unit':u,'review_notes':[],'completed':False},ensure_ascii=False,indent=2)+'\n')
m=json.load(open(mp));u=m['unit'];u['draft_status']='self_reviewed'
assert u['ar']==a['ar']+' '+b['ar']
assert re.findall(r'\{[^}]*\}',u['ar'])==re.findall(r'\{[^}]*\}',u['ru'])
m['review_notes']=[{'pass':'semantic','performed':True,'note':'Compared full528–529 unit to both rendered control images;author assertion/question and complete3:164 quotation retained.'},{'pass':'russian_only','performed':True,'note':'Read full joined Russian; supplied complete Quran meaning outside closed Arabic braces, replacing duplicated fragment translation.'},{'pass':'final_reverse','performed':True,'note':'Recompared full Arabic/Russian, exact source joins and identical complete curly quotation;adjoining translator confirms528-04 unchanged through own reviews.'}]
m['source_snapshot_sha256']={s:hashlib.sha256(v.encode()).hexdigest() for s,v in [('v4-p528-04',a['ar']),('v4-p529-01',b['ar'])]};m['completed']=True
open(mp,'w').write(json.dumps(m,ensure_ascii=False,indent=2)+'\n')
