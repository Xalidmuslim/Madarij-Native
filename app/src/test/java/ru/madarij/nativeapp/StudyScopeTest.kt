package ru.madarij.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyScopeTest {
    private val volumes = mapOf("v1-a" to 1, "v1-b" to 1, "v2-a" to 2, "v3-a" to 3)

    @Test fun wholeBookScopeIncludesBothVolumes() {
        assertTrue(studyScopeMatches("v1-a", "", 0, volumes))
        assertTrue(studyScopeMatches("v2-a", "", 0, volumes))
    }

    @Test fun firstAndSecondVolumesRemainSeparated() {
        assertTrue(studyScopeMatches("v1-b", "", 1, volumes))
        assertFalse(studyScopeMatches("v2-a", "", 1, volumes))
        assertFalse(studyScopeMatches("v1-a", "", 2, volumes))
        assertTrue(studyScopeMatches("v2-a", "", 2, volumes))
    }

    @Test fun selectedChapterNarrowingIsConsistentAcrossTabs() {
        assertTrue(studyScopeMatches("v2-a", "v2-a", 2, volumes))
        assertFalse(studyScopeMatches("v1-a", "v2-a", 2, volumes))
    }

    @Test fun volumeSwitchDoesNotRetainHiddenSelectedChapter() {
        assertEquals("", selectedStudyChapterAfterVolumeChange("v1-b", 2, volumes))
        assertEquals("v1-b", selectedStudyChapterAfterVolumeChange("v1-b", 1, volumes))
        assertEquals("", selectedStudyChapterAfterVolumeChange("v1-b", 0, volumes))
        assertEquals("", selectedStudyChapterAfterVolumeChange("", 2, volumes))
    }

    @Test fun pickerShowsShortPreviewButDoesNotHideSearchResults() {
        val sections=(1..90).map { "section-$it" }
        assertEquals(sections.take(6),studyPickerPreview(sections,searching=false,expanded=false))
        assertEquals(sections,studyPickerPreview(sections,searching=true,expanded=false))
        assertEquals(sections,studyPickerPreview(sections,searching=false,expanded=true))
    }

    @Test fun pickerKeepsSecondVolumeAndSelectedShortVolumesAccessible() {
        val secondVolume = (1..9).map { "v2-$it" }
        assertEquals(6,studyPickerPreview(secondVolume,false,false).size)
        assertEquals(9,studyPickerPreview(secondVolume,false,true).size)
        assertEquals(listOf("one"),studyPickerPreview(listOf("one"),false,false))
    }

    @Test fun invalidChapterIsNotImplicitlyIncludedInVolume() {
        assertFalse(studyScopeMatches("unknown", "", 1, volumes))
    }
    @Test fun thirdVolumeScopeAndTopicSelectionArePreserved() {
        assertTrue(studyScopeMatches("v3-a", "", 0, volumes))
        assertTrue(studyScopeMatches("v3-a", "", 3, volumes))
        assertFalse(studyScopeMatches("v2-a", "", 3, volumes))
        assertEquals("v3-a", selectedStudyChapterAfterVolumeChange("v3-a", 3, volumes))
        assertEquals("", selectedStudyChapterAfterVolumeChange("v3-a", 2, volumes))
    }
}
