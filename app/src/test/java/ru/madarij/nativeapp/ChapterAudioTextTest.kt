package ru.madarij.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.madarij.nativeapp.data.Paragraph

class ChapterAudioTextTest {
    private fun paragraph(index: Int, russian: String, role: String = "body") =
        Paragraph("p$index", "c1", index, "نص عربي", russian, role, "source")

    @Test fun voiceNamesAreRealRuVoiceMaleVoices() {
        assertEquals("ru.kost.ruvoice", RUVOICE_PACKAGE)
        assertEquals("aidar-ru", ChapterAudioVoice.AIDAR.engineName)
        assertEquals("eugene-ru", ChapterAudioVoice.EUGENE.engineName)
        assertTrue(ChapterAudioVoice.AIDAR.displayName != ChapterAudioVoice.EUGENE.displayName)
    }

    @Test fun readsRussianFromEveryVisibleBookParagraphButNotArabicOrNotes() {
        val ps = listOf(
            paragraph(0, "Первый фрагмент"),
            paragraph(1, "Примечание", "editor_note"),
            paragraph(2, "Второй фрагмент."),
            paragraph(3, "  ", "body"),
            paragraph(4, "Примечание издания", "edition_note")
        )
        val chunks = chapterSpeechChunks("Название раздела", ps, maxChars = 100)
        assertEquals(listOf("Название раздела", "Первый фрагмент", "Второй фрагмент."), chunks)
        assertFalse(chunks.joinToString().contains("نص عربي"))
    }

    @Test fun preservesRussianTextWhenSplittingLongParagraph() {
        val text = (1..230).joinToString(" ") { "слово$it" }
        val chunks = chapterSpeechChunks("Глава", listOf(paragraph(0, text)), maxChars = 90)
        assertTrue(chunks.size > 3)
        assertTrue(chunks.all { it.length <= 90 && it.isNotBlank() })
        assertEquals(text, chunks.drop(1).joinToString(" "))
    }

    @Test fun leavesLongTokensIntactEvenWhenSplitting() {
        val longToken = "т".repeat(204)
        val chunks = chapterSpeechChunks("Глава", listOf(paragraph(0, longToken)), maxChars = 70)
        assertTrue(chunks.all { it.length <= 70 })
        assertEquals(longToken, chunks.drop(1).joinToString(""))
    }

    @Test fun refuseSystemEngineVoices() {
        assertTrue(isSelectedRuVoice(listOf("aidar-ru", "eugene-ru")))
        assertFalse(isSelectedRuVoice(listOf("en-us-x-google", "ru-ru-x-google")))
        assertFalse(isSelectedRuVoice(emptyList()))
    }
}
