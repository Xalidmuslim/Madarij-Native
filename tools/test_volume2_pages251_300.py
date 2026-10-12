#!/usr/bin/env python3
"""Regression tests for selected corrected Arabic/Russian material, volume II pages 251–300.
This is *not* certification of every page, printed edition or chains of transmission.
"""
import json,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
BOOK=ROOT/"app/src/main/assets/volume2-preview.json"
class CheckPages251300(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.book=json.loads(BOOK.read_text(encoding="utf8"))
        cls.paragraphs={p["id"]:p for c in cls.book["chapters"] for p in c["paragraphs"]}
    def page(self,n):
        return self.paragraphs[f"madarij-v2-p{n:03d}-draft"]
    def test_uninterrupted_author_voice(self):
        probes={
            251:["Поэт сказал:"],
            256:["Поэт сказал:"],
            273:["по объяснению автора"],
            276:["По мнению автора"],
            283:["по мысли автора"],
            287:["Поэт сказал:"],
            288:["Поэт сказал:"],
            294:["В качестве примеров чрезмерного поиска удобств автор приводит"],
            298:["по мнению автора"],
        }
        for n,terms in probes.items():
            for term in terms:
                with self.subTest(n=n,term=term):
                    self.assertNotIn(term,self.page(n)["ru"])
    def test_full_author_legal_examples_page294(self):
        ru=self.page(294)["ru"]
        for term in ("тамбуре", "град", "персидском", "ташаххуд",
                     "дочерью", "внебрачной", "более слабых"):
            with self.subTest(term=term):
                self.assertIn(term,ru)
    def test_page294_quran_quote_55_64(self):
        ar=self.page(294)["ar"]
        self.assertIn("{مُدْهَامَّتَانِ} [الرحمن: ٦٤]",ar)
        self.assertNotIn("{(٦٣)}",ar)
    def test_page298_quran_qiraat_source_not_approbation(self):
        ar=self.page(298)["ar"]
        self.assertIn("{ابْتِغَاءَ رِضْوَانِ اللَّهِ}",ar)
        ru=self.page(298)["ru"]
        for term in ("приложением", "части целого", "ошибочной заменой"):
            self.assertIn(term,ru)
    def test_preserve_other_volumes_and_draft_status(self):
        self.assertEqual(len(self.book["chapters"]),9)
        self.assertEqual(len(self.paragraphs),676)
        self.assertFalse(self.book["publication_ready"])
        self.assertFalse(self.book["translation_independently_verified"])
        for p in (448,463,507,622):
            self.assertIn(f"madarij-v2-p{p:03d}-draft",self.paragraphs)
if __name__=="__main__":
    unittest.main()
