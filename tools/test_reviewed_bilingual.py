"""Text-only regression tests: do not compile or package an APK."""
import copy
import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from tools.build_reviewed_bilingual import SOURCE, MANIFEST_DIR, reviewed_corpus
from tools.render_reviewed_bilingual import render


class BilingualLiteraryCorpusTest(unittest.TestCase):
    def test_seventeen_batches_keep_all_source_arabic_and_unreviewed_russian(self):
        raw = SOURCE.read_bytes()
        original = json.loads(raw)
        compiled, report = reviewed_corpus()
        self.assertEqual(len(compiled["chapters"]), 90)
        self.assertEqual(sum(len(ch["paragraphs"]) for ch in compiled["chapters"]), 740)
        self.assertEqual([x["replacements"] for x in report["batches"]], [21,38,39,46,66,76,70,64,112,100,79,68,87,76,49,28,32])
        self.assertEqual([x["edited_paragraphs"] for x in report["batches"]], [20,30,28,36,39,40,31,26,42,39,38,27,33,31,29,22,25])
        self.assertEqual([x["reviewed_paragraphs"] for x in report["batches"]], [78,38,40,42,50,41,32,27,44,40,42,32,38,34,44,46,36])
        altered = []
        for i, (before, after) in enumerate(zip(original["chapters"], compiled["chapters"])):
            self.assertEqual(before["id"], after["id"])
            self.assertEqual(before["expected_paragraph_ids"], after["expected_paragraph_ids"])
            for a, b in zip(before["paragraphs"], after["paragraphs"]):
                self.assertEqual(a["id"], b["id"])
                self.assertEqual(a["ar"], b["ar"])
                for key in set(a) - {"ru"}:
                    self.assertEqual(a[key], b[key], f"{a['id']}:{key}")
                if a["ru"] != b["ru"]:
                    self.assertLess(i, 85)
                    altered.append(a["id"])
        self.assertEqual(len(altered), 536)
        self.assertEqual(hashlib.sha256(raw).hexdigest(), report["base_source_sha256"])
        self.assertTrue(any(x["id"] == "madarij-v1-p067-draft"
                            for ch in compiled["chapters"] for x in ch["paragraphs"]))
        self.assertEqual(SOURCE.read_bytes(), raw)

    def test_stale_edit_anchor_is_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            tmp = Path(folder)
            for p in sorted(MANIFEST_DIR.glob("batch[0-9][0-9]_pages*.json")):
                data = json.loads(p.read_text(encoding="utf-8"))
                if "batch02" in p.name:
                    data["replacements"][0]["find"] = "Строки нет в оригинале"
                (tmp / p.name).write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "anchor stale or repeated"):
                reviewed_corpus(manifest_dir=tmp)

    def test_offline_html_has_all_pairs_and_review_status(self):
        corpus, _ = reviewed_corpus()
        manifests = [json.loads(p.read_text(encoding="utf-8"))
                     for p in sorted(MANIFEST_DIR.glob("batch[0-9][0-9]_pages*.json"))]
        with tempfile.TemporaryDirectory() as folder:
            path = render(corpus, output=Path(folder) / "compare.html", manifests=manifests)
            text = path.read_text(encoding="utf-8")
            self.assertEqual(text.count('<article class="pair'), 740)
            self.assertEqual(text.count('<details id="part-'), 90)
            self.assertEqual(text.count('Литературно просмотрено'), 85)
            self.assertIn("فصل", text)
            self.assertIn("свидетельство о причастности к пророческому наследию", text)
            self.assertIn("Над сновидениями поставлен ангел", text)
            self.assertIn("экземпляры Корана", text)
            self.assertIn("В знании различаются две степени убудийи", text)
            self.assertIn("Арабский оригинал", text)
            self.assertIn("Русский перевод", text)


if __name__ == "__main__":
    unittest.main()
