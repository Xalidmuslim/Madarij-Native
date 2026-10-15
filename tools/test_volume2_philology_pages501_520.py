"""Source fidelity regression, second volume printed pp501–520."""
import unittest,json
from pathlib import Path
P=Path(__file__).resolve().parents[1]
APP=json.loads((P/"app/src/main/assets/volume2-preview.json").read_text(encoding="utf-8"))
D=P/"research/volume2-batch-421-520"
class Volume2Fidelity501to520(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.app={p["id"]:p for c in APP["chapters"] for p in c["paragraphs"]}
  cls.ru={}
  for r in ("501-510","511-520"):
   cls.ru.update({p["printed_page"]:p for p in json.loads((D/f"russian-pages-{r}.json").read_text(encoding="utf-8"))["pages"]})
 def page(self,n):return self.app[f"madarij-v2-p{n}-draft"]["ru"]
 def test_sync(self):
  self.assertFalse(APP["publication_ready"])
  self.assertFalse(APP["translation_independently_verified"])
  for n in range(501,521):
   with self.subTest(n=n):
    self.assertEqual(self.page(n),self.ru[n]["ru"])
    self.assertFalse(self.ru[n]["publisher_ready"])
 def test_source(self):
  for n,term in [[507,"многобожники использовали факт"],[516,"проявятся следствия Его мудрости"],[517,"причины. Но сотворение"],[518,"Установленное на надлежащее место наказание"],[520,"Так он не будет доволен"]]:
   with self.subTest(n=n):self.assertIn(term,self.page(n))
  self.assertNotIn("историческое происхождение рассказа нуждается",self.page(520).lower())
if __name__=="__main__":unittest.main()
