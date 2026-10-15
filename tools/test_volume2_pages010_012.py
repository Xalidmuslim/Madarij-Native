#!/usr/bin/env python3
"""Pages 10-12: precise translation of animal-nature analogy and no accidental truncation."""
import json
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

class Page010012Regression(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        v=json.loads((ROOT/"app/src/main/assets/volume2-preview.json").read_text(encoding="utf8"))
        cls.paras={p["id"]:p for c in v["chapters"] for p in c["paragraphs"]}
        cls.book=v

    def test_page10_source_argument_and_author_voice(self):
        b=self.paras["madarij-v2-p010-b"]["ru"]
        d=self.paras["madarij-v2-p010-d"]["ru"]
        e=self.paras["madarij-v2-p010-e"]["ru"]
        self.assertIn("самому жестокосердному",b)
        self.assertIn("питающее подобно питающемуся",e)
        self.assertIn("Аллах запретил",e)
        self.assertIn("лошадей",d)
        for intrusive in ("которого автор описывает","по мнению автора","по его объяснению","автор связывает"):
            self.assertNotIn(intrusive,b.lower()+d.lower()+e.lower())

    def test_page11_12_continuity_and_original_terminology(self):
        self.assertIn("четырёх природных начал",self.paras["madarij-v2-p011-draft"]["ru"])
        self.assertIn("Третий взгляд",self.paras["madarij-v2-p012-draft"]["ru"])
        self.assertIn("покорность",self.paras["madarij-v2-p012-draft"]["ru"])
        self.assertIn("أبلَمُ خبيث",self.paras["madarij-v2-p010-c"]["ar"])

    def test_immutability_of_book_and_previous_corrected_pages(self):
        self.assertEqual(len(self.paras),676)
        self.assertEqual(len(self.book["chapters"]),9)
        self.assertIn("дозволено убить его таким же способом",self.paras["madarij-v2-p007-c"]["ru"])
        self.assertIn("высовывает язык",self.paras["madarij-v2-p004-02"]["ru"])
        self.assertFalse(self.book["translation_independently_verified"])
        self.assertFalse(self.book["publication_ready"])
if __name__=="__main__":
    unittest.main()
