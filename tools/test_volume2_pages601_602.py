#!/usr/bin/env python3
"""Regression guards for volume II printed pages 601–602.
Electronic collation only; not 2019 print-scan or publication certification.
"""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSET = ROOT / "app/src/main/assets/volume2-preview.json"
DRAFT = ROOT / "research/volume2-batch-521-620/russian-pages-601-610.json"


class Volume2Pages601602(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.book = json.loads(ASSET.read_text(encoding="utf-8"))
        cls.paragraphs = {
            p["id"]: p
            for chapter in cls.book["chapters"]
            for p in chapter["paragraphs"]
        }
        cls.draft = {
            p["printed_page"]: p
            for p in json.loads(DRAFT.read_text(encoding="utf-8"))["pages"]
        }

    def test_verse_27_40_has_original_word_order(self):
        arabic = self.paragraphs["madarij-v2-p601-draft"]["ar"]
        self.assertIn(
            "{وَمَنْ شَكَرَ فَإِنَّمَا يَشْكُرُ لِنَفْسِهِ} [النمل: ٤٠]",
            arabic,
        )
        self.assertNotIn("{شَكَرَ فَإِنَّمَا يَشْكُرُ لِنَفْسِهِ وَمَنْ}", arabic)

    def test_page_601_omitted_authorial_phrases_restored(self):
        ru = self.paragraphs["madarij-v2-p601-draft"]["ru"]
        self.assertIn("не приобретает могущество после унижения", ru)
        self.assertIn("не усиливается после слабости", ru)
        self.assertIn("не заслуживает порицания", ru)
        self.assertNotIn("чрезмерно благодарит царя", ru)

    def test_page_602_authorial_metaphor_retained(self):
        ru = self.paragraphs["madarij-v2-p602-draft"]["ru"]
        self.assertIn("из одной святыни в другую", ru)
        self.assertIn("людей первого различения", ru)
        self.assertNotIn("ошибочность сравнения Творца с земным царём", ru)

    def test_app_and_page_drafts_match(self):
        for number in (601, 602):
            page = self.paragraphs[f"madarij-v2-p{number:03d}-draft"]
            self.assertEqual(page["ru"], self.draft[number]["ru"])
            self.assertFalse(self.draft[number]["publisher_ready"])

    def test_no_unjustified_publication_approval(self):
        self.assertEqual(len(self.book["chapters"]), 9)
        self.assertEqual(len(self.paragraphs), 676)
        self.assertFalse(self.book["publication_ready"])
        self.assertFalse(self.book["translation_independently_verified"])


if __name__ == "__main__":
    unittest.main()
