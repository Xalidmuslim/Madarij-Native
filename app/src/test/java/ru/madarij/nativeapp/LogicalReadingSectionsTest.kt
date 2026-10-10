package ru.madarij.nativeapp

import org.junit.Assert.*
import org.junit.Test
import ru.madarij.nativeapp.data.Chapter

class LogicalReadingSectionsTest {
    private fun testChapters() = listOf(
        Chapter("madarij-v1-p132-draft-section",1,19,"Начало", ""),
        Chapter("madarij-v1-p140-draft-section",1,20,"Продолжение", ""),
        Chapter("madarij-v1-p148-draft-section",1,21,"Продолжение", ""),
        Chapter("madarij-v1-p156-draft-section",1,22,"Продолжение", ""),
        Chapter("madarij-v1-p159-draft-section",1,23,"Следующая глава", "")
    )
    @Test fun middlePageSliceIsNotANewChapter() {
        val rows = testChapters()
        val members = LogicalReadingSections.members(rows,"madarij-v1-p140-draft-section")
        assertEquals(4, members.size)
        assertEquals("madarij-v1-p132-draft-section", members.first().id)
        assertEquals("madarij-v1-p156-draft-section", members.last().id)
        assertEquals(2, LogicalReadingSections.roots(rows).size)
        assertEquals("Страницы 132–158", LogicalReadingSections.pageRange(members.first().id))
    }
    @Test fun unknownChapterReturnsEmpty() {
        assertTrue(LogicalReadingSections.members(testChapters(), "unknown").isEmpty())
        assertEquals(30, LogicalReadingSections.starts.size)
    }
}
