#!/usr/bin/env python3
"""Structural completeness check for printed volume II, pages 3–659.

Ensures no missing page mapping and no empty content. A structural check is NOT
a substitute for independent philological, hadith, or original-print review.
"""
import json
import re
import unittest
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BOOK = json.loads((ROOT / "app/src/main/assets/volume2-preview.json").read_text(encoding="utf-8"))

class Volume2Coverage(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.paragraphs = [p for c in BOOK["chapters"] for p in c["paragraphs"]]

    def test_printed_pages_3_through_659_all_represented(self):
        counts = Counter()
        for p in self.paragraphs:
            m = re.match(r"^madarij-v2-p(\d{3})(?:-p(\d{3}))?", p["id"])
            self.assertIsNotNone(m, p["id"])
            first = int(m.group(1))
            last = int(m.group(2)) if m.group(2) else first
            self.assertLessEqual(first, last)
            counts.update(range(first, last + 1))
        self.assertEqual(set(counts), set(range(3,660)))
        self.assertTrue(all(counts[p] >= 1 for p in range(3,660)))

    def test_no_empty_author_text_or_translation(self):
        for p in self.paragraphs:
            with self.subTest(id=p["id"]):
                self.assertTrue(p["ar"].strip())
                self.assertTrue(p["ru"].strip())

    def test_publication_not_certified(self):
        self.assertFalse(BOOK["publication_ready"])
        self.assertFalse(BOOK["translation_independently_verified"])
        self.assertEqual(len(BOOK["chapters"]), 9)
        self.assertEqual(len(self.paragraphs), 676)

if __name__ == "__main__":
    unittest.main()
