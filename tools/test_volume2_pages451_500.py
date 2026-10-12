#!/usr/bin/env python3
"""Content regression tests for volume 2 pages 451-500 (not scholarly certification)."""
import json
import unittest
from pathlib import Path

ASSET=Path(__file__).resolve().parents[1]/"app/src/main/assets/volume2-preview.json"

class Vol2Pages451500(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.book=json.loads(ASSET.read_text(encoding="utf-8"))
        cls.paras={p["id"]:p for ch in cls.book["chapters"] for p in ch["paragraphs"]}
    def p(self,n):
        return self.paras[f"madarij-v2-p{n:03d}-draft"]

    def test_quran_16_127_no_verse_leakage(self):
        self.assertNotIn("{(١٢٦)",self.p(453)["ar"])
        self.assertIn("{وَاصْبِرْ وَمَا صَبْرُكَ إِلَّا بِاللَّهِ} [النحل: ١٢٧]",self.p(453)["ar"])

    def test_quran_89_29_and_98_8_restored(self):
        self.assertNotIn("الصَّرْحَ فِي عِبَادِي",self.p(489)["ar"])
        self.assertIn("فَادْخُلِي فِي عِبَادِي",self.p(489)["ar"])
        self.assertNotIn("{(٧) عِنْدَ",self.p(497)["ar"])
        self.assertIn("{عِنْدَ رَبِّهِمْ جَنَّاتُ عَدْنٍ",self.p(497)["ar"])

    def test_preserve_isnad_instead_of_modern_summary(self):
        ru=self.p(459)["ru"]
        self.assertIn("Мусу ибн Исмаила",ru)
        self.assertIn("Сувайда",ru)
        self.assertIn("Абдуллаха ибн Убайда ибн Умайра",ru)
        self.assertNotIn("Автор указывает цепочку",ru)

    def test_no_intrusive_author_narration(self):
        bad={
            455:["Поэт сказал:"],456:["Один поэт сказал:"],
            461:["Поэт писал:","Поэт сказал:"],
            466:["По мнению автора"],468:["По мысли автора"],
            470:["Поэт сказал:"],471:["Автор полагает","Поэт сказал:"],
            472:["Но автор считает"],473:["Автор приводит сравнение"],
            477:["Автор считает эти сообщения"],
            480:["по мнению автора"],
            481:["По мысли автора"],
            489:["по мнению автора","В извлечённом арабском тексте"],
            490:["автор считает более естественным"],
            493:["Автор рассматривает"],
            495:["Однако автор считает"],
            496:["по мнению автора"],
        }
        for page,phrases in bad.items():
            for phrase in phrases:
                with self.subTest(page=page,phrase=phrase):
                    self.assertNotIn(phrase,self.p(page)["ru"])

    def test_fuller_theological_and_fiqh_wording(self):
        self.assertIn("Оба мнения присутствуют в мазхабе имама Ахмада",self.p(496)["ru"])
        self.assertIn("Снабжающим необходимым",self.p(497)["ru"])

    def test_preserve_previous_repair_and_draft(self):
        self.assertEqual(len(self.book["chapters"]),9)
        self.assertEqual(len(self.paras),676)
        self.assertFalse(self.book["publication_ready"])
        self.assertFalse(self.book["translation_independently_verified"])
        self.assertIn("эта мысль является одним из порицаемых утверждений",self.p(463)["ru"])
        self.assertIn("пока не вкусили Нашего наказания",self.p(507)["ru"])

if __name__=="__main__":
    unittest.main()
