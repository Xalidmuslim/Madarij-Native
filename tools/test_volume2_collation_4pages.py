#!/usr/bin/env python3
"""Regression tests for source-based translation omissions and Qur'an OCR damage."""
import json,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
class CollationRegression(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.book=json.loads((ROOT/"app/src/main/assets/volume2-preview.json").read_text(encoding="utf8"))
  cls.pars={p["id"]:p for c in cls.book["chapters"] for p in c["paragraphs"]}
 def test_448_contains_full_quran_excerpt_coverage(self):
  s=self.pars["madarij-v2-p448-draft"]["ru"].lower()
  for v in ("между которым и тобой была вражда","мы сделали их предметом рассказов","если пожелает, он успокоит ветер"):
   with self.subTest(phrase=v):self.assertIn(v,s)
 def test_507_quotes_all_three_revelatory_passages(self):
  s=self.pars["madarij-v2-p507-draft"]["ru"].lower()
  for v in ("пока не вкусили нашего наказания","мы не поклонялись бы помимо него","нет у них об этом никакого знания"):
   with self.subTest(phrase=v):self.assertIn(v,s)
 def test_463_retains_the_actual_opponents_position_before_rebuttal(self):
  s=self.pars["madarij-v2-p463-draft"]["ru"].lower()
  self.assertIn("терпение возвращает вещи к душе",s)
  self.assertIn("эта мысль является одним из порицаемых утверждений",s)
 def test_622_canonical_quran_quotations_are_no_longer_truncated(self):
  a=self.pars["madarij-v2-p622-draft"]["ar"]
  self.assertIn("وَالَّذِينَ هُمْ مُحْسِنُونَ",a)
  self.assertIn("إِنَّ اللَّهَ مَعَ الصَّابِرِينَ",a)
 def test_source_navigator_and_unverified_status_preserved(self):
  v=self.book
  self.assertEqual(len(v["chapters"]),9)
  self.assertEqual(sum(len(c["paragraphs"]) for c in v["chapters"]),676)
  self.assertFalse(v["translation_independently_verified"])
  self.assertFalse(v["publication_ready"])
if __name__=="__main__":unittest.main()
