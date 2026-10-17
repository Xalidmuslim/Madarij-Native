package ru.madarij.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReedPenInkBoundsTest {
    @Test fun descendersRemainVisibleBelowTheNominalLineBottom() {
        val (top, bottom) = reedPenVisibleLineBounds(
            lineTop = 0f, lineBottom = 28f,
            glyphTop = 1f, glyphBottom = 34f,
            overscan = 7f, height = 52f
        )
        assertEquals(0f, top, .01f)
        assertEquals(41f, bottom, .01f)
        assertTrue(bottom > 34f)
    }

    @Test fun overscanCannotDrawOutsideTextSurface() {
        val (top, bottom) = reedPenVisibleLineBounds(
            lineTop = 4f, lineBottom = 47f,
            glyphTop = -3f, glyphBottom = 54f,
            overscan = 9f, height = 50f
        )
        assertEquals(0f, top, .01f)
        assertEquals(50f, bottom, .01f)
    }

    @Test fun homeQuoteHasExtraFontPaddingWithoutReplacingAnimation() {
        val root = File("src/main/java/ru/madarij/nativeapp")
        val home = File(root, "HomeContents.kt").readText()
        val pen = File(root, "ReedPenWritingText.kt").readText()
        assertTrue(home.contains("includeFontPadding = true"))
        assertTrue(home.contains(".padding(top = 4.dp, bottom = 12.dp)"))
        assertTrue(pen.contains("reedPenVisibleLineBounds("))
        assertTrue(pen.contains("drawWithContent"))
        assertTrue(pen.contains("ReedPenCharacterTimeline"))
    }

    @Test fun tocTabsAreNativeAndNotBitmapsTakenFromReference() {
        val ui = File("src/main/java/ru/madarij/nativeapp/HeritageUi.kt").readText()
        assertTrue(ui.contains("internal fun HeritageTocVolumeTab("))
        assertTrue(ui.contains("internal fun HeritageTocFilterTab("))
        assertTrue(ui.contains("private fun HeritageTocEngraving("))
        assertTrue(ui.contains("role = Role.Tab"))
    }
}
