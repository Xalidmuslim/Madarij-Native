#!/usr/bin/env python3
"""Regression: 2019 edition v2 p24 Qur'an 16:20 qira'a, not Hafs-normalized.
Scope: exact electronic text and translator's rendering. Does not certify a scan.
"""
import json
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
BOOK=ROOT/"app/src/main/assets/volume2-preview.json"
DRAFT=ROOT/"research/volume2-batch-11-80/russian-pages-021-030.json"

class PrintedPage24Reading(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        data=json.loads(BOOK.read_text(encoding="utf-8"))
        cls.book=data
        cls.p={p["id"]:p for ch in data["chapters"] for p in ch["paragraphs"]}
        cls.draft=next(p for p in json.loads(DRAFT.read_text(encoding="utf-8"))["pages"] if p["printed_page"]==24)

    def test_original_reading_taduna_not_automatically_hafs(self):
        p=self.p["madarij-v2-p024-draft"]
        self.assertIn("{وَالَّذِينَ تَدْعُونَ مِنْ دُونِ اللَّهِ لَا يَخْلُقُونَ شَيْئًا وَهُمْ يُخْلَقُونَ} [النحل: ٢٠]",p["ar"])
        self.assertNotIn("وَالَّذِينَ الَّذِينَ",p["ar"])
        self.assertNotIn("وَالَّذِينَ يَدْعُونَ",p["ar"])

    def test_russian_preserves_second_person_in_printed_reading(self):
        ru=self.p["madarij-v2-p024-draft"]["ru"]
        self.assertIn("Те, к кому вы взываете помимо Аллаха",ru)
        self.assertNotIn("Те, кого они призывают помимо Аллаха",ru)
        self.assertIn("Те, к кому вы взываете помимо Аллаха",self.draft["ru"])

    def test_no_false_publication_approval(self):
        self.assertEqual(len(self.book["chapters"]),9)
        self.assertEqual(len(self.p),676)
        self.assertFalse(self.book["publication_ready"])
        self.assertFalse(self.book["translation_independently_verified"])
        self.assertFalse(self.draft["publisher_ready"])
        self.assertEqual(self.draft["review_status"],"draft_unchecked")

    def test_old_repairs_preserved(self):
        for n in [448,463,507,622]:
            self.assertIn(f"madarij-v2-p{n:03d}-draft", self.p)

if __name__=="__main__":
    unittest.main()
