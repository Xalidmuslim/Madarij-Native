#!/usr/bin/env python3
"""Opening volume-II continuation fidelity: printed pages 51–80.

This checks the electronic edition's source page boundaries and annotations;
it does not authenticate hadiths or certify scan-level print collation.
"""
import json
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
BOOK=json.loads((ROOT/"app/src/main/assets/volume2-preview.json").read_text(encoding="utf-8"))
PAGES={p["id"]:p for chapter in BOOK["chapters"] for p in chapter["paragraphs"]}

class ContinuationChecks(unittest.TestCase):
    def page(self,n):
        return PAGES[f"madarij-v2-p{n:03d}-draft"]["ru"]

    def test_hadith_53_54_not_quoted_beyond_print_boundary(self):
        self.assertTrue(self.page(53).endswith("Я лучше знаю его, ибо"))
        self.assertTrue(self.page(54).startswith("Я сотворил его из земли."))
        self.assertIn("**Примечание переводчика", self.page(54))

    def test_provenance_glosses_not_in_author_paragraphs(self):
        self.assertNotIn("Формулировка сообщения и его передача", self.page(56))
        self.assertNotIn("Достоверность и формулировка этого сообщения", self.page(63))

    def test_79_80_arabic_page_cut(self):
        self.assertTrue(self.page(79).endswith("Так арабы говорят: «Такой-то"))
        self.assertTrue(self.page(80).startswith("знает дни арабов и дни людей»"))
        self.assertTrue(self.page(80).endswith("в предисловии к своей книге:"))

    def test_all_drafts_match_app_ignoring_only_page_edge_ellipsis(self):
        for range_ in ("051-060","061-070","071-080"):
            draft=json.loads((ROOT/f"research/volume2-batch-11-80/russian-pages-{range_}.json").read_text(encoding="utf-8"))
            for record in draft["pages"]:
                n=record["printed_page"]
                self.assertEqual(
                    record["ru"].removeprefix("…").removesuffix("…"),
                    self.page(n).removeprefix("…").removesuffix("…"),
                    f"page {n}",
                )
                self.assertFalse(record["publisher_ready"])

    def test_no_publisher_certification(self):
        self.assertFalse(BOOK["publication_ready"])
        self.assertFalse(BOOK["translation_independently_verified"])
        self.assertEqual(len(BOOK["chapters"]),9)
        self.assertEqual(sum(len(c["paragraphs"]) for c in BOOK["chapters"]),676)

if __name__=="__main__":
    unittest.main()
