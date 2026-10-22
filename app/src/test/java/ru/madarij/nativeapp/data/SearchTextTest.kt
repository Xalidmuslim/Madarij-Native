package ru.madarij.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTextTest {
    @Test fun quotedRussianTermCanBeFoundByItsBareWord() {
        val indexWords = normalizeSearchText("по модели «муфааля» (مفاعلة)").split(" ")
        assertTrue(indexWords.contains(normalizeSearchText("муфааля")))
    }

    @Test fun compoundNameCanBeFoundWithoutItsTypographicHyphenOrQuotes() {
        val indexWords = normalizeSearchText("имя «аль‑Вадуд»").split(" ")
        assertTrue(indexWords.containsAll(listOf("аль", "вадуд")))
    }

    @Test fun arabicWordCanBeFoundWithoutQuranOrnamentDelimiters() {
        val indexWords = normalizeSearchText("﴿الكِتَابُ﴾").split(" ")
        assertTrue(indexWords.contains(normalizeSearchText("الكتاب")))
    }

    @Test fun existingRussianAndArabicFoldingStillMatches() {
        assertEquals(normalizeSearchText("еще الإيمان"), normalizeSearchText("ЕЩЁ الإِيمَانُ"))
    }
}
