import json
p='/workspace/scratch/volume4/batch519-528.json';d=json.load(open(p))
for u in d['units']:
 u['ru']=u['ru'].replace('Если знание и решение предшествуют о несчастье','Если заранее известно и решено, что нас ждёт несчастье').replace('Если они предшествуют о счастье','Если заранее известно и решено, что нас ждёт счастье').replace('к кружку Своих сподвижников','к кружку своих сподвижников')
def merge(a,b,ru=None):
 x=next(u for u in d['units'] if u['id']==a+'-ru');y=next(u for u in d['units'] if u['id']==b+'-ru')
 x['source_ids']+=y['source_ids'];x['printed_pages']+=[n for n in y['printed_pages'] if n not in x['printed_pages']];x['ar']+=' '+y['ar'];x['ru']=ru or x['ru']+' '+y['ru'];d['units'].remove(y)
merge('v4-p519-05','v4-p520-01')
merge('v4-p520-05','v4-p521-01')
merge('v4-p522-03','v4-p523-01')
merge('v4-p523-04','v4-p524-01')
merge('v4-p524-04','v4-p525-01')
merge('v4-p525-05','v4-p526-01')
merge('v4-p526-03','v4-p527-01')
merge('v4-p527-05','v4-p528-01')
for u in d['units']:
 u['ru']=u['ru'].replace('между этими крайностями','между этими крайностями')
json.dump(d,open(p,'w'),ensure_ascii=False,indent=2)
import json
p='/workspace/scratch/volume4/batch519-528.json';d=json.load(open(p))
for u in d['units']:
 u['ru']=u['ru'].replace('То, что предшествует в Его решении и знании, — истина','То, что Им заранее предрешено и известно Ему, — истина').replace('которое бывает тоньше, грубее и между этими крайностями','которое бывает тонким, грубым и промежуточным между ними')
json.dump(d,open(p,'w'),ensure_ascii=False,indent=2)
import json
p='/workspace/scratch/volume4/batch519-528.json';d=json.load(open(p))
for u in d['units']:u['ru']=u['ru'].replace('мутят и расстраивают такой смысл','затемняют и запутывают этот смысл')
json.dump(d,open(p,'w'),ensure_ascii=False,indent=2)
