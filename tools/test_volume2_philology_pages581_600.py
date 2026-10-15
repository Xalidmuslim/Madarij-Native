"""Electronic Arabic-Russian fidelity checks, printed pages 581–600; not print certification."""
import json
import unittest
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
APP = json.loads((ROOT / "app/src/main/assets/volume2-preview.json").read_text(encoding="utf-8"))
BASE = ROOT / "research/volume2-batch-521-620"
class Collation581to600(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.app = {p["id"]:p for c in APP["chapters"] for p in c["paragraphs"]}
        cls.ru={}
        for s in ["581-590","591-600"]:
            cls.ru.update({p["printed_page"]:p for p in json.loads((BASE / f"russian-pages-{s}.json").read_text(encoding="utf-8"))["pages"]})
    def page(self,n): return self.app[f"madarij-v2-p{n}-draft"]["ru"]
    def test_sync_and_flags(self):
        self.assertFalse(APP["publication_ready"])
        self.assertFalse(APP["translation_independently_verified"])
        self.assertEqual(sum(len(c["paragraphs"]) for c in APP["chapters"]),676)
        for n in range(581,601):
            with self.subTest(page=n):
                self.assertEqual(self.page(n),self.ru[n]["ru"])
                self.assertFalse(self.ru[n]["publisher_ready"])
    def test_source_sense_restored(self):
        assertions=[[588,"даруй победу над притесняющим меня"],[586,"Он приводит благодарящего к Тому, Кого тот благодарит"],[587,"назвал Себя *Шакиром* и *Шакуром*"],[594,"назвал ислам и веру благодарностью"],[598,"и получение, и дарование"],[599,"поставили впереди то"],[600,"направиться с этим человеческим существованием к собственной душе"],[600,"среди людей умозрения и исследования"]]
        for n,phrase in assertions:
            with self.subTest(page=n,phrase=phrase):
                self.assertIn(phrase,self.page(n))
if __name__=="__main__":unittest.main()
