package ru.madarij.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RussianOnlyTextTest {
    private val arabic = Regex("""[\u0600-\u06FF\u0750-\u077F\u0870-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]""")

    @Test fun verseShowsItsRussianTranslationAndReferenceOnly() {
        val original = "Аллах сказал: {قُلْ بِفَضْلِ اللَّهِ} — «Скажи: пусть они радуются». [«Юнус», 10:58]."
        val visible = russianOnlyText(original)
        assertEquals("Аллах сказал: «Скажи: пусть они радуются». [«Юнус», 10:58].", visible)
        assertFalse(arabic.containsMatchIn(visible))
    }

    @Test fun arabicTermsOutsideBracesDisappearButRussianWordsRemain() {
        val original = "Сердце — القلب, а путь — الطريق; это важное различие."
        val visible = russianOnlyText(original)
        assertEquals("Сердце, а путь; это важное различие.", visible)
        assertFalse(arabic.containsMatchIn(visible))
    }

    @Test fun originalRussianTranslationRemainsUnchanged() {
        val original = "«Знание и справедливость — основа всякого добра» (с. 556)."
        assertEquals(original, russianOnlyText(original))
    }

    @Test fun removesStandaloneArabicAndEmptyQuotations() {
        assertEquals("Русский перевод.", russianOnlyText("«السَّلَامُ» Русский перевод."))
    }

    @Test fun removesArabicWithoutDeletingSourceReferences() {
        val original = "Всевышний сказал: {فَلْيَفْرَحُوا} — «Пусть радуются» [«Юнус», 10:58]."
        val visible = russianOnlyText(original)
        assertTrue(visible.contains("Пусть радуются"))
        assertTrue(visible.contains("10:58"))
        assertFalse(arabic.containsMatchIn(visible))
    }
}
