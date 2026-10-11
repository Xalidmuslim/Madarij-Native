#!/usr/bin/env python3
"""Regression tests for textual collation of printed pages 3-5."""
import json
import unittest
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]

class Collation003005(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.book=json.loads((ROOT/"app/src/main/assets/volume2-preview.json").read_text(encoding="utf-8"))
        cls.paras={p["id"]:p for c in cls.book["chapters"] for p in c["paragraphs"]}

    def test_page_3_printed_heading_preserved(self):
        a=self.paras["madarij-v2-p003-a"]
        source=next(json.loads(x) for x in (ROOT/"research/volume2-source/arabic_pages.jsonl").open(encoding="utf-8") if '"printed_page": 3,' in x)
        self.assertTrue(source["arabic_body"].startswith("فصل في مشاهد الخلق في المعصية"))
        self.assertTrue(a["ar"].startswith("فصل في مشاهد الخلق في المعصية\n"))
        self.assertTrue(a["ru"].startswith("Раздел о взглядах людей на грех\n\n"))
        
    def test_page_4_quran_7_176_metaphor_restored(self):
        p=self.paras["madarij-v2-p004-02"]
        self.assertEqual(p["ru"].count("высовывает язык"),2)
        self.assertIn("إن تحمل عليه يلهث أو تتركه يلهث",p["ar"])

    def test_page_5_no_editorial_intrusion(self):
        p=self.paras["madarij-v2-p005-06"]
        self.assertNotIn("по приводимому им выражению",p["ru"])
        self.assertIn("из-за него человек оказывается в могиле, а верблюд — в котле",p["ru"])
        self.assertEqual(p["role"],"author")

    def test_previous_repairs_and_draft_state_preserved(self):
        self.assertEqual(len(self.book["chapters"]),9)
        self.assertEqual(len(self.paras),676)
        self.assertIn("Если пожелает, Он успокоит ветер",self.paras["madarij-v2-p448-draft"]["ru"])
        self.assertIn("эта мысль является одним из порицаемых утверждений",self.paras["madarij-v2-p463-draft"]["ru"])
        self.assertIn("пока не вкусили Нашего наказания",self.paras["madarij-v2-p507-draft"]["ru"])
        self.assertIn("إِنَّ اللَّهَ مَعَ الصَّابِرِينَ",self.paras["madarij-v2-p622-draft"]["ar"])
        self.assertFalse(self.book["translation_independently_verified"])
        self.assertFalse(self.book["publication_ready"])

if __name__=="__main__":
    unittest.main()
