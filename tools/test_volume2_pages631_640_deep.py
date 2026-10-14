#!/usr/bin/env python3
"""Volume II pp631–640 source fidelity: electronic collation, NOT print certification."""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

class TranslationPages631640(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.book=json.loads((ROOT/"app/src/main/assets/volume2-preview.json").read_text(encoding="utf-8"))
        cls.pars={p["id"]:p for ch in cls.book["chapters"] for p in ch["paragraphs"]}
        cls.draft={p["printed_page"]:p for p in json.loads((ROOT/"research/volume2-batch-621-659/russian-pages-631-640.json").read_text(encoding="utf-8"))["pages"]}
    def ru(self,n):
        return self.pars[f"madarij-v2-p{n:03d}-draft"]["ru"]
    def test_p631_no_page_direction(self):
        self.assertNotIn("Продолжение объяснения следует на следующей странице",self.ru(631))
    def test_p632_preserve_three_uses_of_tongue(self):
        self.assertIn("три значения",self.ru(632))
        self.assertIn("орган речи",self.ru(632))
    def test_p633_quote_not_commented_from_other_version(self):
        self.assertNotIn("В полном хадисе также содержится",self.ru(633))
        self.assertNotIn("Автор относит к этой категории",self.ru(633))
    def test_p634_author_quote_no_next_page_note(self):
        self.assertIn("сорок лет",self.ru(634))
        self.assertNotIn("Следующая страница",self.ru(634))
    def test_p635_faithful_analogy(self):
        self.assertIn("развалин",self.ru(635))
        self.assertNotIn("уже оставленную",self.ru(635))
    def test_p636_no_external_historical_qualification(self):
        self.assertNotIn("в историческом контексте автора",self.ru(636))
    def test_p637_second_explanation(self):
        self.assertIn("гор",self.ru(637))
        self.assertIn("перо",self.ru(637))
    def test_p638_no_source-external universal qualifier(self):
        self.assertNotIn("не об оценке каждого иудея",self.ru(638))
    def test_p639_preserve_preferred_interpretation(self):
        self.assertIn("мы избираем",self.ru(639))
    def test_p640_keep_unverified_attributions_separate(self):
        self.assertIn("божественном предании",self.ru(640))
        self.assertNotIn("Его происхождение требует отдельной проверки",self.ru(640))
        self.assertNotIn("Это личное оценочное высказывание",self.ru(640))
    def test_sync_status_and_structure(self):
        for n in range(631,641):
            with self.subTest(page=n):
                self.assertEqual(self.ru(n),self.draft[n]["ru"])
                self.assertFalse(self.draft[n]["publisher_ready"])
        self.assertEqual(len(self.book["chapters"]),9)
        self.assertEqual(len(self.pars),676)
        self.assertFalse(self.book["publication_ready"])
        self.assertFalse(self.book["translation_independently_verified"])
if __name__=="__main__":
    unittest.main()
