import json
p='/workspace/scratch/volume4/batch359-368.json';d=json.load(open(p))
def merge(a,b,ru=None):
 x=next(u for u in d['units'] if u['id']==a+'-ru');y=next(u for u in d['units'] if u['id']==b+'-ru')
 x['source_ids']+=y['source_ids'];x['printed_pages']+= [n for n in y['printed_pages'] if n not in x['printed_pages']];x['ar']+=' '+y['ar'];x['ru']=ru or x['ru']+' '+y['ru'];d['units'].remove(y)
merge('v4-p359-04','v4-p360-01')
merge('v4-p360-07','v4-p361-01')
merge('v4-p362-05','v4-p363-01')
merge('v4-p364-04','v4-p365-01')
merge('v4-p365-05','v4-p366-01')
merge('v4-p366-05','v4-p367-01')
merge('v4-p367-04','v4-p368-01')
for u in d['units']:
 if u['id']=='v4-p359-01-ru':u['ru']=u['ru'].replace('сопровождающее раба от Истинного','сопровождающее раба в его отношении к Истинному').replace('Осуществление этого — тахкик —','Удостоверение в этом — тахкик —')
 if u['id']=='v4-p359-04-ru':u['ru']=u['ru'].replace('с одним лишь своим спутником — связанным с Истинным','лишь с сопровождающим его в отношении Истинного')
 if u['id']=='v4-p361-02-ru':u['ru']=u['ru'].replace('Его знание не оспаривало твоего знания','твоё знание не оспаривало Его знания')
 if u['id']=='v4-p361-06-ru':u['ru']=u['ru'].replace('Его созерцание не оспаривало твоего созерцания','твоё созерцание не оспаривало Его созерцания')
json.dump(d,open(p,'w'),ensure_ascii=False,indent=2)
import json,hashlib
p='/workspace/scratch/volume4/batch359-368.json';d=json.load(open(p))
for pg in d['pages']:
 for s in pg['paragraphs']:
  s['ar']=s['ar'].replace('وإن يكون بسواه','وأن يكون بسواه').replace('مدارات المعرفة','مدار المعرفة').replace('وإنما أجبنا من أجابنا','وإنما أجابنا من أجابنا');s['sha256']=hashlib.sha256(s['ar'].encode()).hexdigest()
src={s['id']:s for pg in d['pages'] for s in pg['paragraphs']}
for u in d['units']:
 u['ar']=' '.join(src[s]['ar'] for s in u['source_ids'])
 u['ru']=u['ru'].replace('со Своими творениями знанием управлением','со Своими творениями знанием, управлением').replace('Когда это стало известно, то сопровождающее','После этого разъяснения сопровождающее')
json.dump(d,open(p,'w'),ensure_ascii=False,indent=2)
import json
p='/workspace/scratch/volume4/batch359-368.json';d=json.load(open(p))
for u in d['units']:
 u['ru']=u['ru'].replace('от сопровождающего его, связанного с Истинным','от того, что сопровождает его в отношении Истинного').replace('от сопровождающего его и от того','от того, что сопровождает его, и от того').replace('останется лишь с сопровождающим его в отношении Истинного','останется лишь с тем, что сопровождает его в отношении Истинного').replace('Ведь в противном случае ангел и так','Ведь ангел и так').replace('состоит в обнаружении того','состоит в показе того').replace('судебные решения — с доказательствами','судебные дела — с доказательствами').replace('Так же и судебные решения — то есть дела','Так же и судебные дела — то есть события').replace('Эти промежуточные причины заслонили людей разделённого восприятия от подлинно Действующего, Пречистого','Так Он посредством этих промежуточных причин скрыл от людей разделённого восприятия подлинно Действующего, Пречистого')
json.dump(d,open(p,'w'),ensure_ascii=False,indent=2)
