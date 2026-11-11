"""Regression checks: five editorial sections and no Arabic script in APK assets."""
import copy
import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from tools.prepare_russian_reader_assets import (
    ARABIC, BOOKS, EDITIONS, SOURCE, apply_first_batch, build, russian_only
)


class RussianReaderPackagingTest(unittest.TestCase):
    def test_mixed_script_verse_retains_russian_and_quran_reference(self):
        text = "Всевышний сказал: {قُلْ بِفَضْلِ اللَّهِ} — «Скажи: пусть они радуются» [«Юнус», 10:58]."
        visible = russian_only(text)
        self.assertEqual("Всевышний сказал: «Скажи: пусть они радуются» [«Юнус», 10:58].", visible)
        self.assertFalse(ARABIC.search(visible))

    def test_cyrillic_is_unmodified_when_there_is_no_arabic(self):
        text = "Любовь к Аллаху и благоговейный страх — два основания поклонения."
        self.assertEqual(text, russian_only(text))

    def test_solely_arabic_quotation_does_not_leave_empty_guillemets(self):
        self.assertEqual("Русский перевод.", russian_only("«السَّلَامُ» Русский перевод."))

    def test_literary_manifest_refuses_stale_anchors(self):
        manifest = json.loads(EDITIONS.read_text(encoding="utf-8"))
        corpus = json.loads((SOURCE / "corpus.json").read_text(encoding="utf-8"))
        manifest["replacements"][0]["find"] = "невстречающаяся в оригинале строка"
        with self.assertRaisesRegex(ValueError, "anchor changed"):
            apply_first_batch(corpus, manifest)

    def test_all_four_books_and_section_ids_preserved_with_russian_only_assets(self):
        # Source Arabic remains recoverable for the next editor's comparison.
        canonical_sha = {name: hashlib.sha256((SOURCE / name).read_bytes()).hexdigest()
                         for name in BOOKS}
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "packed"
            counts = build(target=output)
            self.assertEqual([740, 676, 3112, 2137], [counts[n] for n in BOOKS])
            for filename in BOOKS:
                source_book = json.loads((SOURCE / filename).read_text(encoding="utf-8"))
                packaged_book = json.loads((output / filename).read_text(encoding="utf-8"))
                self.assertEqual([ch["id"] for ch in source_book["chapters"]],
                                 [ch["id"] for ch in packaged_book["chapters"]])
                self.assertEqual(
                    [p["id"] for c in source_book["chapters"] for p in c["paragraphs"]],
                    [p["id"] for c in packaged_book["chapters"] for p in c["paragraphs"]])
                self.assertTrue(all(not p["ar"] for ch in packaged_book["chapters"]
                                    for p in ch["paragraphs"]))
            for asset in output.rglob("*.json"):
                self.assertFalse(ARABIC.search(asset.read_text(encoding="utf-8")), str(asset))
            book1 = json.loads((output / "corpus.json").read_text(encoding="utf-8"))
            original = json.loads((SOURCE / "corpus.json").read_text(encoding="utf-8"))
            first = {p["id"]: p["ru"] for ch in book1["chapters"][:5] for p in ch["paragraphs"]}
            self.assertIn("высеченными кирками человеческих мнений",
                          first["madarij-v1-opening-011"])
            self.assertIn("Первое доказательство", first["madarij-v1-fatiha-006"])
            # Outside the requested first five, only Arabic letter removal is permitted.
            self.assertEqual(original["chapters"][5]["paragraphs"][0]["ru"],
                             book1["chapters"][5]["paragraphs"][0]["ru"])
            # Only the generated copy changes.
            self.assertEqual(canonical_sha, {
                name: hashlib.sha256((SOURCE / name).read_bytes()).hexdigest()
                for name in BOOKS})


if __name__ == "__main__":
    unittest.main()
