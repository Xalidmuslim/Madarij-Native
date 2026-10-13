#!/usr/bin/env python3
"""Targeted electronic-text regression checks, not publication/print certification."""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BOOK = ROOT / "app/src/main/assets/volume2-preview.json"
DRAFT = ROOT / "research/volume2-batch-521-620/russian-pages-601-610.json"

class Volume2Pages601602(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.book = json.loads(BOOK.read_text(encoding="utf-8"))
        cls.draft = json.loads(DRAFT.read_text(encoding="utf-8"))
        cls.pars = {p["id"]: p for c in cls.book["chapters"] for p in c["paragraphs"]}
        cls.draft_pages = {p["printed_page"]: p for p in cls.draft["pages"]}

    def test_quran_27_40_original_word_order(self):
        arabic = self.pars["madarij-v2-p601-draft"]["ar"]
        self.assertIn("{وَمَنْ شَكَرَ فَإِنَّمَا يَشْكُرُ لِنَفْسِهِ} [النمل: ٤٠]", arabic)
        self.assertNotIn("{شَكَرَ فَإِنَّمَا يَشْكُرُ لِنَفْسِهِ وَمَنْ}", arabic)

    def test_page_601_and_602_authorial_phrases(self):
        a = self.pars["madarij-v2-p601-draft"]["ru"]
        b = self.pars["madarij-v2-p602-draft"]["ru"]
        self.assertIn("взаимному обмену и взаимопомощи", a)
        self.assertNotIn("чрезмерно благодарит", a)
        self.assertIn("не следует порицать", a)
        self.assertIn("восхваляет его", b)
        self.assertIn("«первого различения»", b)
        self.assertIn("пусть каждый говорит что пожелает", b)

    def test_app_draft_synced_preserve_unpublished(self):
        self.assertEqual(len(self.book["chapters"]), 9)
        self.assertEqual(len(self.pars), 676)
        self.assertFalse(self.book["publication_ready"])
        self.assertFalse(self.book["translation_independently_verified"])
        for p in (601, 602):
            self.assertEqual(self.pars[f"madarij-v2-p{p:03d}-draft"]["ru"], self.draft_pages[p]["ru"])

if __name__ == "__main__":
    unittest.main()
