package ru.madarij.nativeapp

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class KnowledgeRemindersTest {
    @Test fun completeDeckAndNoRepeatAtBoundary() {
        assertEquals(26, KnowledgeReminders.items.size)
        val a = KnowledgeReminders.newOrder(-1, Random(10))
        assertEquals(KnowledgeReminders.items.size, a.toSet().size)
        assertEquals(KnowledgeReminders.items.indices.toSet(), a.toSet())
        val b = KnowledgeReminders.newOrder(a.last(), Random(25))
        assertEquals(KnowledgeReminders.items.size, b.toSet().size)
        assertNotEquals(a.last(), b.first())
    }
    @Test fun everyQuoteHasOriginalLocation() {
        assertTrue(KnowledgeReminders.items.all {
            it.page in 3..610 && it.text.length >= 60 &&
            it.chapterId.isNotBlank() && it.paragraphId.isNotBlank()
        })
    }
}
