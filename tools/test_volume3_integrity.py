"""Fault-injection checks for the source/translation checkpoint gate."""
import copy
import json
from pathlib import Path
import tempfile
import unittest

from tools.validate_volume3 import DATA, validate
from tools.volume3_source import extract


class Volume3IntegrityTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.data = Path(self.temp.name)
        for name in ["protected-assets.json", "source-pages.jsonl", "translation.json", "self-review.json"]:
            if (DATA / name).exists():
                (self.data / name).write_bytes((DATA / name).read_bytes())
        self.translation = json.loads((self.data / "translation.json").read_text())

    def write_translation(self):
        (self.data / "translation.json").write_text(json.dumps(self.translation))

    def test_partial_checkpoint_cannot_pass_completeness_gate(self):
        self.assertFalse(validate(self.data)["complete"])
        with self.assertRaisesRegex(ValueError, "INCOMPLETE"):
            validate(self.data, require_complete=True)

    def test_duplicate_translation_rejected(self):
        self.translation["paragraphs"].append(copy.deepcopy(self.translation["paragraphs"][0]))
        self.write_translation()
        with self.assertRaisesRegex(ValueError, "Duplicate"):
            validate(self.data)

    def test_skipped_heading_rejected(self):
        self.translation["paragraphs"].pop(0)
        self.write_translation()
        with self.assertRaisesRegex(ValueError, "skips or reorders"):
            validate(self.data)

    def test_blank_translation_rejected(self):
        self.translation["paragraphs"][0]["ru"] = " "
        self.write_translation()
        with self.assertRaisesRegex(ValueError, "Empty translation"):
            validate(self.data)

    def test_duplicate_source_page_rejected(self):
        p = self.data / "source-pages.jsonl"
        first = p.read_text().splitlines()[0]
        p.write_text(p.read_text() + first + "\n")
        with self.assertRaisesRegex(ValueError, "Duplicate or unordered"):
            validate(self.data)

    def test_modern_notes_excluded_and_author_negation_kept(self):
        html = '''<div class="book-content-texts"><p class="selectionShareable">
        لمن لم <a title="Protected modern commentary" data-margin="1">(1)</a>
        يسألك عنه</p></div><input class="page-input" value="6">'''
        row = extract(html, 6)
        self.assertEqual(row["paragraphs"][0]["ar"], "لمن لم يسألك عنه")
        self.assertEqual(row["excluded_editorial_note_refs"], 1)
        self.assertNotIn("commentary", json.dumps(row))
        with self.assertRaisesRegex(ValueError, "identity mismatch"):
            extract(html, 7)


if __name__ == "__main__":
    unittest.main()
