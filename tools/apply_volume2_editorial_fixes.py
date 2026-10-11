#!/usr/bin/env python3
"""Move identifiable editor-only remarks out of Ibn al-Qayyim's Russian main text.

Conservative curated changes to seven phrases on five printed pages. No automated
sentence splitting, no religious source claims, no first-volume changes.
"""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
p=root/"app/src/main/assets/volume2-preview.json"
book=json.loads(p.read_text(encoding="utf8"))
replacements={
 "madarij-v2-p622-draft":[
  " В текстовом арабском корпусе некоторые слова этих аятов повреждены и требуют сверки с мусхафом."
 ],
 "madarij-v2-p623-draft":[
  " Происхождение истории о причине ниспослания требует проверки.",
  " Точную формулировку и хадисоведческую оценку второй передачи следует уточнить."
 ],
 "madarij-v2-p626-draft":[
  " Точная атрибуция сообщения требует проверки."
 ],
 "madarij-v2-p639-draft":[
  "Здесь описывается **богословское толкование конкретного аята**, а не совет желать смерти себе или другим.\n\n"
 ],
 "madarij-v2-p640-draft":[
  "Это наставление осуждает показуху и зависимость от чужой похвалы; оно не отменяет обязанности беречь чужие права и не раскрывать без необходимости скрытые грехи."
 ],
 "madarij-v2-p659-draft":[
  "\n\n**Конец текста второго тома в выбранном арабском издании — печатная страница 659.**"
 ],
}
assert not book["publication_ready"] and not book["translation_independently_verified"]
edited=[]
allpars={x["id"]:x for ch in book["chapters"] for x in ch["paragraphs"]}
for name,cuts in replacements.items():
 x=allpars[name]
 for cut in cuts:
  assert x["ru"].count(cut)==1,(name,cut)
  x["ru"]=x["ru"].replace(cut,"")
  edited.append({"id":name,"page":int(name.split("-p")[1][:3]),"editorial_insert_removed":cut.strip(),
     "preserved_author_text":True})
 x["ru"]=x["ru"].rstrip()
assert len(edited)==7 and len(book["chapters"])==9
assert sum(len(ch["paragraphs"]) for ch in book["chapters"])==676
p.write_text(json.dumps(book,ensure_ascii=False,indent=2)+"\n",encoding="utf8")
out=root/"research/volume2-scholarly-audit"
out.mkdir(exist_ok=True,parents=True)
(out/"precise-edits.json").write_text(json.dumps(edited,ensure_ascii=False,indent=2)+"\n",encoding="utf8")
print("PASS separated",len(edited),"clearly non-authorial phrases", "from",len(replacements),"pages")
