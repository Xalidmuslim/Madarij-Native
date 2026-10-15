#!/usr/bin/env python3
"""Regression coverage for electronic Arabic/Russian collation, pages 31–40.

Caution: these checks do not establish print-scan or scholarly certification.
"""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

class Volume2Pages031040(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.book = json.loads((ROOT / "app/src/main/assets/volume2-preview.json").read_text(encoding="utf-8"))
        cls.paras = {p["id"]: p for chapter in cls.book["chapters"] for p in chapter["paragraphs"]}

    def test_p34_full_extent_of_author_phrase(self):
        self.assertIn("величайшей радостью, какую только можно себе представить",
                      self.paras["madarij-v2-p034-draft"]["ru"])

    def test_p36_poetry_without_added_speaker(self):
        ru = self.paras["madarij-v2-p036-draft"]["ru"]
        self.assertIn("Порой то, что душе ненавистно", ru)
        self.assertNotIn("Поэт сказал:", ru)

    def test_p38_16_97_quran_and_numbering_restored(self):
        ar = self.paras["madarij-v2-p038-draft"]["ar"]
        self.assertIn("مَا كَانُوا يَعْمَلُونَ} [النحل: ٩٧]", ar)
        self.assertNotIn("{يَعْمَلُونَ (٩٦)", ar)
        self.assertNotIn("{(٩) قُلْ", ar)
        self.assertIn("{قُلْ يَاعِبَادِ الَّذِينَ آمَنُوا", ar)

    def test_p39_no_interposed_modern_author_labels(self):
        ru = self.paras["madarij-v2-p039-draft"]["ru"]
        self.assertNotIn("по мнению автора", ru)
        self.assertNotIn("по описанию автора", ru)
        self.assertIn("правильнее понимать её как относящуюся", ru)

    def test_original_material_and_previous_changes_preserved(self):
        self.assertEqual(len(self.book["chapters"]), 9)
        self.assertEqual(len(self.paras), 676)
        self.assertFalse(self.book["publication_ready"])
        self.assertFalse(self.book["translation_independently_verified"])
        self.assertIn("рухнул бы его престол", self.paras["madarij-v2-p026-draft"]["ru"])
        self.assertIn("Если пожелает, Он успокоит ветер", self.paras["madarij-v2-p448-draft"]["ru"])
        self.assertIn("إِنَّ اللَّهَ مَعَ الصَّابِرِينَ", self.paras["madarij-v2-p622-draft"]["ar"])
        self.assertIn("Ибн Аббас", self.paras["madarij-v2-p040-draft"]["ru"])

if __name__ == "__main__":
    unittest.main()
