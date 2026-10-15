#!/usr/bin/env python3
"""Electronic Arabic source / Qur'an 96:19 fidelity checks, printed pp101–120."""
import json
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
BOOK=json.loads((ROOT/"app/src/main/assets/volume2-preview.json").read_text(encoding="utf-8"))
PAGES={p["id"]:p for ch in BOOK["chapters"] for p in ch["paragraphs"]}

def page(n):
    return PAGES[f"madarij-v2-p{n:03d}-draft"]

class ArabicQuranFidelity(unittest.TestCase):
    def test_quran_96_19_full_negation_in_arabic_and_russian(self):
        self.assertIn("كَلَّا لَا تُطِعْهُ وَاسْجُدْ وَاقْتَرِبْ",page(110)["ar"])
        self.assertIn("Не повинуйся ему",page(110)["ru"])
        self.assertNotIn("«Повинуйся Ему",page(110)["ru"])

    def test_editorial_notes_moved_out_of_the_author_text(self):
        forbidden=[
            (101,"Следует отдельно проверить цепочку передачи"),
            (105,"Атрибуция и достоверность этого сообщения требуют проверки"),
            (105,"Эти слова также требуют проверки источника"),
            (106,"Происхождение и оценка этой передачи требуют отдельной сверки"),
        ]
        for n,text in forbidden:
            with self.subTest(page=n,text=text):
                self.assertNotIn(text,page(n)["ru"])

    def test_draft_and_app_match_exactly(self):
        for suffix in ("101-110","111-120"):
            data=json.loads((ROOT/f"research/volume2-batch-81-150/russian-pages-{suffix}.json").read_text(encoding="utf-8"))
            for p in data["pages"]:
                self.assertEqual(p["ru"],page(p["printed_page"])["ru"])
                self.assertFalse(p["publisher_ready"])

    def test_readiness_flags_are_false(self):
        self.assertFalse(BOOK["publication_ready"])
        self.assertFalse(BOOK["translation_independently_verified"])
        self.assertEqual(len(BOOK["chapters"]),9)
        self.assertEqual(sum(len(ch["paragraphs"]) for ch in BOOK["chapters"]),676)

if __name__=="__main__":
    unittest.main()
