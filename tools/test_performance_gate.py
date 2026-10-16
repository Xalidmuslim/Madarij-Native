import unittest
from tools.performance_gate import summarize, evaluate, report

class PerformanceGateTest(unittest.TestCase):
    def setUp(self):
        self.reference = "".join(
            f"---- Cold start {i} ----\nStatus: ok\nTotalTime: {time}\n"
            for i, time in enumerate([4290, 4370, 3080, 4300, 4020], 1)
        )
        self.gfx = (
            "Total frames rendered: 42\n"
            "Janky frames: 29 (69.05%)\n"
            "50th percentile: 48ms\n"
            "90th percentile: 192ms\n"
        )

    def test_reference_accepted(self):
        data = summarize(self.reference, self.gfx)
        self.assertEqual(4290, data["cold_median_ms"])
        self.assertEqual(42, data["frames"])
        self.assertEqual([], evaluate(data))
        self.assertIn("Optimized #502 reference", report(data, []))

    def test_extreme_cold_start_regression_fails(self):
        timings = self.reference.replace("4290", "10000").replace(
            "4370", "13000").replace("3080", "11000").replace(
            "4300", "14000").replace("4020", "12000")
        data = summarize(timings, self.gfx)
        self.assertIn("Cold-start median >150% of reference #502", evaluate(data))

    def test_raster_regression_fails(self):
        data = summarize(self.reference, self.gfx.replace(
            "29 (69.05%)", "40 (95.24%)").replace("48ms", "101ms"))
        self.assertEqual(2, len(evaluate(data)))

    def test_rejects_unmeasured_or_unverified_reader(self):
        for invalid in ["", "Total frames rendered: 2\nJanky frames: 1 (50%)\n50th percentile: 50ms"]:
            with self.assertRaises(ValueError):
                summarize(self.reference, invalid)
        with self.assertRaises(ValueError):
            summarize("TotalTime: 9", self.gfx)

    def test_rejects_inconsistent_percentages(self):
        with self.assertRaises(ValueError):
            summarize(self.reference, self.gfx.replace("69.05%", "22.0%"))

if __name__ == "__main__":
    unittest.main()
