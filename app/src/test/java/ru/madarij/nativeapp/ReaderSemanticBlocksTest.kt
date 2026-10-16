package ru.madarij.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderSemanticBlocksTest {
    @Test fun verseWithReferenceIsSeparateFromAuthorExplanation() {
        val input = "Автор разъясняет этот смысл. Всевышний сказал: «Это — прямой путь ко Мне». [Аль-Хиджр, 15:41] Затем он продолжает рассуждение."
        val list = parseReaderRichSegments(input)
        val block = list.filterIsInstance<ReaderRichSegment.Quote>().single()
        assertEquals(ReaderCitationKind.VERSE, block.kind)
        assertTrue(block.body.contains("«Это — прямой путь ко Мне»"))
        assertTrue(block.body.contains("[Аль-Хиджр, 15:41]"))
        assertTrue(list.last() is ReaderRichSegment.Paragraph)
        assertEquals(input, list.joinToString(" ") { it.raw })
    }

    @Test fun scholarWordsAreNeverCalledPropheticReport() {
        val list = parseReaderRichSegments("Аль-Хасан сказал: «Знание требует действия».")
        val block = list.filterIsInstance<ReaderRichSegment.Quote>().single()
        assertEquals(ReaderCitationKind.SAYING, block.kind)
        assertTrue(block.body.contains("«Знание требует действия»"))
    }

    @Test fun propheticReportIsExplicitOnly() {
        val list = parseReaderRichSegments("Пророк сказал: «Дела оцениваются по намерениям».")
        assertEquals(
            ReaderCitationKind.PROPHETIC_REPORT,
            list.filterIsInstance<ReaderRichSegment.Quote>().single().kind
        )
        val plain = parseReaderRichSegments("Он сказал об этой книге, что надо размышлять.")
        assertTrue(plain.all { it is ReaderRichSegment.Paragraph })
    }

    @Test fun numberedItemsPreserveTextAndOrder() {
        val input = "Здесь возможны два объяснения. Первое: взаимоотношение слов. Второе: смысловое толкование."
        val list = parseReaderRichSegments(input)
        assertEquals(2, list.count { it is ReaderRichSegment.Numbered })
        assertEquals(input, list.joinToString(" ") { it.raw })
    }

    @Test fun quotationAndOrdinaryTextAreNotDuplicated() {
        val input = "Здесь обычный текст. Аль-Хасан сказал: «Истина ясна». Затем следует объяснение."
        val list = parseReaderRichSegments(input)
        assertEquals(input, list.joinToString(" ") { it.raw })
    }
}
