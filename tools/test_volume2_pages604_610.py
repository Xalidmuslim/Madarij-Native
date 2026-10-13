#!/usr/bin/env python3
"""Source-fidelity regression tests for volume II, printed pages 604–610.
Tests do not certify the printed 2019 scan or religious source authenticity.
"""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BOOK = ROOT / "app/src/main/assets/volume2-preview.json"
DRAFT = ROOT / "research/volume2-batch-521-620/russian-pages-601-610.json"

class Volume2Pages604610(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.book = json.loads(BOOK.read_text(encoding="utf-8"))
        cls.paragraphs = {
            p["id"]: p for ch in cls.book["chapters"] for p in ch["paragraphs"]
        }
        cls.draft = {
            p["printed_page"]: p
            for p in json.loads(DRAFT.read_text(encoding="utf-8"))["pages"]
        }

    def page(self, n):
        return self.paragraphs[f"madarij-v2-p{n:03d}-draft"]["ru"]

    def test_p604_preserves_source_not_interpolated_psychology(self):
        self.assertIn("не любит неприятное", self.page(604))
        self.assertNotIn("настолько доверяет Господу", self.page(604))

    def test_p605_preserves_servitude_and_rank(self):
        self.assertIn("предпочтение тому, что угодно его Господу", self.page(605))
        self.assertIn("достоинство и близость", self.page(605))

    def test_p606_retains_whipping_story_setup(self):
        self.assertIn("били плетьми", self.page(606))
        self.assertIn("на последнем ударе", self.page(606))
        self.assertNotIn("ясно видит смысл испытания", self.page(606))

    def test_p608_returns_with_snow_on_printed_page(self):
        self.assertIn("вернулся со снегом", self.page(608))
        self.assertNotIn("Кто же проявляет более верное служение?", self.page(608))

    def test_p609_has_second_story_beginning(self):
        self.assertIn("два человека", self.page(609))
        self.assertIn("что вы хотите", self.page(609).lower())
        self.assertNotIn("Тем самым автор показывает", self.page(609))

    def test_p610_returns_to_ended_arabic_sentence(self):
        self.assertIn("любовью, порождённой болезнью души", self.page(610))
        self.assertIn("суфии поклоняются своим душам", self.page(610))
        self.assertNotIn("некоторые суфии", self.page(610))
        self.assertNotIn("Под этим подразумевалось не название всякого суфия", self.page(610))

    def test_drafts_synchronized_and_not_certified(self):
        for n in (604, 605, 606, 608, 609, 610):
            with self.subTest(page=n):
                self.assertEqual(self.page(n), self.draft[n]["ru"])
                self.assertFalse(self.draft[n]["publisher_ready"])
        self.assertEqual(len(self.book["chapters"]), 9)
        self.assertEqual(len(self.paragraphs), 676)
        self.assertFalse(self.book["publication_ready"])

if __name__ == "__main__":
    unittest.main()
