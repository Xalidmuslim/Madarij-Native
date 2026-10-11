#!/usr/bin/env python3
"""Source-faithfulness regression checks: these must fail before restoring page text."""
import json,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
class FaithfulRendering(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  b=json.loads((ROOT/"app/src/main/assets/volume2-preview.json").read_text(encoding="utf8"))
  cls.book=b
  cls.p={p["id"]:p for ch in b["chapters"] for p in ch["paragraphs"]}
 def page(self,n):return self.p[f"madarij-v2-p{n:03d}-draft"]["ru"].lower()
 def test_271_contains_explicit_enemy_allegiance_and_disputing_divine_will(self):
  t=self.page(271)
  self.assertIn("заключает с ним союз",t)
  self.assertIn("противопоставляет тому, чего господь желает",t)
 def test_407_has_five_separate_citations_not_one_vague_group(self):
  t=self.page(407)
  for label in ("(65:5)","(65:2)","(65:4)","(4:69)","(65:3)"):
   with self.subTest(label=label):self.assertIn(label,t)
 def test_440_keeps_imagination_vs_reason_and_earlier_vs_later_states(self):
  t=self.page(440)
  self.assertIn("спор между выводом разума и предположением воображения",t)
  self.assertIn("древние времена",t.replace("давние времена","древние времена"))
 def test_444_explicit_human_role_as_instrument(self):
  self.assertIn("средством осуществления этого предания",self.page(444))
 def test_446_keeps_explicit_verse_4_25_and_clipped_original_quote(self):
  t=self.page(446)
  self.assertIn("(4:25)",t)
  self.assertIn("арабская цитата на этой печатной странице обрывается",t)
 def test_468_preserves_prohibition_protecting_positive_commands(self):
  self.assertIn("запрещено то, что ослабляет исполнение предписанного",self.page(468))
 def test_478_four_distinctions_and_conscious_subordination_to_prophet(self):
  t=self.page(478)
  self.assertIn("четыре положения",t)
  self.assertIn("ни в вопросах имён",t)
 def test_479_contains_analogy_to_dry_ablution_and_arabic_antithesis(self):
  t=self.page(479)
  self.assertIn("тайаммума",t)
  self.assertIn("содержимое могил",t)
  self.assertIn("временная дружба",t)
 def test_497_lists_quran_passages_and_full_divine_attributes(self):
  t=self.page(497)
  self.assertIn("сжимающим и расширяющим",t)
  self.assertIn("(89:27–28)",t)
  self.assertIn("(98:8)",t)
 def test_498_cites_all_three_anam_verses_separately(self):
  t=self.page(498)
  for ref in ("(6:114)","(6:14)","(6:164)"):
   self.assertIn(ref,t)
 def test_book_keeps_all_prior_pages_and_nonpublication_flags(self):
  self.assertEqual(len(self.book["chapters"]),9)
  self.assertEqual(sum(len(c["paragraphs"]) for c in self.book["chapters"]),676)
  self.assertFalse(self.book["translation_independently_verified"])
  self.assertFalse(self.book["publication_ready"])
if __name__=="__main__":unittest.main()
