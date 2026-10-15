#!/usr/bin/env python3
"""Volume-two printed pages 6–9: precise translation and provenance regressions."""
import json
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]

class Page006009Collation(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.book=json.loads((ROOT/"app/src/main/assets/volume2-preview.json").read_text(encoding="utf-8"))
        cls.paras={p["id"]:p for c in cls.book["chapters"] for p in c["paragraphs"]}

    def test_page_6_preserves_complete_author_reasoning(self):
        text=self.paras["madarij-v2-p006-01"]["ru"]
        self.assertIn("Вина же лежит",text)
        self.assertIn("постоянно носить доспехи",text)
        for intrusion in ("По объяснению автора", "Автор связывает", "По его сравнению"):
            self.assertNotIn(intrusion,text)

    def test_page_7_explicit_legal_killing_not_euphemized(self):
        self.assertIn("дозволено убить его таким же способом",self.paras["madarij-v2-p007-c"]["ru"])
        self.assertIn("вправе убить виновного",self.paras["madarij-v2-p007-d"]["ru"])
        self.assertIn("أن يقتله بمثل ما قتل به",self.paras["madarij-v2-p007-c"]["ar"])
        self.assertIn("أن يقتله بالحال",self.paras["madarij-v2-p007-d"]["ar"])

    def test_page_8_author_first_person_and_direct_examples(self):
        self.assertIn("насильственного вливания",self.paras["madarij-v2-p008-a"]["ru"])
        self.assertNotIn("Автор сопоставляет",self.paras["madarij-v2-p008-a"]["ru"])
        self.assertIn("нам и другим людям",self.paras["madarij-v2-p008-c"]["ru"])
        self.assertNotIn("По мнению автора",self.paras["madarij-v2-p008-c"]["ru"])

    def test_page_9_and_full_book_safeguards(self):
        self.assertIn("клюнул его три раза",self.paras["madarij-v2-p009-a"]["ru"])
        self.assertIn("мимо хорошей пищи",self.paras["madarij-v2-p009-b"]["ru"])
        self.assertEqual(len(self.paras),676)
        self.assertEqual(len(self.book["chapters"]),9)
        self.assertFalse(self.book["translation_independently_verified"])
        self.assertFalse(self.book["publication_ready"])
if __name__=="__main__":
    unittest.main()
