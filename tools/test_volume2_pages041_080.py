#!/usr/bin/env python3
"""Source-fidelity regression checks for printed volume-2 pages 41–80.

Requires electronically compared Arabic source; these tests do not certify the
2019 print scans or independently authenticate hadith.
"""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSET = ROOT / "app/src/main/assets/volume2-preview.json"

class Vol2Pages041080(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.book=json.loads(ASSET.read_text(encoding="utf-8"))
        cls.para={p["id"]:p for c in cls.book["chapters"] for p in c["paragraphs"]}

    def get(self, page):
        return self.para[f"madarij-v2-p{page:03d}-draft"]

    def test_remove_interpolated_author_narration(self):
        for n, terms in {
            41:["согласно объяснению автора","По мысли автора","по утверждению автора"],
            42:["по словам автора"],
            43:["автор рассматривает"],
            45:["по сравнению автора"],
            46:["По словам автора"],
            48:["по словам автора"],
            50:["Поэт сказал:"],
            75:["Автор считает такое истолкование","По его словам, оно несостоятельно"],
            79:["по мнению автора"]
        }.items():
            with self.subTest(page=n):
                for term in terms:
                    self.assertNotIn(term,self.get(n)["ru"])

    def test_quran_40_13_complete_on_p55_and_p68(self):
        q1=self.get(55)["ar"]
        self.assertIn("هُوَ الَّذِي يُرِيكُمْ آيَاتِهِ وَيُنَزِّلُ",q1)
        self.assertIn("إِلَّا مَنْ يُنِيبُ} [غافر: ١٣]",q1)
        self.assertNotIn("{(١٢) هُوَ",q1)
        self.assertIn("{وَمَا يَتَذَكَّرُ إِلَّا مَنْ يُنِيبُ} [غافر: ١٣]",self.get(68)["ar"])

    def test_no_previous_verse_number_inside_2_160_p57(self):
        ar=self.get(57)["ar"]
        self.assertIn("{إِلَّا الَّذِينَ تَابُوا} [البقرة: ١٦٠]",ar)
        self.assertNotIn("{(١٥٩) إِلَّا",ar)

    def test_79_42_45_restored_and_not_corrupted(self):
        ar=self.get(77)["ar"]
        self.assertIn("مُنْذِرُ مَنْ يَخْشَاهَا} [النازعات: ٤٢ - ٤٥]",ar)
        self.assertNotIn("{(٤١) يَسْأَلُونَكَ",ar)

    def test_unverified_long_report_separately_marked(self):
        p53=self.get(53)["ru"]
        p54=self.get(54)["ru"]
        self.assertNotIn("Прежде чем использовать его как пророческий хадис",p53)
        self.assertIn("**Примечание переводчика (источник и достоверность):**",p54)
        self.assertIn("не установлено как достоверный",p54)

    def test_p78_40_repetitions_are_not_prophetic_instruction(self):
        t=self.get(78)["ru"]
        self.assertNotIn("автор упоминает такой",t)
        self.assertIn("**Примечание переводчика:**",t)
        self.assertIn("не является установленной сунной",t)

    def test_previous_pages_untouched_and_unpublished(self):
        self.assertEqual(len(self.book["chapters"]),9)
        self.assertEqual(len(self.para),676)
        self.assertFalse(self.book["publication_ready"])
        self.assertFalse(self.book["translation_independently_verified"])
        p=self.para
        self.assertIn("Если пожелает, Он успокоит ветер",p["madarij-v2-p448-draft"]["ru"])
        self.assertIn("эта мысль является одним из порицаемых утверждений",p["madarij-v2-p463-draft"]["ru"])
        self.assertIn("пока не вкусили Нашего наказания",p["madarij-v2-p507-draft"]["ru"])
        self.assertIn("إِنَّ اللَّهَ مَعَ الصَّابِرِينَ",p["madarij-v2-p622-draft"]["ar"])

if __name__ == "__main__":
    unittest.main()
