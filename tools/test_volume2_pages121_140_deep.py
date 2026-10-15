#!/usr/bin/env python3
"""Source-fidelity regression checks, printed volume II pp121-140."""
import json
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
BOOK=json.loads((ROOT/"app/src/main/assets/volume2-preview.json").read_text(encoding="utf-8"))
PAGES={p["id"]:p for ch in BOOK["chapters"] for p in ch["paragraphs"]}

class Volume2Pages121to140(unittest.TestCase):
    def page(self,n):
        return PAGES[f"madarij-v2-p{n:03d}-draft"]["ru"]

    def test_literal_theological_formula_on_123(self):
        self.assertIn("посредством Него, от Него и к Нему",self.page(123))
        self.assertNotIn("от Его кары к Его милости",self.page(123))

    def test_shirk_debate_129_without_inferred_divine_satisfaction(self):
        self.assertIn("близости к Нему",self.page(129))

    def test_139_warn_on_hadith_in_audit_not_as_ibn_alqayyim(self):
        self.assertNotIn("его нельзя выдавать за подтверждённый хадис",self.page(139))
        self.assertNotIn("Достоверность и исходная форма",self.page(139))

    def test_corresponding_research_files_match(self):
        for suffix in ("121-130","131-140"):
            data=json.loads((ROOT/f"research/volume2-batch-81-150/russian-pages-{suffix}.json").read_text(encoding="utf-8"))
            for x in data["pages"]:
                n=x["printed_page"]
                self.assertEqual(x["ru"],self.page(n))
                self.assertFalse(x["publisher_ready"])

    def test_ready_flags(self):
        self.assertEqual(len(BOOK["chapters"]),9)
        self.assertEqual(sum(len(ch["paragraphs"]) for ch in BOOK["chapters"]),676)
        self.assertFalse(BOOK["publication_ready"])
        self.assertFalse(BOOK["translation_independently_verified"])

if __name__=="__main__":
    unittest.main()
