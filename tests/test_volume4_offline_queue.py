"""Repeatable, no-key review-queue tests."""
import json
import tempfile
import unittest
from pathlib import Path
from test_volume4_auto_review import fixture
from tools.volume4_auto_review import build_queue
from tools.volume4_offline_queue import run_one


class OfflineTests(unittest.TestCase):
    def test_resume_until_empty(self):
        with tempfile.TemporaryDirectory() as tmp:
            r = Path(tmp); source = r / "source"; state = r / "state"
            fixture(source)
            self.assertIn("V4-LEX-046", run_one(source, state))
            self.assertIn("V4-SYNTAX-166", run_one(source, state))
            self.assertIn("IDLE", run_one(source, state))
            files = list((state / "reviews/volume4/offline-packets").glob("*.json"))
            self.assertEqual(len(files), 2)
            doc = json.loads(files[0].read_text())
            self.assertEqual(doc["state"], "STRUCTURAL_CHECK_ONLY")
            self.assertFalse(doc["is_ai_review"])
            self.assertFalse(doc["human_approved"])
            self.assertNotIn("text", doc["arabic"][0])

    def test_source_change_stops(self):
        with tempfile.TemporaryDirectory() as tmp:
            r = Path(tmp); source = r / "source"; state = r / "state"
            fixture(source); run_one(source, state)
            p = source / "research/volume4/source-pages.jsonl"
            p.write_text(p.read_text().replace("الصلف", "الفصل"))
            with self.assertRaisesRegex(ValueError, "inconsistent"):
                run_one(source, state)

    def test_missing_translation_blocks(self):
        with tempfile.TemporaryDirectory() as tmp:
            r = Path(tmp); source = r / "source"; state = r / "state"
            fixture(source, missing=True)
            with self.assertRaisesRegex(ValueError, "incomplete"):
                run_one(source, state)
            self.assertFalse(list(state.rglob("*.json")))

    def test_prior_ai_report_not_overwritten(self):
        with tempfile.TemporaryDirectory() as tmp:
            r = Path(tmp); source = r / "source"; state = r / "state"
            fixture(source)
            existing = build_queue(source, state)
            f = state / "reviews/volume4/V4-LEX-046.json"
            f.parent.mkdir(parents=True)
            f.write_text(json.dumps({"source_fingerprint": existing[0][2]}))
            self.assertIn("V4-SYNTAX-166", run_one(source, state))
            self.assertFalse((state / "reviews/volume4/offline-packets/V4-LEX-046.json").exists())


if __name__ == "__main__":
    unittest.main()
