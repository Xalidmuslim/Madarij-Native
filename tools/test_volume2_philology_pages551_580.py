"""Regression checks for manually electronic-reviewed pp551–580."""
import unittest,json
from pathlib import Path
P=Path(__file__).resolve().parents[1]
APP=json.loads((P/"app/src/main/assets/volume2-preview.json").read_text(encoding="utf-8"))
BASE=P/"research/volume2-batch-521-620"
class Volume2SourceFidelity551to580(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.app={x["id"]:x for c in APP["chapters"] for x in c["paragraphs"]}
  cls.pages={}
  for name in ("551-560","561-570","571-580"):
   cls.pages.update({x["printed_page"]:x for x in json.loads((BASE/f"russian-pages-{name}.json").read_text(encoding="utf-8"))["pages"]})
 def get(self,n):return self.app[f"madarij-v2-p{n}-draft"]["ru"]
 def test_sync(self):
  self.assertFalse(APP["publication_ready"])
  self.assertFalse(APP["translation_independently_verified"])
  for n in range(551,581):
   with self.subTest(n=n):
    self.assertEqual(self.get(n),self.pages[n]["ru"])
    self.assertFalse(self.pages[n]["publisher_ready"])
 def test_source_fidelity(self):
  assertions=[[557,"не просить ни у кого, кроме Него"],[558,"Того, Кто его предопределил"],[561,"имеют изъян и не свободны"],[562,"высотой его устремления"],[572,"«любимым и надёжным»"],[573,"пусть лишится его"],[574,"свиток аль-Муталаммиса"],[575,"запретное приобретение"],[578,"согласовывать своё отношение"]]
  for n,term in assertions:
   with self.subTest(n=n):self.assertIn(term,self.get(n))
if __name__=="__main__":unittest.main()
