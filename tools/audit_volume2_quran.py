#!/usr/bin/env python3
"""A 657-page machine-readable review queue; NOT independent religious verification."""
import json,re,unicodedata,difflib,urllib.request
from pathlib import Path
from collections import Counter,defaultdict
root=Path(__file__).resolve().parents[1]
out=root/"research/volume2-scholarly-audit"
out.mkdir(parents=True,exist_ok=True)
src={x["printed_page"]:x for line in (root/"research/volume2-source/arabic_pages.jsonl").read_text().splitlines() if line.strip() for x in [json.loads(line)]}
view=json.loads((root/"app/src/main/assets/volume2-preview.json").read_text())
pgr=[p for c in view["chapters"] for p in c["paragraphs"]]
assert len(src)==659 and len(pgr)==676 and not view["publication_ready"]
def norm(t):
 t=unicodedata.normalize("NFKD",t)
 t="".join(c for c in t if not unicodedata.combining(c) and not ("\u06d6"<=c<="\u06ed"))
 for a,b in (("ٱ","ا"),("أ","ا"),("إ","ا"),("آ","ا"),("ى","ي"),("ة","ه"),("ؤ","و"),("ئ","ي"),("ـ","")):t=t.replace(a,b)
 t=t.translate(str.maketrans("٠١٢٣٤٥٦٧٨٩","0123456789"))
 return "".join(re.findall(r"[\u0621-\u064a]+",t))
def save(file,data):
 (out/file).write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf8")
url="https://raw.githubusercontent.com/asim/quran-json-arabic/main/dist/quran.json"
with urllib.request.urlopen(urllib.request.Request(url,headers={"User-Agent":"Madarij-Editorial-CI/1"}),timeout=90) as r:
 raw=r.read(3000000)
whole=json.loads(raw)
if isinstance(whole,dict): whole=whole.get("chapters") or whole.get("surahs")
assert len(whole)==114
chapters={};verse={}; total=0
for c in whole:
 no=int(c.get("id",c.get("number")))
 chapters[norm(c["name"])]=no
 for x in c.get("verses",c.get("ayahs",[])):
  vno=int(x.get("id",x.get("number")))
  if vno==0:continue
  verse[(no,vno)]=x["text"]
  total+=1
assert total==6236,total
chapters.update({norm(n):v for n,v in (("آل عمران",3),("ق",50),("يس",36),("الإسراء",17),("طه",20))})
arref=re.compile(r"\[([^:\]\n]{1,40}):\s*([٠-٩0-9]+)(?:\s*[-–]\s*([٠-٩0-9]+))?\]")
ruref=re.compile(r"\((\d{1,3}):(\d{1,3})(?:[–-](\d{1,3}))?\)")
ed=re.compile(r"достоверност|хадисовед|необходимо\s+(?:сверить|проверить)|(?:требует|требуют)\s+(?:отдельной\s+|независимой\s+|самостоятельной\s+)?(?:сверки|проверки)|не\s+(?:следует|может)\s+(?:считать|выдавать)|редакцион|атрибуци",re.I)
footnote=re.compile(r"\s*\(¬[٠-٩0-9]+\)")
issues=[];qs=[];idx=defaultdict(list);same=0
for p in pgr:
 m=re.search(r"стр\.\s*(\d+)",p["source"])
 n=int(m.group(1)) if m else int(re.search(r"-p(\d{3})",p["id"]).group(1))
 idx[n].append(p)
 if p["id"].endswith("-draft"):
  ar=footnote.sub("",src[n]["arabic_body"]).strip()
  if p["ar"].strip()==ar:same+=1
  else:issues.append({"page":n,"id":p["id"],"type":"ARABIC_SOURCE_MISMATCH"})
 if ed.search(p["ru"]):issues.append({"page":n,"id":p["id"],"type":"EDITORIAL_COMMENT_IN_AUTHOR_TEXT","sample":ed.search(p["ru"]).group()})
 for x in ruref.finditer(p["ru"]):
  s,v,w=map(lambda a:int(a) if a else None,x.groups())
  if (s,v) not in verse or (w and ((s,w) not in verse or w<v)):
   issues.append({"page":n,"id":p["id"],"type":"INVALID_RU_QURAN_REFERENCE","ref":x.group()})
 for x in arref.finditer(p["ar"]):
  name,v,w=x.groups(); sur=chapters.get(norm(name)); n1=int(v.translate(str.maketrans("٠١٢٣٤٥٦٧٨٩","0123456789")));n2=int(w.translate(str.maketrans("٠١٢٣٤٥٦٧٨٩","0123456789"))) if w else n1
  item={"page":n,"id":p["id"],"ref":x.group(),"sura":sur,"start":n1,"end":n2}
  if not sur:item["status"]="SURAH_NOT_RESOLVED"
  elif n2<n1 or n2-n1>15 or any((sur,k) not in verse for k in range(n1,n2+1)):
   item["status"]="INVALID_REFERENCE"
  else:
   preceding=p["ar"][:x.start()]
   spans=list(re.finditer(r"\{([^{}\n]{5,500})\}",preceding))
   quote=spans[-1].group(1) if spans and x.start()-spans[-1].end()<90 else ""
   if quote and re.search(r"[٠-٩0-9]",quote):
    item["source_embedded_numbers"]=True
    issues.append({"page":n,"id":p["id"],"type":"EMBEDDED_NUMBER_IN_ARABIC_QURAN_QUOTE","ref":item["ref"]})
   if not quote:item["status"]="REFERENCE_WITHOUT_NEARBY_QUOTE"
   else:
    full=norm(" ".join(verse[(sur,k)] for k in range(n1,n2+1)))
    q=norm(quote)
    if len(q)<8:item["status"]="QUOTE_TOO_SHORT"
    elif q in full:item["status"]="EXACT_AFTER_ARABIC_NORMALIZATION"
    else:
     match=difflib.SequenceMatcher(None,q,full,autojunk=False).find_longest_match(0,len(q),0,len(full))
     item["match_ratio"]=round(match.size/max(1,len(q)),3)
     item["status"]="CHECK_QUOTE_MISMATCH" if item["match_ratio"]<.75 else "QUOTE_VARIANT_REQUIRES_REVIEW"
     item["quote_excerpt"]=quote[:170]
  qs.append(item)
  if item["status"] in ("INVALID_REFERENCE","CHECK_QUOTE_MISMATCH","SURAH_NOT_RESOLVED"):
   issues.append({"page":n,"id":p["id"],"type":item["status"],"ref":item["ref"]})
assert len(idx)==657 and set(idx)==set(range(3,660))
summary={
 "arabic_printed_pages":len(src),"russian_draft_printed_pages":len(idx),
 "paragraphs":len(pgr),"arabic_pages_matched_exactly_to_source":same,
 "arabic_quran_references_tested":len(qs),
 "citation_statuses":dict(Counter(item["status"] for item in qs)),
 "issue_counts":dict(Counter(x["type"] for x in issues)),
 "independently_semantically_reviewed_pages":0,
 "independently_authenticated_hadith":0,
 "hadith_primary_source_spot_checks":5,
 "curated_editorial_insertions_separated_from_author_text":7,
 "quran_verification_scope":"Automated Arabic quote and reference checks only, NOT verified Russian translations or tafsir",
 "publication_ready":False,
 "quran_data_source":url
}
save("summary.json",summary)
save("issues.json",issues)
save("quran-reference-screen.json",qs)
lines=["# Второй том: первый автоматический редакционный аудит","","**Не является полной экспертной или постраничной визуальной проверкой.**","",
f"- Охват основного текста: **{len(idx)}/657** страниц (стр. 3–659).",
f"- Арабских оригинальных страниц: **{len(src)}/659**.",
f"- Двуязычных фрагментов: **{len(pgr)}**.",
f"- Автоматически просмотренных арабских ссылок на Коран: **{len(qs)}**.",
f"- Буквально совпавших с корпусом одностраничных арабских текстов: **{same}**.","",
"## Типы обнаруженных замечаний",""]
for k,v in sorted(summary["issue_counts"].items(),key=lambda t:-t[1]):lines.append(f"- {k}: **{v}**")
lines+=["","## Статусы коранических фрагментов",""]
for k,v in sorted(summary["citation_statuses"].items(),key=lambda t:-t[1]):lines.append(f"- {k}: **{v}**")
lines+=["","## Список критических случаев",""]
for t in issues:
 if t["type"] in ("INVALID_REFERENCE","CHECK_QUOTE_MISMATCH","INVALID_RU_QURAN_REFERENCE"):
  lines.append(f"- Стр. {t['page']} — {t['type']}: {t.get('ref','')}")
lines+=["","## Что не пройдено","",
"1. Ручная сверка каждой страницы по скану выбранного издания.",
"2. Смысловая проверка русских переводов и отделение редакционных вставок от авторского повествования.",
"3. Полная проверка хадисных иснадов, их версий и степени достоверности.",
"4. Независимая религиозная, литературная и визуальная проверка приложения.",
"","Канонический арабский текст: https://github.com/asim/quran-json-arabic/blob/main/README.md",
"Проверено пять идентификаторов хадисных первоисточников; полный иснад-аудит остальных передач не проводился.",
"Все совпадения и несовпадения арабского текста с Кораном являются машинными подсказками и требуют визуальной проверки. За доказанные ошибки перевода их принимать нельзя.",
"См. hadith-spot-checks.json и precise-edits.json.",,
"","**Никакой автоматически пройденный тест не повышает публикационный статус черновика.**"]
(out/"README.md").write_text("\n".join(lines)+"\n",encoding="utf8")
print(json.dumps(summary,ensure_ascii=False,indent=2))
