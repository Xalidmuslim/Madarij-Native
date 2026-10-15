#!/usr/bin/env python3
"""Translation source-fidelity regressions, printed pages 621–630 of volume II.

These checks use the available electronic Arabic text. They DO NOT authenticate
hadith, verify the original 2019 print scan, or certify publication readiness.
"""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BOOK = ROOT / "app/src/main/assets/volume2-preview.json"
DRAFT = ROOT / "research/volume2-batch-621-659/russian-pages-621-630.json"

class Pages621630(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.book = json.loads(BOOK.read_text(encoding="utf-8"))
        cls.paragraphs = {
            p["id"]: p for ch in cls.book["chapters"] for p in ch["paragraphs"]
        }
        cls.drafts = {
            p["printed_page"]: p
            for p in json.loads(DRAFT.read_text(encoding="utf-8"))["pages"]
        }

    def page(self, num):
        return self.paragraphs[f"madarij-v2-p{num:03d}-draft"]["ru"]

    def test_621_authorial_observation_not_editorial(self):
        self.assertIn("боящийся", self.page(621))
        self.assertNotIn("духовного старания", self.page(621))

    def test_622_maiyya_does_not_imply_separation_either(self):
        self.assertIn("ни соседства, ни отдалённости", self.page(622))
        self.assertNotIn("чрезмерное погружение", self.page(622))

    def test_623_prayer_and_worship_are_distinguished(self):
        self.assertIn("близость к взывающему", self.page(623))
        self.assertIn("близость к поклоняющемуся", self.page(623))
        self.assertNotIn("хадис продолжается на следующей странице", self.page(623))

    def test_624_retain_opposed_view_without_smoothing(self):
        self.assertIn("отрицающего", self.page(624))
        self.assertIn("пребывая над Своим Троном", self.page(624))
        self.assertNotIn("Автор возражает", self.page(624))

    def test_625_source_says_interaction_not_excessive_interaction(self):
        self.assertIn("общение с людьми становится ему неприятным", self.page(625))
        self.assertNotIn("чрезмерного погружения", self.page(625))

    def test_626_completion_of_endless_limit_passage(self):
        self.assertIn("нет предела", self.page(626))
        self.assertIn("милости", self.page(626))

    def test_627_original_analogy_kept_as_authorial_claim(self):
        self.assertIn("Из обителей пророков", self.page(627))
        self.assertNotIn("— образ постоянной поддержки", self.page(627))

    def test_628_quran_47_21_and_contrast_of_truth_lie(self):
        self.assertIn("Когда дело будет решено", self.page(628))
        self.assertIn("основа лицемерия", self.page(628))

    def test_629_three_degrees_and_verses(self):
        self.assertIn("Правдивость в состояниях", self.page(629))
        self.assertIn("самая высокая степень правдивости", self.page(629))

    def test_630_full_historical_comparison_no_editorial_disclaimer(self):
        self.assertIn("Бану Курайза", self.page(630))
        self.assertIn("не был совершён ради Аллаха", self.page(630))
        self.assertNotIn("Это историческое изложение автора", self.page(630))

    def test_draft_sync_and_scope(self):
        for n in range(621, 631):
            with self.subTest(page=n):
                self.assertEqual(self.page(n), self.drafts[n]["ru"])
                self.assertFalse(self.drafts[n]["publisher_ready"])
        self.assertEqual(len(self.book["chapters"]), 9)
        self.assertEqual(
            len(self.paragraphs), 676
        )
        self.assertFalse(self.book["publication_ready"])
        self.assertFalse(self.book["translation_independently_verified"])

if __name__ == "__main__":
    unittest.main()
