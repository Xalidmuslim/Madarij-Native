import json,hashlib
p='/workspace/scratch/volume4/batch409-418.json';d=json.load(open(p))
for pg in d['pages']:
 for s in pg['paragraphs']:
  s['ar']=s['ar'].replace('أعظم الشواهد الأدلة','أعظم الشواهد والأدلة');s['sha256']=hashlib.sha256(s['ar'].encode()).hexdigest()
for u in d['units']:
 u['ar']=u['ar'].replace('أعظم الشواهد الأدلة','أعظم الشواهد والأدلة')
 if u['id']=='v4-p417-03-ru':u['ru']=u['ru'].replace('исчезновение знаний свидетельств в чистом знании от Него','полное исчезновение знаний свидетельств в знании, дарованном от Него').replace('всё, что вмещает указание','всё, что охватывается указанием')
 if u['id']=='v4-p417-04-ru':u['ru']=u['ru'].replace('Знание от Него — аль-‘ильм аль-лядунни —','Знание, дарованное от Него (ладунни), —')
 if u['id']=='v4-p413-04-ru':u['ru']=u['ru'].replace('Подлинное понимание сына воды и глины состоит в том, чтобы','Постичь истинную природу сына воды и глины — значит добиться, чтобы')
def merge(a,b,ru=None):
 x=next(u for u in d['units'] if u['id']==a+'-ru');y=next(u for u in d['units'] if u['id']==b+'-ru')
 x['source_ids']+=y['source_ids'];x['printed_pages']+=[n for n in y['printed_pages'] if n not in x['printed_pages']];x['ar']+=' '+y['ar'];x['ru']=ru or x['ru']+' '+y['ru'];d['units'].remove(y)
merge('v4-p409-05','v4-p410-01')
merge('v4-p410-04','v4-p411-01')
merge('v4-p411-04','v4-p412-01')
merge('v4-p412-05','v4-p413-01')
merge('v4-p413-06','v4-p414-01')
merge('v4-p415-04','v4-p416-01')
merge('v4-p416-04','v4-p417-01')
merge('v4-p417-04','v4-p418-01')
# Smooth the final split around conjunction and direct object.
u=next(u for u in d['units'] if u['id']=='v4-p417-04-ru')
u['ru']=u['ru'].replace('знания, опирающиеся на доказательства, исчезают и свидетельства, в знании','знания, опирающиеся на доказательства и свидетельства, исчезают в знании')
json.dump(d,open(p,'w'),ensure_ascii=False,indent=2)
import json
p='/workspace/scratch/volume4/batch409-418.json';d=json.load(open(p))
for u in d['units']:
 u['ru']=u['ru'].replace('всего, что вмещает указание','всего, что охватывается указанием')
json.dump(d,open(p,'w'),ensure_ascii=False,indent=2)
import json
p='/workspace/scratch/volume4/batch409-418.json';d=json.load(open(p))
for u in d['units']:
 u['ru']=u['ru'].replace('«И научили его знанию от Нас»','«И Мы научили его знанию от Нас»')
 if u['id']=='v4-p418-02-ru':u['ru']=u['ru'].replace('непосредственная убеждённость','непосредственное видение убеждённости')
 if u['id']=='v4-p415-01-ru':u['ru']=u['ru'].replace('двойственность, от которой необходимо избавиться, — это признать двух богов и созерцать наряду с Аллахом другого бога','двойственность, от которой необходимо избавиться, заключается в признании двух богов и созерцании наряду с Аллахом другого бога')
json.dump(d,open(p,'w'),ensure_ascii=False,indent=2)
