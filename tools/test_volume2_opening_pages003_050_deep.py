#!/usr/bin/env python3
"""Guards for the first printed pages of Madarij volume II.

Validates fidelity to the electronic Arabic text and printed-page boundaries.
Does not certify the edition's print images, footnotes or hadith grading.
"""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BOOK = json.loads((ROOT / "app/src/main/assets/volume2-preview.json").read_text(encoding="utf-8"))
DRAFT_31_40 = json.loads((ROOT / "research/volume2-batch-11-80/russian-pages-031-040.json").read_text(encoding="utf-8"))
PARAS = {p["id"]: p for ch in BOOK["chapters"] for p in ch["paragraphs"]}

class OpeningPrintedPages(unittest.TestCase):
    def test_first_ten_printed_pages_are_not_lost(self):
        for number in range(3, 11):
            self.assertTrue(any(
                f"p{number:03d}" in p["id"]
                for p in PARAS.values()
            ), f"missing printed page {number}")

    def test_bear_metaphor_uses_actual_arabic_quality(self):
        self.assertIn("тупого и скверного", PARAS["madarij-v2-p010-c"]["ru"])

    def test_quran_16_30_quotation_break_is_accurate(self):
        p38 = PARAS["madarij-v2-p038-draft"]["ru"]
        p39 = PARAS["madarij-v2-p039-draft"]["ru"]
        self.assertTrue(p38.rstrip().endswith("«Тем, кто"))
        self.assertTrue(p39.startswith("творил добро в этом мире"))
        self.assertNotIn("творил добро…»", p38)
        self.assertIn("16:30", p39)

    def test_every_31_to_40_draft_synced_with_app(self):
        for page in DRAFT_31_40["pages"]:
            n=page["printed_page"]
            self.assertEqual(page["ru"], PARAS[f"madarij-v2-p{n:03d}-draft"]["ru"])
            self.assertFalse(page["publisher_ready"])

    def test_scope_and_readiness(self):
        self.assertEqual(len(BOOK["chapters"]), 9)
        self.assertEqual(sum(len(c["paragraphs"]) for c in BOOK["chapters"]), 676)
        self.assertFalse(BOOK["publication_ready"])
        self.assertFalse(BOOK["translation_independently_verified"])

if __name__ == "__main__":
    unittest.main()
