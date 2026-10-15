#!/usr/bin/env python3
"""Volume II pages 81-120: text-only regressions; no scan or hadith certification."""
import json
import unittest
from pathlib import Path

BASE=Path(__file__).resolve().parents[1]

class Collation081120(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.book=json.loads((BASE/"app/src/main/assets/volume2-preview.json").read_text(encoding="utf-8"))
        cls.p={int(p["id"].split("-p")[1][:3]):p for c in cls.book["chapters"] for p in c["paragraphs"] if "-draft" in p["id"]}

    def test_quran_79_46_restored(self):
        a=self.p[82]["ar"]
        self.assertIn("عَشِيَّةً أَوْ ضُحَاهَا} [النازعات: ٤٦]",a)
        self.assertNotIn("{(٤٥) كَأَنَّهُمْ",a)

    def test_quran_47_24_23_68_restored(self):
        a=self.p[83]["ar"]
        self.assertIn("قُلُوبٍ أَقْفَالُهَا} [محمد: ٢٤]",a)
        self.assertIn("أَفَلَمْ يَدَّبَّرُوا الْقَوْلَ} [المؤمنون: ٦٨]",a)
        self.assertNotIn("{(٢٣) أَفَلَا",a)
        self.assertNotIn("{(٦٧) أَفَلَمْ",a)

    def test_author_voice(self):
        forbidden={
            87:["то автор указал"],
            93:["По объяснению автора"],
            96:["автор считает лучше"],
            97:["по мысли автора","которые автор считает"],
            117:["По объяснению автора"],
            120:["По оценке автора","Он называет эту группу"]
        }
        for n,terms in forbidden.items():
            for term in terms:
                with self.subTest(page=n,term=term):
                    self.assertNotIn(term,self.p[n]["ru"])

    def test_do_not_erase_legitimate_verse_numbers(self):
        self.assertIn("(٢)",self.p[117]["ar"])
        self.assertIn("(٦)",self.p[119]["ar"])
        self.assertIn("لَتَرَوُنَّ الْجَحِيمَ",self.p[119]["ar"])

    def test_source_and_publication_safety(self):
        self.assertEqual(len(self.book["chapters"]),9)
        self.assertEqual(sum(len(c["paragraphs"]) for c in self.book["chapters"]),676)
        self.assertFalse(self.book["publication_ready"])
        self.assertFalse(self.book["translation_independently_verified"])
        self.assertIn("рухнул бы его престол",self.p[26]["ru"])
        self.assertIn("مَا كَانُوا يَعْمَلُونَ} [النحل: ٩٧]",self.p[38]["ar"])

if __name__=="__main__":
    unittest.main()
