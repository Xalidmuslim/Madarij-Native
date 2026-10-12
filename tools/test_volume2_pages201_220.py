#!/usr/bin/env python3
"""Regression checks for second-volume electronic collation p.201–220."""
import json
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
class Pages201220(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.book=json.loads((ROOT/"app/src/main/assets/volume2-preview.json").read_text(encoding="utf-8"))
        cls.ps={p["id"]:p for c in cls.book["chapters"] for p in c["paragraphs"]}
    def p(self,n):return self.ps[f"madarij-v2-p{n:03d}-draft"]
    def test_scholarship_exact_voice(self):
        self.assertNotIn("По мнению автора",self.p(205)["ru"])
        self.assertNotIn("автор считает более правильным",self.p(208)["ru"])
        self.assertNotIn("по словам автора",self.p(215)["ru"])
        self.assertIn("все наставники духовного пути",self.p(215)["ru"])
    def test_quran_16_96_10_24_p218(self):
        ar=self.p(218)["ar"]
        self.assertIn("وَمَا عِنْدَ اللَّهِ بَاقٍ} [النحل: ٩٦]",ar)
        self.assertIn("لِقَوْمٍ يَتَفَكَّرُونَ} [يونس: ٢٤]",ar)
        self.assertNotIn("{(٩٥)",ar)
        self.assertNotIn("{(٢٣)",ar)
    def test_quran_87_16_17_p219(self):
        ar=self.p(219)["ar"]
        self.assertIn("بَلْ تُؤْثِرُونَ الْحَيَاةَ الدُّنْيَا",ar)
        self.assertIn("وَالْآخِرَةُ خَيْرٌ وَأَبْقَى} [الأعلى: ١٦ - ١٧]",ar)
        self.assertNotIn("{(١٥)",ar)
    def test_translator_notes_separated(self):
        for page in (202,218,219):
            self.assertIn("Примечание переводчика",self.p(page)["ru"])
    def test_no_draft_approval_and_previous_repairs(self):
        self.assertEqual(len(self.book["chapters"]),9)
        self.assertEqual(len(self.ps),676)
        self.assertFalse(self.book["publication_ready"])
        self.assertFalse(self.book["translation_independently_verified"])
        self.assertIn("وَمَا نَزَلَ مِنَ الْحَقِّ",self.p(193)["ar"])
        self.assertIn("إِنَّ اللَّهَ مَعَ الصَّابِرِينَ",self.p(622)["ar"])
if __name__=="__main__":unittest.main()
