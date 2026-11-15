#!/usr/bin/env python3
"""Prepare one reproducible review packet with no AI or paid services."""
import argparse
import hashlib
import json
import os
import re
import sys
import tempfile
from pathlib import Path
from tools.volume4_auto_review import build_queue, json_object

FOLDER = Path("reviews/volume4/offline-packets")


def sha(value):
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def packet(issue_id, evidence, fingerprint):
    return {
        "issue_id": issue_id,
        "state": "STRUCTURAL_CHECK_ONLY",
        "is_ai_review": False,
        "is_translation": False,
        "human_approved": False,
        "source_fingerprint": fingerprint,
        "source_commit": "943f2449774a6f780707efe5ac013b042ea92619",
        "existing_finding": evidence["finding"],
        "existing_status": evidence["status"],
        "arabic": [
            {
                "source_id": item["source_id"],
                "sha256": sha(item["text"]),
                "characters": len(item["text"]),
                "contains_arabic_script": bool(re.search(r"[\u0621-\u064A]", item["text"])),
            }
            for item in evidence["arabic"]
        ],
        "russian": [
            {
                "id": item["id"], "source_ids": item["source_ids"],
                "sha256": sha(item["text"]),
                "characters": len(item["text"]),
                "contains_cyrillic": bool(re.search(r"[\u0400-\u04FF]", item["text"])),
            }
            for item in evidence["existing_russian"]
        ],
        "next_action": "Needs human or authorized AI semantic review. No edits performed.",
        "limitations": "Structural IDs, content presence and hashes only. No language model, PDF comparison or source authentication.",
    }


def run_one(source_root, state_root):
    queue = build_queue(Path(source_root), Path(state_root))
    folder = Path(state_root) / FOLDER
    for issue_id, evidence, fingerprint, _ in queue:
        output = folder / (issue_id + ".json")
        if output.is_file():
            saved = json_object(output)
            if saved.get("source_fingerprint") != fingerprint or saved.get("state") != "STRUCTURAL_CHECK_ONLY":
                raise ValueError("Existing packet inconsistent with source: " + issue_id)
            continue
        folder.mkdir(parents=True, exist_ok=True)
        with tempfile.NamedTemporaryFile(
            "w", encoding="utf-8", dir=folder, prefix=".pending-", delete=False
        ) as f:
            json.dump(packet(issue_id, evidence, fingerprint), f, ensure_ascii=False, indent=2)
            f.write("\n")
            tmp = f.name
        os.replace(tmp, output)
        return "PACKET_CREATED: " + issue_id
    return "IDLE: every eligible issue already has a packet or AI review"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-root", type=Path, required=True)
    parser.add_argument("--state-root", type=Path, required=True)
    args = parser.parse_args()
    print(run_one(args.source_root, args.state_root))


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError) as exc:
        print("STOPPED:", exc, file=sys.stderr)
        sys.exit(1)
