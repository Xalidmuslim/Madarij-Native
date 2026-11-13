"""Volume 2 additional literary review tests: no independent religious checking."""
from __future__ import annotations

import copy
import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from tools.build_reviewed_bilingual_volume2 import (
    SOURCE, MANIFEST_DIR, reviewed_corpus
)
from tools.render_volume2_editorial import build as render_build


class Volume2LiteraryReviewTest(unittest.TestCase):
    def test_canonical_source_and_arabic_are_unchanged(self):
        raw = SOURCE.read_bytes()
        original = json.loads(raw)
        revised, report = reviewed_corpus()
        self.assertEqual(report["source_sha256"], hashlib.sha256(raw).hexdigest())
        self.assertEqual(len(revised["chapters"]), 9)
        self.assertEqual(sum(len(c["paragraphs"]) for c in revised["chapters"]), 676)
        self.assertEqual(len(report["batches"]), 2)
        self.assertEqual(report["reviewed_paragraphs"], 37)
        self.assertEqual(report["replacements"], 23)
        self.assertEqual(report["edited_paragraphs"], 19)
        self.assertEqual(report["batches"][0]["rejected_candidates"], 1)
        self.assertEqual(
            {k: v for k, v in original.items() if k != "chapters"},
            {k: v for k, v in revised.items() if k != "chapters"}
        )
        changed_ids = []
        for i, (a_chapter, b_chapter) in enumerate(zip(
            original["chapters"], revised["chapters"]
        )):
            self.assertEqual(set(a_chapter), set(b_chapter))
            for key in a_chapter:
                if key != "paragraphs":
                    self.assertEqual(a_chapter[key], b_chapter[key])
            for p, q in zip(a_chapter["paragraphs"], b_chapter["paragraphs"]):
                self.assertEqual(
                    {k: v for k, v in p.items() if k != "ru"},
                    {k: v for k, v in q.items() if k != "ru"},
                )
                if p["ru"] != q["ru"]:
                    self.assertIn(i, (0, 1))
                    changed_ids.append(p["id"])
        self.assertEqual(len(changed_ids), 19)
        self.assertEqual(SOURCE.read_bytes(), raw)

    def test_invalid_anchor_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            manifest = json.loads(
                next(MANIFEST_DIR.glob("batch[0-9][0-9]_pages*.json")).read_text()
            )
            manifest["replacements"][0]["find"] = "Фраза, отсутствующая в источнике."
            temp = Path(folder)
            (temp / "batch01_pages003_010.json").write_text(
                json.dumps(manifest, ensure_ascii=False), encoding="utf-8"
            )
            with self.assertRaisesRegex(ValueError, "anchor stale or repeated"):
                reviewed_corpus(manifest_dir=temp)

    def test_quoted_text_cannot_be_edited(self):
        with tempfile.TemporaryDirectory() as folder:
            manifest = json.loads(
                next(MANIFEST_DIR.glob("batch[0-9][0-9]_pages*.json")).read_text()
            )
            manifest["replacements"].append({
                "paragraphId": "madarij-v2-p005-05",
                "find": "Пречист Тот, Кто создал его для причинения порчи",
                "replace": "Изменённая прямая цитата",
                "reason": "Test only",
            })
            temp = Path(folder)
            (temp / "batch01_pages003_010.json").write_text(
                json.dumps(manifest, ensure_ascii=False), encoding="utf-8"
            )
            with self.assertRaisesRegex(ValueError, "Protected quotation changed"):
                reviewed_corpus(manifest_dir=temp)

    def test_russian_comparison_and_bilingual_exports(self):
        with tempfile.TemporaryDirectory() as folder:
            dest = Path(folder)
            report = render_build(
                output_json=dest / "ru.json",
                output_html=dest / "ru.html",
                bilingual_html=dest / "bilingual.html",
            )
            self.assertEqual(report["totalChapters"], 9)
            self.assertEqual(report["totalParagraphs"], 676)
            self.assertEqual(report["reviewedParagraphs"], 37)
            self.assertEqual(report["corrections"], 23)
            self.assertEqual(report["editedParagraphs"], 19)
            self.assertEqual(len(report["changes"]), 19)
            self.assertEqual(
                len({r["paragraphId"] for r in report["changes"]}), 19
            )
            self.assertIn("наставить кого-либо или ввести его в заблуждение", json.loads(SOURCE.read_text(encoding="utf-8"))["chapters"][1]["paragraphs"][3]["ru"])
            self.assertTrue(all(
                "oldRussian" in r and "newRussian" in r and r["reasons"]
                for r in report["changes"]
            ))
            self.assertTrue(all("ar" not in r for r in report["changes"]))
            russian = (dest / "ru.html").read_text(encoding="utf-8")
            bilingual = (dest / "bilingual.html").read_text(encoding="utf-8")
            self.assertEqual(russian.count('<article>'), 19)
            self.assertEqual(bilingual.count('<article class="bilingual-pair">'), 676)
            self.assertIn('lang="ar"', bilingual)
            self.assertNotIn('lang="ar"', russian)


if __name__ == "__main__":
    unittest.main()
