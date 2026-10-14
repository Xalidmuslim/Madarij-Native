#!/usr/bin/env python3
"""Volume II source fidelity guards for pages 631–659 and 657-page coverage.

NOT a certificate of print-scan verification, hadith grading, or publication readiness.
"""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BOOK = json.loads((ROOT / "app/src/main/assets/volume2-preview.json").read_text(encoding="utf-8"))
BASE = ROOT / "research/volume2-batch-621-659"

class FinalPages(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.paragraphs = {p["id"]: p for ch in BOOK["chapters"] for p in ch["paragraphs"]}
        cls.russian = {}
        for r in ("631-640", "641-650", "651-659"):
            data = json.loads((BASE / f"russian-pages-{r}.json").read_text(encoding="utf-8"))
            cls.russian.update({p["printed_page"]: p for p in data["pages"]})

    def page(self, number):
        return self.paragraphs[f"madarij-v2-p{number}-draft"]["ru"]

    def test_pages_synced_and_not_certified(self):
        for n in range(631, 660):
            with self.subTest(n=n):
                self.assertEqual(self.page(n), self.russian[n]["ru"])
                self.assertFalse(self.russian[n]["publisher_ready"])
        self.assertEqual(len(BOOK["chapters"]), 9)
        self.assertEqual(sum(len(c["paragraphs"]) for c in BOOK["chapters"]), 676)
        self.assertFalse(BOOK["publication_ready"])
        self.assertFalse(BOOK["translation_independently_verified"])

    def test_quran_19_50_continues_across_printed_boundary(self):
        self.assertIn("язык правды", self.page(631))
        self.assertNotIn("19:50", self.page(631))
        self.assertIn("19:50", self.page(632))

    def test_633_no_unquoted_hadith_augmentation(self):
        self.assertNotIn("В полном хадисе также", self.page(633))

    def test_634_author_did_not_announce_next_page(self):
        self.assertNotIn("Следующая страница объясняет", self.page(634))

    def test_638_no_anachronistic_editorial_interpolation(self):
        self.assertNotIn("а не об оценке каждого иудея", self.page(638))

    def test_640_attributed_reports_without_editorial_notes_in_author_text(self):
        self.assertNotIn("Его происхождение требует", self.page(640))
        self.assertNotIn("Это личное оценочное", self.page(640))

    def test_645_remove_unattributed_gloss(self):
        self.assertNotIn("Здесь описывается духовный образ автора", self.page(645))

    def test_646_umar_report_not_modified(self):
        self.assertNotIn("в историческом контексте", self.page(646))

    def test_648_source_hadith_quoted_closely(self):
        self.assertIn("унесли награду", self.page(648))

    def test_650_retain_authorial_first_to_ignite_fire(self):
        self.assertIn("ими первыми разожгут Огонь", self.page(650))
        self.assertNotIn("Второе толкование следует", self.page(650))

    def test_657_658_source_boundaries_not_narrator_notes(self):
        self.assertNotIn("Ответ следует на следующей странице", self.page(657))
        self.assertNotIn("Продолжение приводится на следующей странице", self.page(658))

    def test_659_preserve_divine_essence_distinction(self):
        self.assertIn("близостью Его сущности", self.page(659))

if __name__ == "__main__":
    unittest.main()
