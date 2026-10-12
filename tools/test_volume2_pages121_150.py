#!/usr/bin/env python3
"""Regression assertions for volume 2, printed pages 121–150.

Based on electronic Arabic/Russian collation; does not certify print scans,
individual hadith attributions or theological correctness.
"""
import json
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
BOOK=ROOT/"app/src/main/assets/volume2-preview.json"

class Volume2Pages121150(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.book=json.loads(BOOK.read_text(encoding="utf-8"))
        cls.p={p["id"]:p for c in cls.book["chapters"] for p in c["paragraphs"]}
    def page(self,n):
        return self.p[f"madarij-v2-p{n:03d}-draft"]

    def test_quran_8_23_and_5_83_page131(self):
        t=self.page(131)["ar"]
        self.assertIn("{وَإِذَا سَمِعُوا مَا أُنْزِلَ",t)
        self.assertIn("لَتَوَلَّوْا وَهُمْ مُعْرِضُونَ} [الأنفال: ٢٣]",t)
        self.assertNotIn("{(٨٢) سَمِعُوا",t)
        self.assertNotIn("{(٢٢) وَلَوْ",t)

    def test_quran_41_26_page131(self):
        t=self.page(131)["ar"]
        self.assertIn("لَعَلَّكُمْ تَغْلِبُونَ} [فصلت: ٢٦]",t)

    def test_quran_72_1_2_and_8_23_page134(self):
        t=self.page(134)["ar"]
        self.assertIn("فَآمَنَّا بِهِ} [الجن: ١ - ٢]",t)
        self.assertIn("لَتَوَلَّوْا وَهُمْ مُعْرِضُونَ} [الأنفال: ٢٣]",t)
        self.assertNotIn("{(٢٢) وَلَوْ",t)

    def test_no_added_author_narration(self):
        terms={
            138:["по мнению автора"],
            139:["Поэт сказал:"],
            140:["по оценке автора","Он считает, что","По мнению автора","пишет автор"],
            143:["Автор считает, что"],
            147:["по утверждению автора"],
            148:["По словам автора","возражает автор"],
            149:["по утверждению автора"],
            150:["По оценке автора","Автор отвергает","автор считает, что","По мнению автора","по его выражению"]
        }
        for n,bad in terms.items():
            with self.subTest(page=n):
                for phrase in bad:
                    self.assertNotIn(phrase,self.page(n)["ru"])

    def test_preserve_status_and_previous_repairs(self):
        self.assertEqual(len(self.book["chapters"]),9)
        self.assertEqual(len(self.p),676)
        self.assertFalse(self.book["publication_ready"])
        self.assertFalse(self.book["translation_independently_verified"])
        self.assertIn("Если пожелает, Он успокоит ветер",self.page(448)["ru"])
        self.assertIn("эта мысль является одним из порицаемых утверждений",self.page(463)["ru"])
        self.assertIn("пока не вкусили Нашего наказания",self.page(507)["ru"])
        self.assertIn("إِنَّ اللَّهَ مَعَ الصَّابِرِينَ",self.page(622)["ar"])

if __name__=="__main__":
    unittest.main()
