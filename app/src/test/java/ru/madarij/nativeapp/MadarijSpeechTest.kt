package ru.madarij.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.madarij.nativeapp.data.Paragraph

class MadarijSpeechTest {
    private fun p(id: String, text: String, role: String = "body") =
        Paragraph(id, "c1", 0, "بسم الله", text, role, "")

    @Test fun speechCoversAllVisibleRussianParagraphsWithoutArabicOrEditorialMarkers() {
        val paragraphs = listOf(p("a", "Первый абзац."), p("b", "  "),
            p("c", "**Второй** абзац."), p("d", "Примечание", "editor_note"))
        val chunks = buildMadarijSpeech("Название главы", paragraphs, 200)
        assertEquals(listOf("Название главы", "Первый абзац.", "Второй абзац.", "Примечание"), chunks.map { it.text })
        assertFalse(chunks.any { it.text.contains("بسم") })
    }

    @Test fun speechKeepsLongTextBoundedAndPreservesWords() {
        val input = (1..100).joinToString(" ") { "слово$it" }
        val chunks = buildMadarijSpeech("Глава", listOf(p("a", input)), 90)
        assertTrue(chunks.size > 3)
        assertTrue(chunks.all { it.text.length <= 90 && it.text.isNotBlank() })
        assertEquals(input, chunks.drop(1).joinToString(" ") { it.text })
    }

    @Test fun selectableVoiceUsesExactRuVoiceCatalogAndNoAndroidFallback() {
        assertEquals("ru.kost.ruvoice", RUVOICE_PACKAGE)
        assertEquals("aidar-ru", VoiceChoice.AIDAR.voiceName)
        assertEquals("eugene-ru", VoiceChoice.EUGENE.voiceName)
        assertTrue(isRuVoiceCatalog(listOf("aidar-ru", "eugene-ru")))
        assertFalse(isRuVoiceCatalog(listOf("ru-ru-x-google", "en-us-google")))
        assertEquals(1.0f, NATURAL_SPEECH_RATE, 0.00001f)
    }
}
