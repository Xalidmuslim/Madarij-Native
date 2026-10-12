#!/usr/bin/env python3
"""Electronic Arabic/Russian collation regressions, volume 2, pages 201–250.

Content-only tests. No print scan / independent isnad certification implied.
"""
import json
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
BOOK=ROOT/"app/src/main/assets/volume2-preview.json"

class Pages201250(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data=json.loads(BOOK.read_text(encoding="utf8"))
        cls.p={p["id"]:p for c in cls.data["chapters"] for p in c["paragraphs"]}
    def page(self,n):
        return self.p[f"madarij-v2-p{n:03d}-draft"]
    def test_no_interpolated_author_labels(self):
        needles={
            205:["По мнению автора"],
            208:["автор считает более правильным"],
            215:["по словам автора", "почти все наставники"],
            231:["по словам автора"],
            233:["по мнению автора"],
            235:["по мнению автора", "автор считает наиболее правильным", "не универсальное эмпирическое правило"],
            248:["По мнению автора"],
            250:["Поэт сказал"],
        }
        for n, phrases in needles.items():
            for phrase in phrases:
                with self.subTest(page=n, phrase=phrase):
                    self.assertNotIn(phrase,self.page(n)["ru"])

    def test_retained_author_strength_on_p215(self):
        ru=self.page(215)["ru"]
        self.assertIn("единодушны",ru)
        self.assertIn("как придерживавшиеся истины, так и заблуждавшиеся",ru)

    def test_quran_22_34_35_restored_p209(self):
        ar=self.page(209)["ar"]
        self.assertIn("{الْمُخْبِتِينَ} [الحج: ٣٤]",ar)
        self.assertIn("مِمَّا رَزَقْنَاهُمْ يُنْفِقُونَ} [الحج: ٣٥]",ar)
        self.assertIn("لفظ {الْمُخْبِتِينَ}",ar)
        self.assertNotIn("{(٣٤)}",ar)

    def test_quran_16_96_and_10_24_restored_p218(self):
        ar=self.page(218)["ar"]
        self.assertIn("{مَا عِنْدَكُمْ يَنْفَدُ وَمَا عِنْدَ اللَّهِ بَاقٍ} [النحل: ٩٦]",ar)
        self.assertIn("نُفَصِّلُ الْآيَاتِ لِقَوْمٍ يَتَفَكَّرُونَ} [يونس: ٢٤]",ar)
        self.assertNotIn("{(٩٥)",ar)
        self.assertNotIn("{(٢٣)",ar)

    def test_quran_87_16_17_restored_p219(self):
        ar=self.page(219)["ar"]
        self.assertIn("{بَلْ تُؤْثِرُونَ الْحَيَاةَ الدُّنْيَا (١٦) وَالْآخِرَةُ خَيْرٌ وَأَبْقَى} [الأعلى: ١٦ - ١٧]",ar)
        self.assertNotIn("{(١٥)",ar)

    def test_quran_74_4_is_not_74_3_p234(self):
        ar=self.page(234)["ar"]
        self.assertIn("{وَثِيَابَكَ فَطَهِّرْ} [المدثر: ٤]",ar)
        self.assertNotIn("{فَكَبِّرْ (٣)} [المدثر: ٤]",ar)

    def test_author_verse_preserved_and_russian_not_lost(self):
        self.assertIn("Поэт сказал:",self.page(233)["ru"])
        self.assertIn("Как сказано:",self.page(250)["ru"])
        self.assertIn("Влияние сердца и души на одежду",self.page(235)["ru"])
        for n in (201,209,215,218,219,234,250):
            self.assertEqual(self.page(n)["status"],"draft_self_checked")

    def test_global_invariants(self):
        self.assertEqual(len(self.data["chapters"]),9)
        self.assertEqual(len(self.p),676)
        self.assertFalse(self.data["publication_ready"])
        self.assertFalse(self.data["translation_independently_verified"])
        for n in (448,463,507,622):
            self.assertIn(f"madarij-v2-p{n:03d}-draft",self.p)

if __name__=="__main__":
    unittest.main()
