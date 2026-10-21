"""Mutation tests for missing pages, chapters, navigation and text corruption."""
import copy
import json
import unittest

from tools.validate_volume3_app import ASSETS, validate


class Volume3PackagingTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.bundle = json.loads((ASSETS / "volume3-draft.json").read_text())
        cls.structure = json.loads((ASSETS / "volume3-structure.json").read_text())

    def rejected(self, change):
        bundle, structure = copy.deepcopy(self.bundle), copy.deepcopy(self.structure)
        change(bundle, structure)
        with self.assertRaises(ValueError):
            validate(bundle, structure)

    def test_complete_working_text_loads_with_explicit_unresolved_crux(self):
        report = validate(self.bundle, self.structure)
        self.assertEqual(582, report["author_pages"])
        self.assertEqual(3096, report["author_paragraphs"])
        self.assertEqual(["v3-p110-04", "v3-p246-03", "v3-p246-05", "v3-p247-03"],
                         report["unresolved_source_ids"])

    def test_missing_chapter_is_rejected(self):
        self.rejected(lambda b, s: b["chapters"].pop(7))

    def test_duplicate_paragraph_is_rejected(self):
        self.rejected(lambda b, s: b["chapters"][0]["paragraphs"].append(b["chapters"][0]["paragraphs"][0]))

    def test_missing_heading_link_is_rejected(self):
        self.rejected(lambda b, s: s["groups"][0]["items"][0]["topics"].pop())

    def test_cross_chapter_link_is_rejected(self):
        self.rejected(lambda b, s: s["groups"][0]["items"][0].update(entryParagraphId=b["chapters"][1]["paragraphs"][0]["id"]))

    def test_silent_text_edit_is_rejected(self):
        self.rejected(lambda b, s: b["chapters"][0]["paragraphs"][2].update(ru="Сокращённый пересказ"))

    def test_lost_page_binding_is_rejected(self):
        self.rejected(lambda b, s: b["chapters"][0]["paragraphs"][2].update(printed_pages=[4]))

    def test_unsupported_publication_claim_is_rejected(self):
        self.rejected(lambda b, s: b.update(publication_ready=True))

    def test_unresolved_passages_cannot_be_relabelled_reviewed(self):
        for sid in self.bundle["unresolved_source_ids"]:
            with self.subTest(source_id=sid):
                def change(b, s):
                    for c in b["chapters"]:
                        for p in c["paragraphs"]:
                            if p["id"] == sid + "-ru":
                                p["status"] = "draft_self_checked"
                self.rejected(change)

    def test_own_note_cannot_be_imported_as_author(self):
        def change(b, s):
            for c in b["chapters"]:
                for p in c["paragraphs"]:
                    if p["role"] == "editor_note":
                        p["role"] = "author"
                        return
        self.rejected(change)


if __name__ == "__main__":
    unittest.main()
