"""Regressions for old/new RUSSIAN text comparison, without Arabic original columns."""
import json
import tempfile
import unittest
from pathlib import Path

from tools.render_russian_changes import build, collect_changes, highlighted, render


class RussianBeforeAfterTest(unittest.TestCase):
    def test_every_saved_literary_change_has_russian_old_and_new(self):
        report = collect_changes()
        self.assertEqual(report["totalSections"], 90)
        self.assertEqual(report["totalParagraphs"], 740)
        self.assertEqual(report["reviewedSections"], 30)
        self.assertEqual(report["corrections"], 286)
        self.assertEqual(report["editedParagraphs"], 193)
        self.assertEqual({c["batch"] for c in report["changes"]}, {1, 2, 3, 4, 5, 6})
        self.assertEqual(len({c["paragraphId"] for c in report["changes"]}), 193)
        self.assertTrue(all(c["oldRussian"] != c["newRussian"] and c["reasons"]
                            for c in report["changes"]))
        self.assertTrue(all("ar" not in c for c in report["changes"]))
        self.assertTrue(all(c["section"] <= 30 for c in report["changes"]))
        changed = {c["paragraphId"]: c for c in report["changes"]}
        self.assertIn("списки Корана", changed["madarij-v1-p113-draft"]["oldRussian"])
        self.assertIn("экземпляры Корана", changed["madarij-v1-p113-draft"]["newRussian"])
        self.assertIn("Слышание", changed["madarij-v1-p069-draft"]["oldRussian"].title())
        self.assertIn("Его степени в знании", changed["madarij-v1-p164-draft"]["oldRussian"])
        self.assertIn("В знании различаются две степени убудийи", changed["madarij-v1-p164-draft"]["newRussian"])
        self.assertIn("начинается словами «вместе с погружавшимися»", changed["madarij-v1-p159-draft-editor-note-01"]["newRussian"])

    def test_diff_marks_old_and_new_text_without_losing_words(self):
        before, after = highlighted("Это был прежний текст.", "Это стал новый текст.")
        self.assertIn("removed", before)
        self.assertIn("added", after)
        self.assertIn("прежний", before)
        self.assertIn("новый", after)
        self.assertNotIn("новый", before)

    def test_html_and_json_include_only_russian_comparison(self):
        with tempfile.TemporaryDirectory() as folder:
            out = Path(folder)
            report = build(html_output=out / "compare.html", json_output=out / "compare.json")
            data = json.loads((out / "compare.json").read_text(encoding="utf-8"))
            page = (out / "compare.html").read_text(encoding="utf-8")
            self.assertEqual(data["changes"], report["changes"])
            self.assertEqual(page.count('<article>'), 193)
            self.assertEqual(page.count('<details class="batch"'), 6)
            self.assertIn("БЫЛО — русский перевод", page)
            self.assertIn("СТАЛО — русский перевод", page)
            self.assertIn("экземпляры Корана", page)
            self.assertIn("убудийи", page)  # highlighted spans split the exact phrase in HTML
            self.assertNotIn("Арабский оригинал", page)
            self.assertNotIn('lang="ar"', page)
            self.assertLess(page.index('id="batch-6"'), page.index('id="batch-5"'))


if __name__ == "__main__":
    unittest.main()
