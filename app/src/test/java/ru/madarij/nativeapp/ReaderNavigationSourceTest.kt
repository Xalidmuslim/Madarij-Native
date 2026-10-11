package ru.madarij.nativeapp

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Regression guard for editorial navigation entering the author's reading text.
 * Not a font test: the visual review remains an emulator QA scenario.
 */
class ReaderNavigationSourceTest {
    @Test fun navigationTitlesAreNotInsertedIntoReaderBody() {
        val source = File("src/main/java/ru/madarij/nativeapp/Reader.kt").readText()
        assertTrue(!source.contains("topicsByParagraph[p.id]"))
        assertTrue(source.contains("ReaderFrontispiece(logicalChapters.take(chapterIndex + 1)"))
        // The ordinal now resets at each volume boundary.
        assertTrue(source.contains("items(sectionTopics, key ="))
    }
}
