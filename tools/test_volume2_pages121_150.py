#!/usr/bin/env python3
"""Regression checks for selected verified corrections, pages 121–150.

Text/citation repairs only; print scans and independent hadith criticism pending.
"""
import json
import unittest
from pathlib import Path

ASSET=Path(__file__).resolve().parents[1]/"app/src/main/assets/volume2-preview.json"

class Volume2Pages121150(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.book=json.loads(ASSET.read_text(encoding="utf-8"))
        cls.p={int(p["id"].split("-p")[1][:3]):p for c in cls.book["chapters"] for p in c["paragraphs"] if p["id"].endswith("-draft")}

    def test_131_verse_5_83_corrected(self):
        x=self.p[131]["ar"]
        self.assertIn("{وَإِذَا سَمِعُوا مَا أُنْزِلَ إِلَى الرَّسُولِ",x)
        self.assertNotIn("{(٨٢) سَمِعُوا",x)

    def test_131_134_verse_8_23_complete(self):
        for n in (131,134):
            with self.subTest(n=n):
                x=self.p[n]["ar"]
                self.assertIn("وَهُمْ مُعْرِضُونَ} [الأنفال: ٢٣]",x)
                self.assertNotIn("{(٢٢) وَلَوْ",x)

    def test_131_verse_41_26_complete(self):
        self.assertIn("لَعَلَّكُمْ تَغْلِبُونَ} [فصلت: ٢٦]",self.p[131]["ar"])

    def test_134_jinn_72_1_2_reference(self):
        x=self.p[134]["ar"]
        self.assertIn("[الجن: ١ - ٢]",x)

    def test_editorial_insertions_not_in_author_text(self):
        forbidden={138:["по мнению автора"],140:["по оценке автора","По мнению автора"],143:["Автор считает"],147:["по утверждению автора"],148:["По словам автора","возражает автор"],149:["по утверждению автора"],150:["По оценке автора","Автор отвергает","автор считает","По мнению автора"]}
        for n,terms in forbidden.items():
            for term in terms:
                with self.subTest(n=n,term=term):
                    self.assertNotIn(term,self.p[n]["ru"])

    def test_invariants(self):
        self.assertFalse(self.book["publication_ready"])
        self.assertFalse(self.book["translation_independently_verified"])
        self.assertEqual(len(self.book["chapters"]),9)
        self.assertEqual(sum(len(c["paragraphs"]) for c in self.book["chapters"]),676)
        self.assertIn("إِنَّ اللَّهَ مَعَ الصَّابِرِينَ",self.p[622]["ar"])

if __name__=="__main__":
    unittest.main()
