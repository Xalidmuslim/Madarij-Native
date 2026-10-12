#!/usr/bin/env python3
"""Arabic-Russian source fidelity, volume two printed pages 151–200.

These content tests do not certify print images or hadith authenticity.
"""
import json
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
BOOK=ROOT/"app/src/main/assets/volume2-preview.json"
class Page151200(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.book=json.loads(BOOK.read_text(encoding="utf8"))
        cls.fragments={p["id"]:p for c in cls.book["chapters"] for p in c["paragraphs"]}
    def page(self,n):
        return self.fragments[f"madarij-v2-p{n:03d}-draft"]

    def test_retain_edition_and_draft(self):
        self.assertEqual(len(self.book["chapters"]),9)
        self.assertEqual(len(self.fragments),676)
        self.assertFalse(self.book["publication_ready"])
        self.assertFalse(self.book["translation_independently_verified"])
        for n in (448,463,507,622):
            self.assertIn(f"madarij-v2-p{n:03d}-draft", self.fragments)

    def test_authorial_voice_not_moderated(self):
        needles={
            157:["По словам автора","Автор считает"],
            159:["по мысли автора","автор видит"],
            160:["автор считает очевидным","по его объяснению"],
            161:["Автор считает их свободными"],
            162:["По оценке автора"],
            164:["Поэт сказал:"],
            169:["по мысли автора"],
            170:["По мысли автора"],
            171:["по оценке автора","по словам автора"],
            172:["автор называет верным","по словам автора"],
            177:["по словам автора"],
            186:["Поэт сказал:"],
            197:["по мнению автора"],
        }
        for n,phrases in needles.items():
            for phrase in phrases:
                with self.subTest(page=n, phrase=phrase):
                    self.assertNotIn(phrase,self.page(n)["ru"])

    def test_quran_57_16_recovered(self):
        ar=self.page(193)["ar"]
        self.assertIn("أَلَمْ يَأْنِ لِلَّذِينَ آمَنُوا",ar)
        self.assertIn("وَمَا نَزَلَ مِنَ الْحَقِّ",ar)
        self.assertIn("وَكَثِيرٌ مِنْهُمْ فَاسِقُونَ} [الحديد: ١٦]",ar)
        self.assertNotIn("مِنَ نَزَّلَ",ar)

    def test_quran_55_46_79_40_recovered(self):
        ar=self.page(197)["ar"]
        self.assertIn("وَلِمَنْ خَافَ مَقَامَ رَبِّهِ جَنَّتَانِ} [الرحمن: ٤٦]",ar)
        self.assertIn("وَأَمَّا مَنْ خَافَ مَقَامَ رَبِّهِ وَنَهَى النَّفْسَ عَنِ الْهَوَى} [النازعات: ٤٠]",ar)
        self.assertNotIn("{(٤٥)",ar)
        self.assertNotIn("{(٣٩)",ar)

    def test_hadith_author_classification_not_erased(self):
        ru=self.page(171)["ru"]
        self.assertIn("недостоверн",ru)
        self.assertIn("неизвестные",ru)
        self.assertIn("не имеет известной цепочки передачи",ru)

if __name__=="__main__":unittest.main()
