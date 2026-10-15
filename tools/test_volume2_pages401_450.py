#!/usr/bin/env python3
"""Regression tests for documented electronic source corrections, vol. 2 pp. 401–450.

Passing these tests is not scholarly print-scan or isnad certification.
"""
import json
import unittest
from pathlib import Path

BOOK = Path(__file__).resolve().parents[1] / "app/src/main/assets/volume2-preview.json"

class Volume2Pages401450(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.obj = json.loads(BOOK.read_text(encoding="utf-8"))
        cls.paras = {p["id"]: p for ch in cls.obj["chapters"] for p in ch["paragraphs"]}

    def page(self, n):
        return self.paras[f"madarij-v2-p{n:03d}-draft"]

    def test_author_tone_and_missing_nuance(self):
        check = {
            402: ["Автор считает такое распределение сил"],
            404: ["по мнению автора", "Автор отвечает"],
            408: ["Автор отмечает"],
            411: ["Автор считает постоянное обращение"],
            412: ["Поэт сказал:"],
            416: ["Автор считает, что необходимо"],
            423: ["в нескольких местах Корана", "автор приводит кораническое"],
            427: ["Автор подчёркивает"],
            437: ["по мнению автора"],
            438: ["автор считает"],
            443: ["Автор считает эту трактовку"],
            445: ["автор считает предметом", "автор рассматривает"],
        }
        for p, phrases in check.items():
            for needle in phrases:
                with self.subTest(page=p, needle=needle):
                    self.assertNotIn(needle, self.page(p)["ru"])

    def test_nuances_not_softened(self):
        self.assertIn("противодействие сторонникам религиозных нововведений", self.page(403)["ru"])
        self.assertIn("В основе своей просьбы к творениям запрещены", self.page(411)["ru"])
        self.assertIn("в четырёх местах Корана", self.page(423)["ru"])
        self.assertIn("единогласия общины", self.page(445)["ru"])
        self.assertIn("завесой", self.page(410)["ru"])

    def test_quran_quotes_preserved_and_restored(self):
        p445=self.page(445)["ar"]
        self.assertNotIn("{(١٥٢)", p445)
        self.assertNotIn("{(١٢٦)", p445)
        self.assertIn("{وَلَا تَهِنُوا} [آل عمران: ١٣٩]", p445)
        self.assertNotIn("تَهِنُوا فِي", p445)
        self.assertNotIn("لَهُمْ كَأَنَّهُمْ", p445)
        p446=self.page(446)["ar"]
        self.assertIn("مَا كَانُوا يَعْمَلُونَ} [النحل: ٩٦]", p446)
        self.assertIn("بِغَيْرِ حِسَابٍ} [الزمر: ١٠]", self.page(447)["ar"])

    def test_invariants(self):
        self.assertEqual(len(self.obj["chapters"]),9)
        self.assertEqual(len(self.paras),676)
        self.assertFalse(self.obj["publication_ready"])
        self.assertFalse(self.obj["translation_independently_verified"])
        for n in (448, 463, 507, 622):
            self.assertIn(f"madarij-v2-p{n:03d}-draft", self.paras)

if __name__=="__main__":
    unittest.main()
