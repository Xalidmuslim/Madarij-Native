#!/usr/bin/env python3
"""Regression: Arabic-Russian source fidelity for volume II, printed p.301-350.

Assertions cover confirmed repairs, NOT independent scholarly/print approval.
"""
import json
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
BOOK=ROOT/"app/src/main/assets/volume2-preview.json"

class Vol2Pages301350(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data=json.loads(BOOK.read_text(encoding="utf-8"))
        cls.p={p["id"]:p for c in cls.data["chapters"] for p in c["paragraphs"]}

    def page(self,n):
        return self.p[f"madarij-v2-p{n:03d}-draft"]

    def test_no_truncated_logic_and_author_interpositions(self):
        must={
            311:["все праведные предшественники","запрещённое","дозволенное"],
            312:["разорение мира","разрушение основ религии"],
            313:["своего тагута","Вам переданное знание"],
            319:["защиту от пренебрежения"],
            329:["Следует сказать так:"],
            330:["любое проявление Его довольства"],
        }
        for n,terms in must.items():
            for term in terms:
                with self.subTest(page=n,term=term):
                    self.assertIn(term,self.page(n)["ru"])
        removals={
            311:["по мнению автора"],
            312:["по оценке автора"],
            313:["по оценке автора"],
            319:["по мнению автора"],
            330:["Поэт сказал:","по объяснению автора"],
            333:["По утверждению автора"],
            334:["по объяснению автора"],
            337:["автор приводит"],
            338:["по словам автора","по оценке автора"],
            340:["по словам автора","по его мнению","по оценке автора"],
            341:["По мнению автора","автор говорит"]
        }
        for n,phrases in removals.items():
            for phrase in phrases:
                with self.subTest(page=n,phrase=phrase):
                    self.assertNotIn(phrase,self.page(n)["ru"])

    def test_corrupt_10_25_quote_restored(self):
        ar=self.page(329)["ar"]
        self.assertIn("{وَاللَّهُ يَدْعُو إِلَى دَارِ السَّلَامِ} [يونس: ٢٥]",ar)
        self.assertNotIn("{(٢٤) وَاللَّهُ يَدْعُو إِلَى دَارِ}",ar)

    def test_quran_67_2_no_editor_footnote_inside_quote(self):
        ar=self.page(344)["ar"]
        self.assertIn("{الَّذِي خَلَقَ الْمَوْتَ وَالْحَيَاةَ",ar)
        self.assertNotIn("{(¬٣) الَّذِي",ar)

    def test_volume_two_only_and_draft_remains(self):
        self.assertEqual(len(self.data["chapters"]),9)
        self.assertEqual(len(self.p),676)
        self.assertFalse(self.data["publication_ready"])
        self.assertFalse(self.data["translation_independently_verified"])
        for n in (448,463,507,622):
            self.assertIn(f"madarij-v2-p{n:03d}-draft",self.p)

if __name__=="__main__":
    unittest.main()
