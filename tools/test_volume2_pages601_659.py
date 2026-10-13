#!/usr/bin/env python3
"""Regressions for selective content collation of volume II pages 601–659.

These are textual fidelity guards, not printed-scan, sanad or publication certification.
"""
import json
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
BOOK=ROOT/"app/src/main/assets/volume2-preview.json"

class Vol2LastPages(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.book=json.loads(BOOK.read_text(encoding="utf-8"))
        cls.p={p["id"]:p for ch in cls.book["chapters"] for p in ch["paragraphs"]}
    def page(self,n):
        return self.p[f"madarij-v2-p{n:03d}-draft"]

    def test_arabic_quran_repaired_without_verse_number_noise(self):
        self.assertIn("{أَلَمْ يَعْلَمْ بِأَنَّ اللَّهَ يَرَى} [العلق: ١٤]",self.page(611)["ar"])
        self.assertNotIn("{(١٣) أَلَمْ",self.page(611)["ar"])
        self.assertIn("{إِنَّ اللَّهَ مَعَ الَّذِينَ اتَّقَوْا وَالَّذِينَ هُمْ مُحْسِنُونَ} [النحل: ١٢٨]",self.page(622)["ar"])
        self.assertNotIn("{(١٢٧) إِنَّ اللَّهَ",self.page(622)["ar"])
        self.assertNotIn("В текстовом арабском корпусе некоторые слова этих аятов повреждены",self.page(622)["ru"])
        self.assertIn("{وَإِذَا سَأَلَكَ عِبَادِي عَنِّي فَإِنِّي قَرِيبٌ أُجِيبُ دَعْوَةَ الدَّاعِ إِذَا دَعَانِ} [البقرة: ١٨٦]",self.page(623)["ar"])
        self.assertNotIn("{(١٨٥) وَإِذَا",self.page(623)["ar"])

    def test_p603_restores_missing_discussion_and_no_interpolated_conditions(self):
        t=self.page(603)["ru"]
        self.assertIn("уровни наказания в Огне различаются",t)
        self.assertIn("признание благодеяния",t)
        self.assertNotIn("по мнению автора",t)
        self.assertNotIn("уровни ответственности людей различаются",t)

    def test_p607_specific_scholarly_discussion_not_an_editorial_abstract(self):
        t=self.page(607)["ru"]
        self.assertIn("степенью собранности",t)
        self.assertIn("исчезновение осознания",t)
        self.assertNotIn("Автор описывает эту позицию без признания её обязательной",t)

    def test_authorial_discourse_on_later_pages(self):
        for n in (612,615,624,630,637,639,648,650,655,657,659):
            with self.subTest(n=n):
                ru=self.page(n)["ru"]
                self.assertNotIn("по мнению автора",ru)
                self.assertNotIn("По мнению автора",ru)
        self.assertIn("Именно это толкование мы избираем",self.page(639)["ru"])

    def test_preserved_book_and_previous_repairs(self):
        self.assertEqual(len(self.book["chapters"]),9)
        self.assertEqual(len(self.p),676)
        self.assertFalse(self.book["publication_ready"])
        self.assertFalse(self.book["translation_independently_verified"])
        self.assertIn("Если пожелает, Он успокоит ветер",self.page(448)["ru"])
        self.assertIn("эта мысль является одним из порицаемых утверждений",self.page(463)["ru"])
        self.assertIn("пока не вкусили Нашего наказания",self.page(507)["ru"])
        self.assertIn("إِنَّ اللَّهَ مَعَ الصَّابِرِينَ",self.page(622)["ar"])

if __name__=="__main__":
    unittest.main()
