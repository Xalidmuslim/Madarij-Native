#!/usr/bin/env python3
"""Source-fidelity regression checks for selected high-risk earlier printed pages.

The electronic-screening and regression checks do not independently verify
the 2019 printed page images, commentary apparatus, or hadith authenticity.
"""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BOOK = json.loads((ROOT / "app/src/main/assets/volume2-preview.json").read_text(encoding="utf-8"))

class EarlierPages(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.paragraphs = {p["id"]: p for ch in BOOK["chapters"] for p in ch["paragraphs"]}

    def page(self, n):
        return self.paragraphs[f"madarij-v2-p{n}-draft"]["ru"]

    def test_407_all_quran_arguments_and_two_origins_of_reliance(self):
        self.assertIn("65:5", self.page(407))
        self.assertIn("65:2", self.page(407))
        self.assertIn("65:4", self.page(407))
        self.assertIn("65:3", self.page(407))
        self.assertIn("двух знаний", self.page(407))

    def test_478_prophet_obedience_includes_authoritative_judgment(self):
        self.assertIn("не обращается за решением к иному", self.page(478))
        self.assertIn("таяммума", self.page(479))

    def test_479_original_hadith_analogy_and_authors_words(self):
        self.assertIn("падали и крови", self.page(479))
        self.assertIn("умножь мою чуждость", self.page(479))

    def test_480_continuation_not_double_recap(self):
        self.assertIn("ни силы, ни помощника", self.page(480))

    def test_516_angel_and_examples_in_authorial_sequence(self):
        self.assertIn("народом Самуд", self.page(516))
        self.assertIn("народом Лута", self.page(516))
        self.assertIn("26", self.page(516))

    def test_517_previous_sentence_continues(self):
        self.assertTrue(self.page(517).startswith("причины"))

    def test_529_keep_19th_point_on_own_page(self):
        self.assertIn("Девятнадцатое", self.page(529))
        self.assertIn("недуг", self.page(529))
        self.assertNotIn("поддерживают друг друга", self.page(529))

    def test_publication_flags_remain_off(self):
        self.assertFalse(BOOK["publication_ready"])
        self.assertFalse(BOOK["translation_independently_verified"])
        self.assertEqual(len(BOOK["chapters"]), 9)
        self.assertEqual(sum(len(ch["paragraphs"]) for ch in BOOK["chapters"]), 676)

if __name__ == "__main__":
    unittest.main()
