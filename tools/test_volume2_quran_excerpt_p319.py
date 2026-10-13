#!/usr/bin/env python3
"""Protect p319 repair of an incomplete Qur'an quotation (Al-Hajj 22:30).
Validation is electronic, not target printed-page image certification.
"""
import json
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
BOOK=ROOT/"app/src/main/assets/volume2-preview.json"
DRAFT=ROOT/"research/volume2-batch-221-320/russian-pages-311-320.json"

class PrintedPage319Verse(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.book=json.loads(BOOK.read_text(encoding="utf-8"))
        cls.p=next(p for c in cls.book["chapters"] for p in c["paragraphs"] if p["id"]=="madarij-v2-p319-draft")
        cls.draft=next(p for p in json.loads(DRAFT.read_text(encoding="utf-8"))["pages"] if p["printed_page"]==319)

    def test_complete_selected_quranic_phrase(self):
        self.assertIn("{وَمَنْ يُعَظِّمْ حُرُمَاتِ اللَّهِ فَهُوَ خَيْرٌ لَهُ عِنْدَ رَبِّهِ} [الحج: ٣٠]",self.p["ar"])
        self.assertNotIn("وَأُحِلَّتْ} [الحج: ٣٠]",self.p["ar"])

    def test_separate_outdated_ocr_note_from_book(self):
        for ru in [self.p["ru"],self.draft["ru"]]:
            self.assertIn("Кто возвеличивает неприкосновенные установления Аллаха",ru)
            self.assertNotIn("*Примечание:* запись арабской цитаты аята в извлечённом источнике неполная",ru)

    def test_preserve_draft_and_earlier_pages(self):
        self.assertEqual(len(self.book["chapters"]),9)
        self.assertEqual(sum(len(c["paragraphs"]) for c in self.book["chapters"]),676)
        self.assertFalse(self.book["publication_ready"])
        self.assertFalse(self.book["translation_independently_verified"])
        self.assertFalse(self.draft["publisher_ready"])
        self.assertEqual(self.draft["review_status"],"draft_unchecked")

if __name__=="__main__":
    unittest.main()
