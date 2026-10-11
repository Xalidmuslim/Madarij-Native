#!/usr/bin/env python3
"""Pages 13-20, author voice and continuity regressions."""
import json
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
class Page013020Collation(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data=json.loads((ROOT/"app/src/main/assets/volume2-preview.json").read_text(encoding="utf-8"))
        cls.p={p["id"]:p for c in cls.data["chapters"] for p in c["paragraphs"]}
    def test_page13_15_author_voice_preserved(self):
        p=self.p
        self.assertIn("такие люди — враги Аллаха",p["madarij-v2-p013-draft"]["ru"])
        self.assertNotIn("по словам автора",p["madarij-v2-p013-draft"]["ru"])
        self.assertIn("они крайне обделены",p["madarij-v2-p014-draft"]["ru"])
        self.assertNotIn("по оценке автора",p["madarij-v2-p014-draft"]["ru"])
        self.assertNotIn("По словам автора",p["madarij-v2-p015-draft"]["ru"])
        self.assertNotIn("как утверждает автор",p["madarij-v2-p015-draft"]["ru"])
        self.assertIn("[الأعراف: ٥٤]",p["madarij-v2-p015-draft"]["ar"])
    def test_page16_20_no_injected_poetry_attribution(self):
        p=self.p
        self.assertNotIn("Поэт сказал:",p["madarij-v2-p016-draft"]["ru"])
        self.assertIn("Иблиса",p["madarij-v2-p019-draft"]["ru"])
        self.assertNotIn("по передаче автора",p["madarij-v2-p017-draft"]["ru"])
        self.assertIn("Шестой взгляд",p["madarij-v2-p020-draft"]["ru"])
    def test_data_safety(self):
        self.assertEqual(sum(len(c["paragraphs"]) for c in self.data["chapters"]),676)
        self.assertFalse(self.data["publication_ready"])
        self.assertFalse(self.data["translation_independently_verified"])
        self.assertIn("питающее подобно питающемуся",self.p["madarij-v2-p010-e"]["ru"])
if __name__=="__main__":
    unittest.main()
