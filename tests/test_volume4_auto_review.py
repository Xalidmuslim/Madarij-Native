"""Offline tests for resumable review; no paid model calls."""
import json
import tempfile
import unittest
from pathlib import Path
from tools.volume4_auto_review import build_queue, perform_one, extract_text


def fixture(root, missing=False):
    p = root / "research" / "volume4"
    p.mkdir(parents=True)
    (p / "progress.json").write_text(json.dumps({
        "draft_complete_pages": 554, "author_pages_total": 554,
        "open_issue_ids": ["V4-LEX-046", "V4-SYNTAX-166"]}))
    (p / "editorial-issues.json").write_text(json.dumps({"issues": [
        {"id": "V4-LEX-046", "source_id": "v4-p046-03",
         "status": "open_contextual_lexical_question", "finding": "Meaning?"},
        {"id": "V4-SYNTAX-166", "source_id": "v4-p166-01",
         "status": "open_control_syntax_variant", "finding": "Syntax?"}]}))
    (p / "translation.json").write_text(json.dumps({"paragraphs": [
        {"id": "a-ru", "source_ids": ["v4-p046-03"], "ru": None if missing else "Перевод."},
        {"id": "b-ru", "source_ids": ["v4-p166-01"], "ru": "Ещё перевод."}]}))
    (p / "source-pages.jsonl").write_text(
        json.dumps({"paragraphs": [{"id": "v4-p046-03", "ar": "الصلف"}]}, ensure_ascii=False) + "\n"
        + json.dumps({"paragraphs": [{"id": "v4-p166-01", "ar": "كِتَابٌ"}]}, ensure_ascii=False) + "\n")


class ResumeTests(unittest.TestCase):
    def test_disabled_is_safe(self):
        with tempfile.TemporaryDirectory() as t:
            p = Path(t)
            self.assertEqual(perform_one(p, p, False, "", "x", lambda *a: self.fail()), "DISABLED")

    def test_resumes_and_stops_without_duplicate_billing(self):
        with tempfile.TemporaryDirectory() as t:
            root = Path(t); source = root / "src"; state = root / "state"
            fixture(source)
            calls = []
            def ask(*args):
                calls.append(args)
                return "report"
            self.assertIn("V4-LEX-046", perform_one(source, state, True, "fake", "m", ask))
            self.assertIn("V4-SYNTAX-166", perform_one(source, state, True, "fake", "m", ask))
            self.assertIn("IDLE", perform_one(source, state, True, "fake", "m", ask))
            self.assertEqual(len(calls), 4)
            report = json.loads((state / "reviews/volume4/V4-LEX-046.json").read_text())
            self.assertEqual(report["status"], "NEEDS_HUMAN_REVIEW")
            self.assertFalse(report["human_approved"])

    def test_modified_source_blocks_resume(self):
        with tempfile.TemporaryDirectory() as t:
            root = Path(t); source = root / "src"; state = root / "state"
            fixture(source)
            perform_one(source, state, True, "fake", "m", lambda *a: "review")
            p = source / "research/volume4/editorial-issues.json"
            d = json.loads(p.read_text()); d["issues"][0]["finding"] = "changed"
            p.write_text(json.dumps(d))
            with self.assertRaisesRegex(ValueError, "changed source"):
                build_queue(source, state)

    def test_missing_draft_is_not_billed(self):
        with tempfile.TemporaryDirectory() as t:
            root = Path(t); source = root / "src"; state = root / "state"
            fixture(source, missing=True)
            with self.assertRaisesRegex(ValueError, "incomplete"):
                perform_one(source, state, True, "fake", "m", lambda *a: self.fail("called"))

    def test_failed_request_leaves_checkpoint_unmodified(self):
        with tempfile.TemporaryDirectory() as t:
            root = Path(t); source = root / "src"; state = root / "state"
            fixture(source)
            def fail(*args):
                raise RuntimeError("failed")
            with self.assertRaises(RuntimeError):
                perform_one(source, state, True, "fake", "m", fail)
            self.assertFalse(list(state.rglob("*.json")))

    def test_incomplete_model_reply_rejected(self):
        with self.assertRaises(ValueError):
            extract_text({"status": "incomplete", "output": []})


if __name__ == "__main__":
    unittest.main()
