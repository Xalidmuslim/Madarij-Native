#!/usr/bin/env python3
"""Regression checks for concrete Arabic/Russian restorations, volume II pages 501–550.

This is not a print-scan certification or an authentication of historical reports.
"""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSET = ROOT / "app/src/main/assets/volume2-preview.json"

class Volume2Pages501550(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.book = json.loads(ASSET.read_text(encoding="utf-8"))
        cls.fragments = {p["id"]: p for c in cls.book["chapters"] for p in c["paragraphs"]}

    def page(self, n):
        return self.fragments[f"madarij-v2-p{n:03d}-draft"]

    def test_quran_98_7_8_restored(self):
        ar = self.page(501)["ar"]
        self.assertIn("إِنَّ الَّذِينَ آمَنُوا وَعَمِلُوا الصَّالِحَاتِ أُولَئِكَ هُمْ خَيْرُ الْبَرِيَّةِ", ar)
        self.assertIn("جَزَاؤُهُمْ عِنْدَ رَبِّهِمْ جَنَّاتُ عَدْنٍ", ar)
        self.assertNotIn("{(٦) إِنَّ الَّذِينَ", ar)

    def test_quran_35_6_has_correct_verse_boundary(self):
        ar = self.page(515)["ar"]
        self.assertIn("{إِنَّ الشَّيْطَانَ لَكُمْ عَدُوٌّ فَاتَّخِذُوهُ عَدُوًّا}", ar)
        self.assertNotIn("{(٥) إِنَّ الشَّيْطَانَ", ar)

    def test_no_shortened_theological_argument(self):
        p504 = self.page(504)["ru"]
        self.assertIn("вторые противоречили Его довольству и недовольству", p504)
        self.assertNotIn("по оценке автора", p504)
        p506 = self.page(506)["ru"]
        self.assertIn("одно из мнений аль-Ашари", p506)
        self.assertIn("большинство его последователей", p506)
        self.assertIn("Кади Абу Бакр", p506)
        self.assertIn("если оно является существующим", p506)

    def test_original_similes_and_contrast_kept(self):
        ru=self.page(511)["ru"]
        self.assertIn("отсечь поражённую гниением часть тела",ru)
        self.assertIn("Джибриля",ru)
        self.assertNotIn("по объяснению автора",ru)

    def test_direct_question_and_answer_page_537(self):
        ru=self.page(537)["ru"]
        self.assertIn("Почему Аллах не наделил его содействием",ru)
        self.assertIn("Почему же Он не сделал всех творений одинаковыми",ru)
        self.assertNotIn("по объяснению автора",ru)
        self.assertNotIn("по мысли автора",ru)

    def test_author_and_athar_wording_page_540(self):
        ru=self.page(540)["ru"]
        self.assertIn("даром, даже если он выглядит лишением",ru)
        self.assertIn("Он не лишил тебя иначе как для того, чтобы одарить",ru)
        self.assertNotIn("по словам автора",ru)
        self.assertNotIn("Это образные слова духовных наставников",ru)

    def test_rida_argument_unabridged_page_545(self):
        ru=self.page(545)["ru"]
        self.assertIn("заменяет многие виды поклонения",ru)
        self.assertIn("не пожелала бы оказаться в тени",ru)
        self.assertNotIn("Автор считает",ru)
        self.assertNotIn("Довольство не отменяет обязательных дел",ru)

    def test_unverified_status_unchanged(self):
        self.assertEqual(len(self.book["chapters"]),9)
        self.assertEqual(len(self.fragments),676)
        self.assertFalse(self.book["publication_ready"])
        self.assertFalse(self.book["translation_independently_verified"])
        for n in (448,463,507,622):
            self.assertIn(f"madarij-v2-p{n:03d}-draft",self.fragments)

if __name__ == "__main__":
    unittest.main()
