package ru.madarij.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderTextBlocksTest {
    @Test fun standaloneSectionMarkerAndRealTitleAreSeparatedFromBody() {
        val result = splitReaderText(
            "До раздела.\nРаздел\nО степенях наставления\nИх десять.\nПервая степень...",
            marker = "Раздел",
            label = "Раздел"
        )
        assertEquals(
            listOf(
                ReaderTextBlock.Body("До раздела."),
                ReaderTextBlock.Section(label = "Раздел", title = "О степенях наставления"),
                ReaderTextBlock.Body("Их десять.\nПервая степень...")
            ),
            result
        )
    }

    @Test fun sectionMarkerBeforeProseDoesNotPromoteFirstWordsToHeading() {
        val body = "Покаяние раба перед Всевышним Аллахом окружено обращением Аллаха к нему до него и обращением после него. Затем следует продолжение."
        val result = splitReaderText("До раздела.\nРаздел\n$body", marker = "Раздел", label = "Раздел")
        assertEquals(
            listOf(ReaderTextBlock.Body("До раздела."), ReaderTextBlock.Section(label = "Раздел"), ReaderTextBlock.Body(body)),
            result
        )
    }

    @Test fun arabicSectionMarkerKeepsFollowingTextAndDirection() {
        val body = "وتوبة العبد إلى الله تعالى محفوفة بتوبةٍ من الله عليه قبلها."
        assertEquals(
            listOf(ReaderTextBlock.Section(label = "فصل"), ReaderTextBlock.Body(body)),
            splitReaderText("فصل\n$body", marker = "فصل", label = "فصل")
        )
    }
    @Test fun markdownSectionFromSecondVolumeIsBookDividerNotLiteralStars() {
        val original = "Некоторые объясняют происхождение стыдливости иначе: состояние называется *хая*.\n\n**Раздел**\n\nАвтор «Маназиль» сказал: «У стыдливости три степени»."
        val result = splitReaderText(original, marker = "Раздел", label = "Раздел")
        assertEquals(3, result.size)
        assertEquals(ReaderTextBlock.Section(label = "Раздел"), result[1])
        assertEquals(
            "Некоторые объясняют происхождение стыдливости иначе: состояние называется *хая*.",
            (result[0] as ReaderTextBlock.Body).text
        )
        assertEquals(
            "Автор «Маназиль» сказал: «У стыдливости три степени».",
            (result[2] as ReaderTextBlock.Body).text
        )
    }

    @Test fun inlineMarkdownIsRemovedOnlyFromReaderDisplay() {
        val original = "Состояние *хая*. Жаловаться **Самому Аллаху**. "
        assertEquals("Состояние хая. Жаловаться Самому Аллаху. ", readerDisplayMarkdown(original))
        assertEquals("Состояние *хая*. Жаловаться **Самому Аллаху**. ", original)
        assertEquals("2 * 3 и *непарный", readerDisplayMarkdown("2 * 3 и *непарный"))
    }

    @Test fun headingMarkdownDoesNotSwallowFollowingBookText() {
        val source = "Текст до.\n\n**Раздел**\n\nПодлинное содержание продолжается.\n\nСледующий абзац."
        val parts = splitReaderText(source, marker = "Раздел", label = "Раздел")
        assertEquals(3, parts.size)
        assertEquals(ReaderTextBlock.Body("Текст до."), parts[0])
        assertEquals(ReaderTextBlock.Section(label = "Раздел"), parts[1])
        assertEquals(ReaderTextBlock.Body("Подлинное содержание продолжается.\n\nСледующий абзац."), parts[2])
    }

}
