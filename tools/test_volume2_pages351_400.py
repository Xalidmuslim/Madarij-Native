#!/usr/bin/env python3
"""Content regression checks for Volume II printed pages 351–400.

Electronic-source checks; NOT scanned-edition, hadith or theological sign-off.
"""
import json
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
BOOK=ROOT/"app/src/main/assets/volume2-preview.json"
class Volume2Pages351400(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.book=json.loads(BOOK.read_text(encoding="utf-8"))
        cls.p={p["id"]:p for c in cls.book["chapters"] for p in c["paragraphs"]}
    def page(self,n):
        return self.p[f"madarij-v2-p{n:03d}-draft"]
    def test_repaired_quran(self):
        self.assertIn("{وَلَوْلَا أَنْ ثَبَّتْنَاكَ لَقَدْ كِدْتَ تَرْكَنُ إِلَيْهِمْ شَيْئًا قَلِيلًا} [الإسراء: ٧٤]",self.page(352)["ar"])
        self.assertNotIn("{(٧٣) وَلَوْلَا",self.page(352)["ar"])
        self.assertIn("وَإِلَيْكَ الْمَصِيرُ} [الممتحنة: ٤]",self.page(381)["ar"])
        self.assertIn("к Тебе предстоит возвращение",self.page(381)["ru"])
    def test_authentic_author_voice(self):
        terms={360:["по мнению автора"],362:["по оценке автора"],374:["По описанию автора","автор считает"],380:["по мнению автора"],388:["по словам автора"],400:["Автор приводит ответ"]}
        for n,phrases in terms.items():
            for phrase in phrases:
                with self.subTest(page=n,phrase=phrase):
                    self.assertNotIn(phrase,self.page(n)["ru"])
    def test_p378_not_abridged(self):
        ru=self.page(378)["ru"]
        self.assertIn("собственное смирение",ru)
        self.assertIn("не в действительности",ru)
        self.assertIn("не приписывать духовные состояния",ru)
    def test_preserve_draft_and_old_revisions(self):
        self.assertEqual(len(self.book["chapters"]),9)
        self.assertEqual(len(self.p),676)
        self.assertFalse(self.book["publication_ready"])
        self.assertFalse(self.book["translation_independently_verified"])
        for n in (448,463,507,622):
            self.assertIn(f"madarij-v2-p{n:03d}-draft",self.p)
if __name__=="__main__":
    unittest.main()
