package ru.madarij.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationPolicyTest {
    @Test fun readerAndSearchSelectBookWhileBookmarksSelectMore() {
        assertEquals("contents", selectedMainTab("read/{id}?paragraph={paragraph}&query={query}"))
        assertEquals("contents", selectedMainTab("search"))
        assertEquals("more", selectedMainTab("bookmarks"))
    }

    @Test fun primaryNavigationHasFourDestinations() {
        assertEquals(listOf("home", "contents", "study", "more"), primaryTabRoutes)
    }

    @Test fun centralHorizontalGesturesChangeSectionDirectionally() {
        assertEquals(ReaderPageSwipe.NEXT, classifyReaderSwipe(200f, 400f, -90f, 8f, 48f, 72f))
        assertEquals(ReaderPageSwipe.PREVIOUS, classifyReaderSwipe(200f, 400f, 90f, 8f, 48f, 72f))
    }

    @Test fun swipeIgnoresBackEdgesShortAndVerticalGestures() {
        assertNull(classifyReaderSwipe(20f, 400f, -100f, 0f, 48f, 72f))
        assertNull(classifyReaderSwipe(380f, 400f, 100f, 0f, 48f, 72f))
        assertNull(classifyReaderSwipe(200f, 400f, -60f, 0f, 48f, 72f))
        assertNull(classifyReaderSwipe(200f, 400f, -100f, 110f, 48f, 72f))
    }

    @Test fun openingFromContentsStartsAtStructuralBeginningInsteadOfSavedPosition() {
        assertEquals("first", readerEntryParagraph(ReaderEntryKind.CHAPTER, "first", null, null, "saved"))
        assertEquals("topic-start", readerEntryParagraph(ReaderEntryKind.TOPIC, "first", "topic-tail", "topic-start", "saved"))
        assertEquals("topic-tail", readerEntryParagraph(ReaderEntryKind.TOPIC, "first", "topic-tail", null, "saved"))
        assertEquals("saved", readerEntryParagraph(ReaderEntryKind.CONTINUE, "first", null, null, "saved"))
    }

    @Test fun backAtHomeStaysAndBackFromPrimaryTabsReturnsHome() {
        assertEquals(PrimaryBackAction.STAY, primaryBackAction("home"))
        assertEquals(PrimaryBackAction.HOME, primaryBackAction("contents"))
        assertEquals(PrimaryBackAction.HOME, primaryBackAction("study"))
        assertEquals(PrimaryBackAction.POP, primaryBackAction("read/{id}"))
    }

    @Test fun contentsTabFromReaderOpensContentsRootButReselectingKeepsIt() {
        assertEquals(true, shouldOpenContentsRoot("read/{id}?paragraph={paragraph}"))
        assertEquals(true, shouldOpenContentsRoot("search"))
        assertEquals(false, shouldOpenContentsRoot("contents"))
    }
}
