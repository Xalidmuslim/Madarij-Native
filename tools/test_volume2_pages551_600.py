#!/usr/bin/env python3
"""Regression tests for selected volume-II p551-600 electronic collation repairs."""
import json
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
class VolumeTwo551600(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.book=json.loads((ROOT/"app/src/main/assets/volume2-preview.json").read_text(encoding="utf-8"))
        cls.paras={p["id"]:p for c in cls.book["chapters"] for p in c["paragraphs"]}
    def para(self,n): return self.paras[f"madarij-v2-p{n:03d}-draft"]
    def test_quran_16_78(self):
        ar=self.para(587)["ar"]
        self.assertIn("لَعَلَّكُمْ تَشْكُرُونَ} [النحل: ٧٨]",ar)
        self.assertNotIn("{قَدِيرٌ (٧٧)",ar)
    def test_quran_2_151_152(self):
        ar=self.para(587)["ar"]
        self.assertIn("وَلَا تَكْفُرُونِ} [البقرة: ١٥١ - ١٥٢]",ar)
        self.assertNotIn("{(١٥٠) كَمَا",ar)
        self.assertIn("وَلَا تَكْفُرُونِ} [البقرة: ١٥٢]",self.para(586)["ar"])
    def test_direct_critical_author_voice(self):
        for n,bad in {558:["По мысли автора"],565:["Автор считает"],569:["Автор говорит"],577:["по мнению автора"],586:["по словам автора"],590:["Автор предлагает","Автор считает"],597:["по мнению автора","Автор возражает"],600:["автор считает"]}.items():
            for term in bad:
                with self.subTest(page=n,term=term): self.assertNotIn(term,self.para(n)["ru"])
    def test_full_reasoning_restore(self):
        self.assertIn("не довольствуется одним только отсутствием гнева",self.para(558)["ru"])
        self.assertIn("не иначе как относительно прав Аллаха",self.para(565)["ru"])
        self.assertIn("собственную честь и достоинство",self.para(569)["ru"])
        self.assertIn("различать два значения",self.para(577)["ru"])
        self.assertIn("половина веры",self.para(586)["ru"])
        self.assertIn("второе толкование совершеннее",self.para(590)["ru"])
        self.assertIn("О если бы шейх уберёг свою книгу",self.para(597)["ru"])
        self.assertIn("привёл их к поклонению солнцу, луне и идолам",self.para(600)["ru"])
    def test_preserve_draft_and_previous_pages(self):
        self.assertEqual(len(self.book["chapters"]),9)
        self.assertEqual(len(self.paras),676)
        self.assertFalse(self.book["publication_ready"])
        self.assertFalse(self.book["translation_independently_verified"])
        self.assertIn("جَزَاؤُهُمْ عِنْدَ رَبِّهِمْ",self.para(501)["ar"])
        self.assertIn("Если пожелает, Он успокоит ветер",self.para(448)["ru"])
if __name__=="__main__": unittest.main()
