#!/usr/bin/env python3
"""Regression checks for verified Arabic/Russian editorial repairs, pages 21–30.

These tests protect direct author voice and metaphors from reintroduction of
interpolated Russian editorial prose; they DO NOT certify the printed edition.
"""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BOOK = ROOT / "app/src/main/assets/volume2-preview.json"

class Volume2Pages021030Collation(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data = json.loads(BOOK.read_text(encoding="utf-8"))
        cls.paragraphs = {
            p["id"]: p
            for chapter in cls.data["chapters"]
            for p in chapter["paragraphs"]
        }

    def test_page_21_dominion_is_not_a_tautology(self):
        ru = self.paragraphs["madarij-v2-p021-draft"]["ru"]
        self.assertIn("Все творения подчинены Его власти.", ru)
        self.assertNotIn("всевластной властью", ru)

    def test_page_25_no_added_editorial_author_label(self):
        p = self.paragraphs["madarij-v2-p025-draft"]
        self.assertIn("ولكن أفرد بالذِّكر", p["ar"])
        self.assertIn("но он выделен для отдельного рассмотрения", p["ru"])
        self.assertNotIn("но автор выделяет его особо", p["ru"])

    def test_page_26_throne_metaphor_is_preserved(self):
        p = self.paragraphs["madarij-v2-p026-draft"]
        self.assertIn("لثُلَّ عرشُه", p["ar"])
        self.assertIn("рухнул бы его престол", p["ru"])
        self.assertNotIn("рухнули бы опоры его веры", p["ru"])

    def test_page_29_author_critique_not_modern_annotation(self):
        p = self.paragraphs["madarij-v2-p029-draft"]
        self.assertIn("وعَلِم أنّه من أبطل", p["ar"])
        self.assertNotIn("По оценке автора", p["ru"])
        self.assertIn("одно из самых ложных и худших учений", p["ru"])

    def test_preserve_previous_repairs_and_unverified_state(self):
        self.assertEqual(len(self.data["chapters"]), 9)
        self.assertEqual(len(self.paragraphs), 676)
        self.assertIn("Если пожелает, Он успокоит ветер", self.paragraphs["madarij-v2-p448-draft"]["ru"])
        self.assertIn("эта мысль является одним из порицаемых утверждений", self.paragraphs["madarij-v2-p463-draft"]["ru"])
        self.assertIn("пока не вкусили Нашего наказания", self.paragraphs["madarij-v2-p507-draft"]["ru"])
        self.assertIn("إِنَّ اللَّهَ مَعَ الصَّابِرِينَ", self.paragraphs["madarij-v2-p622-draft"]["ar"])
        self.assertFalse(self.data["translation_independently_verified"])
        self.assertFalse(self.data["publication_ready"])
        self.assertEqual(self.paragraphs["madarij-v2-p023-draft"]["status"], "draft_self_checked")

if __name__ == "__main__":
    unittest.main()
