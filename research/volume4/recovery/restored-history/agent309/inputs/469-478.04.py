import json
p='/workspace/scratch/volume4/batch469-478.json';d=json.load(open(p))
for u in d['units']:
 if u['id']=='v4-p469-03-ru':u['ru']=u['ru'].replace('Его Сущность не любят','Его Сущность не является предметом любви').replace('сердца не стремятся к Нему','сердца не томятся по Нему')
 if u['id']=='v4-p473-01-ru':u['ru']=u['ru'].replace('До тебя Мы посылали только мужей, которым внушалось откровение. Спросите людей Напоминания, если вы не знаете. С ясными доказательствами и Писаниями','До тебя Мы посылали с ясными доказательствами и Писаниями только мужей, которым внушалось откровение. Спросите людей Напоминания, если вы не знаете')
def merge(a,b,ru=None):
 x=next(u for u in d['units'] if u['id']==a+'-ru');y=next(u for u in d['units'] if u['id']==b+'-ru')
 x['source_ids']+=y['source_ids'];x['printed_pages']+=[n for n in y['printed_pages'] if n not in x['printed_pages']];x['ar']+=' '+y['ar'];x['ru']=ru or x['ru']+' '+y['ru'];d['units'].remove(y)
merge('v4-p470-05','v4-p471-01')
merge('v4-p472-03','v4-p473-01')
merge('v4-p475-03','v4-p476-01')
merge('v4-p476-04','v4-p477-01')
u=next(u for u in d['units'] if u['id']=='v4-p476-04-ru');u['ru']=u['ru'].replace('самодостаточность, щедрость благодеяние','самодостаточность, щедрость, благодеяние')
json.dump(d,open(p,'w'),ensure_ascii=False,indent=2)
import json
p='/workspace/scratch/volume4/batch469-478.json';d=json.load(open(p))
for u in d['units']:
 u['ru']=u['ru'].replace('Здесь заканчивается следствие условия','Здесь заканчивается ответная часть условного предложения')
 if u['id']=='v4-p475-03-ru':u['ru']=u['ru'].replace('пока им не станет ясно, что это — истина», — то есть Коран','пока им не станет ясно, что это — истина» [41:53], — то есть Коран')
json.dump(d,open(p,'w'),ensure_ascii=False,indent=2)
import json
p='/workspace/scratch/volume4/batch469-478.json';d=json.load(open(p))
for u in d['units']:
 u['ru']=u['ru'].replace('такие знамения, при виде которых люди уверовали','такие знамения, на основании которых люди уверовали').replace('Оно включает то, что засвидетельствованное Им Он разъяснил, прояснил и обнаружил','Оно подразумевает, что Он разъяснил, прояснил и обнаружил засвидетельствованное Им')
json.dump(d,open(p,'w'),ensure_ascii=False,indent=2)
