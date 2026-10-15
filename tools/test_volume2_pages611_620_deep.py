#!/usr/bin/env python3
"""Volume II pages 611–620: regression checks for close electronic collation.
Not a certificate of 2019 print-scan fidelity or hadith authentication.
"""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BOOK = ROOT / "app/src/main/assets/volume2-preview.json"
DRAFT = ROOT / "research/volume2-batch-521-620/russian-pages-611-620.json"

class Pages611620(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.book = json.loads(BOOK.read_text(encoding="utf-8"))
        cls.paras = {p["id"]: p for ch in cls.book["chapters"] for p in ch["paragraphs"]}
        cls.drafts = {p["printed_page"]: p for p in json.loads(DRAFT.read_text(encoding="utf-8"))["pages"]}

    def page(self, number):
        return self.paras[f"madarij-v2-p{number:03d}-draft"]["ru"]

    def test_611_no_inferred_excessive_modesty(self):
        self.assertNotIn("чрезмерной стыдливости", self.page(611))
        self.assertNotIn("Эти сообщения показывают", self.page(611))

    def test_612_quoted_report_complete_and_uninterpolated(self):
        self.assertIn("тот проявил подлинную стыдливость перед Аллахом", self.page(612))
        self.assertNotIn("Точную формулировку", self.page(612))

    def test_613_report_preserves_original_divine_speech_opening(self):
        self.assertIn("О сын Адама", self.page(613))
        self.assertNotIn("Это сообщение требует", self.page(613))

    def test_614_no_doctrinal_smoothing_of_quoted_words(self):
        self.assertIn("Матери Книги", self.page(614))
        self.assertIn("стыдится", self.page(614))
        self.assertNotIn("будто бы велел", self.page(614))

    def test_615_author_explicitly_distinguishes_divine_attribute(self):
        self.assertIn("минбаре", self.page(615))
        self.assertIn("стыдливости Господа", self.page(615))

    def test_616_report_attributions_retained(self):
        self.assertIn("не способен определить её образ", self.page(616))
        self.assertIn("стыдится наказать", self.page(616))
        self.assertIn("Аллах стыдится его", self.page(616))

    def test_617_two_narratives_without_added_moral_summary(self):
        self.assertIn("Адам", self.page(617))
        self.assertIn("Мы не поклонялись Тебе", self.page(617))
        self.assertNotIn("Смысл наставления", self.page(617))

    def test_618_ali_question_exact_and_report_type_retained(self):
        self.assertIn("мази", self.page(618))
        self.assertIn("исраилият", self.page(618))

    def test_619_ninth_category_begins_on_its_original_page(self):
        self.assertIn("Стыд благородства и достоинства", self.page(619))

    def test_620_has_no_editorial_explanation_of_next_page(self):
        self.assertIn("из возвеличивания, соединённого с любовью", self.page(620))
        self.assertNotIn("Следующая страница", self.page(620))

    def test_sync_and_scope(self):
        for n in range(611,621):
            with self.subTest(page=n):
                self.assertEqual(self.page(n), self.drafts[n]["ru"])
                self.assertFalse(self.drafts[n]["publisher_ready"])
        self.assertEqual(len(self.book["chapters"]),9)
        self.assertEqual(len(self.paras),676)
        self.assertFalse(self.book["publication_ready"])
        self.assertFalse(self.book["translation_independently_verified"])

if __name__ == "__main__":
    unittest.main()
