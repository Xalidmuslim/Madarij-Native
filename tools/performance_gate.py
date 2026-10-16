#!/usr/bin/env python3
"""Conservative emulator-only regression gate, not a physical-device FPS claim.

Baseline: known-good optimized R8 build #502, Pixel 6 API 35 with SwiftShader.
Cold am-start TotalTime: 4.29, 4.37, 3.08 seconds; reader gfxinfo:
42 rendered frames, 29 janky (69.05%) and 48 ms median.

Software rendering and Android emulator hosts are variable; tolerances
catch severe regressions while requiring real navigation and frame evidence.
Maintainers must still test physical devices before asserting silky smooth UI.
"""
import argparse
from pathlib import Path
import re
import statistics
import sys

BASELINE_COLD_MEDIAN_MS = 4290
BASELINE_JANK_PCT = 69.05
BASELINE_FRAME_P50_MS = 48

def summarize(measurements: str, gfxinfo: str):
    cold = re.findall(r"TotalTime:\s*(\d+)", measurements)
    starts = [int(s) for s in cold[:5]]
    if len(starts) < 3 or any(t <= 0 for t in starts):
        raise ValueError("At least three valid cold launches are required")
    frame = re.search(r"Total frames rendered:\s*([\d,]+)", gfxinfo)
    jank = re.search(r"Janky frames:\s*([\d,]+)\s*\(([\d.]+)%\)", gfxinfo)
    median = re.search(r"50th percentile:\s*(\d+)ms", gfxinfo)
    if not frame or not jank or not median:
        raise ValueError("Real reader gfxinfo counters are required")
    frames = int(frame.group(1).replace(",", ""))
    skipped = int(jank.group(1).replace(",", ""))
    percentage = float(jank.group(2))
    frame_p50 = int(median.group(1))
    if frames < 20 or skipped > frames or not (0 <= percentage <= 100):
        raise ValueError("Invalid or insufficient reader rendering frames")
    if abs(percentage - 100 * skipped / frames) > 0.2:
        raise ValueError("Inconsistent frame counters")
    cold_median = statistics.median(starts)
    return {
        "cold_ms": starts,
        "cold_median_ms": cold_median,
        "frames": frames,
        "janky_frames": skipped,
        "jank_pct": percentage,
        "frame_p50_ms": frame_p50,
    }

def evaluate(metrics):
    # 502 was already janky on *software* rendering, so do not assert 60fps.
    # Catch only material degradations, not small CI host fluctuations.
    issues = []
    if metrics["cold_median_ms"] > 1.5 * BASELINE_COLD_MEDIAN_MS:
        issues.append("Cold-start median >150% of reference #502")
    if metrics["jank_pct"] > min(95, 1.35 * BASELINE_JANK_PCT):
        issues.append("Reader jank >135% of reference #502")
    if metrics["frame_p50_ms"] > 2 * BASELINE_FRAME_P50_MS:
        issues.append("Reader p50 frame time >200% of reference #502")
    return issues

def report(metrics, issues):
    return (
        "# Performance regression check: Android API 35 / Pixel 6 / SwiftShader\n\n"
        "| Measure | Optimized #502 reference | Current R8 build |\n"
        "|---|---:|---:|\n"
        f"| Cold-start median | {BASELINE_COLD_MEDIAN_MS} ms | {metrics['cold_median_ms']:.0f} ms |\n"
        f"| Reader janky frames | {BASELINE_JANK_PCT:.2f}% | {metrics['jank_pct']:.2f}% "
        f"({metrics['janky_frames']}/{metrics['frames']}) |\n"
        f"| Reader median frame time | {BASELINE_FRAME_P50_MS} ms | {metrics['frame_p50_ms']} ms |\n\n"
        f"Verdict: **{'FAIL' if issues else 'PASS (gross regression only)'}**.\n\n"
        + ("Failures: " + "; ".join(issues) + "\n\n" if issues else "")
        + "These are emulator-software-GPU observations, NOT verified 60/120 FPS, "
        "not physical-device benchmarks, and not an apples-to-apples baseline "
        "from identical concurrent CI hosts. A passing result cannot certify "
        "unchanged responsiveness on the owner's phone.\n"
    )

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--measurements", required=True)
    p.add_argument("--gfxinfo", required=True)
    p.add_argument("--report", required=True)
    args = p.parse_args()
    try:
        metrics = summarize(
            Path(args.measurements).read_text(encoding="utf-8"),
            Path(args.gfxinfo).read_text(encoding="utf-8"),
        )
        issues = evaluate(metrics)
        result = report(metrics, issues)
    except (OSError, ValueError) as exc:
        result = f"# Performance regression check: FAIL\n\nInvalid measurement: {exc}\n"
        issues = [str(exc)]
    Path(args.report).parent.mkdir(parents=True, exist_ok=True)
    Path(args.report).write_text(result, encoding="utf-8")
    print(result)
    return 1 if issues else 0

if __name__ == "__main__":
    sys.exit(main())
