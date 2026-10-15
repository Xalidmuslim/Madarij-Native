"""Electronic Arabic fidelity checks for volume II pages 521–550."""
import json,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
APP=json.loads((ROOT/"app/src/main/assets/volume2-preview.json").read_text(encoding="utf-8"))
DIR=ROOT/"research/volume2-batch-521-620"
class Volume2Fidelity521to550(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.app={v["id"]:v for c in APP["chapters"] for v in c["paragraphs"]}
  cls.ru={}
  for name in ("521-530","531-540","541-550"):
   cls.ru.update({v["printed_page"]:v for v in json.loads((DIR/f"russian-pages-{name}.json").read_text(encoding="utf-8"))["pages"]})
 def page(self,n):return self.app[f"madarij-v2-p{n}-draft"]["ru"]
 def test_synced(self):
  self.assertFalse(APP["publication_ready"])
  self.assertFalse(APP["translation_independently_verified"])
  for n in range(521,551):
   with self.subTest(n=n):
    self.assertEqual(self.page(n),self.ru[n]["ru"])
    self.assertFalse(self.ru[n]["publisher_ready"])
 def test_source(self):
  for n,term in [[529,"От наказания Аллаха спасётся"],[529,"его убеждённость поражена"],[530,"Довольство и убеждённость идут вместе"],[533,"«Поистине,"],[534,"Моё довольство — в твоём довольстве"],[534,"закрывает эти"],[535,"врата. Если ты поразмыслишь"],[535,"не доволен Его именами и атрибутами"],[536,"и предопределение самого греха"],[539,"духовной высотой ответа"]]:
   with self.subTest(n=n):self.assertIn(term,self.page(n))
  self.assertNotIn("Моё довольство — в твоём довольстве Моим решением",self.page(533))
if __name__=="__main__":unittest.main()
