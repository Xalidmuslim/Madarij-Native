#!/usr/bin/env python3
"""Regression checks for source quotation and cross-page fidelity on pp328–329, 444, 567–569."""
import json
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
BOOK=json.loads((ROOT/"app/src/main/assets/volume2-preview.json").read_text(encoding="utf-8"))
PARAS={p["id"]:p for c in BOOK["chapters"] for p in c["paragraphs"]}

class CitationRepairs(unittest.TestCase):
    def page(self,n):
        return PARAS[f"madarij-v2-p{n}-draft"]["ru"]

    def test_328_argument_does_not_preempt_p329(self):
        self.assertNotIn("Они также говорят, что Аллах любит",self.page(328))

    def test_329_retains_authorial_two_arguments(self):
        self.assertIn("не просит Его",self.page(329))
        self.assertIn("10:25",self.page(329))

    def test_444_surrendered_form_in_authorial_pair(self):
        self.assertIn("собственный облик",self.page(444))
        self.assertIn("орудием этого предания",self.page(444))

    def test_567_explicit_quran_refs_and_arab_poetry(self):
        self.assertIn("74:48",self.page(567))
        self.assertIn("2:123",self.page(567))
        self.assertIn("Имру аль-Кайс",self.page(567))
        self.assertNotIn("не основание отказывать нуждающемуся",self.page(567))

    def test_568_begging_conclusion_not_modernized(self):
        self.assertIn("в своей основе запрещена",self.page(568))
        self.assertNotIn("Эта оценка относится к необоснованному",self.page(568))

    def test_569_continuation_and_sources_only(self):
        self.assertNotIn("Эти тексты требуют учитывать",self.page(569))

if __name__=="__main__":
    unittest.main()
