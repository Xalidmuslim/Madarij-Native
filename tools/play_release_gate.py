#!/usr/bin/env python3
"""Fail-closed policy/source provenance gate for public Play bundle generation.

QA candidate AABs may be created with an isolated application ID and a
development signing key. They are NOT ready for Play publication.
Production mode requires independently recorded documentary evidence.
"""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read_clearance():
    obj = json.loads((ROOT / "docs/play-clearance.json").read_text(encoding="utf-8"))
    assert obj["schema"] == 1
    assert obj["checks"]
    assert set(obj["checks"]) == set(obj["evidence"])
    return obj

def blockers(obj):
    return [name for name, passed in obj["checks"].items()
            if passed is not True or not str(obj["evidence"].get(name, "")).strip()]

def check_source_inventory():
    obj = json.loads((ROOT / "research/volume2-source/source_audit.json").read_text(encoding="utf-8"))
    assert obj["pages_retrieved"] == 659
    assert "Nabil" in obj["print_edition"]
    # This inventory is explicit that modern editorial layers are separated
    # and source scans were not yet fully visually verified; never infer more.
    return obj

def run(mode):
    clearance = read_clearance()
    inventory = check_source_inventory()
    pending = blockers(clearance)
    print("ARABIC SOURCE:", inventory["print_edition"])
    print("Modern editor notes separated on source pages:",
          inventory.get("pages_with_separated_editorial_notes", "unknown"))
    print("Visually verified pages in source inventory:",
          inventory.get("arabic_pages_visually_verified", "unknown"))
    print("CHECKS:", len(clearance["checks"]) - len(pending),
          "/", len(clearance["checks"]))
    if pending:
        print("Outstanding clearance:")
        for item in pending:
            print(" -", item)
    if mode == "production" and pending:
        raise SystemExit("BLOCKED: public Play AAB requires source, editorial, "
                         "religious-content and privacy reviews with documentary evidence")
    print("QA candidate only — not proof of publication rights or Play approval"
          if mode == "candidate" else "Documentary release gate passed")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--mode", choices=("candidate", "production"), required=True)
    run(parser.parse_args().mode)
