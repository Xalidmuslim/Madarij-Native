"""Regression checks for the fourth volume's offline reader package."""
import copy
import unittest
from tools.build_volume4_assets import build, validate, require


class Volume4ReaderPackageTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.bundle, cls.structure = build()

    def test_fourth_volume_contains_every_original_paragraph(self):
        report = validate(self.bundle, self.structure)
        self.assertEqual(554, report["author_pages"])
        self.assertEqual(2137, report["author_paragraphs"])
        self.assertEqual(23, report["navigation_ranges"])
        self.assertEqual(21, report["open_review_paragraphs"])

    def test_all_navigation_targets_exist_in_the_same_range(self):
        by_id = {
            c["id"]: {p["id"] for p in c["paragraphs"]}
            for c in self.bundle["chapters"]
        }
        for item in self.structure["groups"][0]["items"]:
            self.assertIn(item["entryParagraphId"], by_id[item["chapterId"]])
            for topic in item["topics"]:
                self.assertIn(topic["paragraphId"], by_id[item["chapterId"]])

    def test_draft_cannot_be_silently_marked_for_publication(self):
        draft = copy.deepcopy(self.bundle)
        draft["publication_ready"] = True
        with self.assertRaises(ValueError):
            validate(draft, self.structure)

    def test_lost_paragraph_is_rejected(self):
        draft = copy.deepcopy(self.bundle)
        draft["chapters"][0]["paragraphs"].pop()
        with self.assertRaises(ValueError):
            validate(draft, self.structure)

    def test_changed_russian_text_is_rejected(self):
        draft = copy.deepcopy(self.bundle)
        draft["chapters"][0]["paragraphs"][1]["ru"] = "Сокращённый пересказ"
        with self.assertRaises(ValueError):
            validate(draft, self.structure)

    def test_broken_toc_anchor_is_rejected(self):
        structure = copy.deepcopy(self.structure)
        structure["groups"][0]["items"][0]["topics"][0]["paragraphId"] = "missing"
        with self.assertRaises(ValueError):
            validate(self.bundle, structure)


if __name__ == "__main__":
    unittest.main()
