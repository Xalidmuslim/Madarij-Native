#!/usr/bin/env python3
"""Source-text fidelity checks for volume-II printed pages 81–100.

These tests do not imply full hadith authentication, review of 2019 scans, or publication readiness.
"""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BOOK = json.loads((ROOT / "app/src/main/assets/volume2-preview.json").read_text(encoding="utf-8"))
PARAGRAPHS = {p["id"]:p for ch in BOOK["chapters"] for p in ch["paragraphs"]}

class VolumeTwoPrinted81To100(unittest.TestCase):
    @staticmethod
    def page(n):
        return PARAGRAPHS[f"madarij-v2-p{n:03d}-draft"]["ru"]

    def test_no_hadith_grading_gloss_inside_unmarked_author_text(self):
        forbidden = {
            82: "Передача требует проверки по первоисточнику.",
            83: "Достоверность и точную формулировку передачи необходимо проверить.",
            93: "Точную формулировку хадиса следует проверить по первоисточнику.",
            95: "Источник и оценку передачи следует проверить отдельно.",
            96: "Это передаётся как рассказ; его происхождение требует проверки.",
            100: "Достоверность этого сообщения и точность формулировки требуют отдельной проверки.",
        }
        for n, phrase in forbidden.items():
            with self.subTest(n=n):
                self.assertNotIn(phrase, self.page(n))

    def test_research_drafts_synchronized_with_app(self):
        for boundary in ("081-090", "091-100"):
            draft = json.loads((ROOT / f"research/volume2-batch-81-150/russian-pages-{boundary}.json").read_text(encoding="utf-8"))
            for record in draft["pages"]:
                n=record["printed_page"]
                self.assertEqual(record["ru"], self.page(n), f"Page {n}")
                self.assertFalse(record["publisher_ready"])

    def test_source_point_and_safety_flags(self):
        self.assertIn("вервь Аллаха", self.page(100))
        self.assertIn("посредством", self.page(99))
        self.assertFalse(BOOK["publication_ready"])
        self.assertFalse(BOOK["translation_independently_verified"])
        self.assertEqual(len(BOOK["chapters"]),9)
        self.assertEqual(sum(len(c["paragraphs"]) for c in BOOK["chapters"]),676)

if __name__ == "__main__":
    unittest.main()
