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
}
