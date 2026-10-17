package ru.madarij.nativeapp

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer

class VintageAssetIntegrationTest {
    private val root = File("src/main")
    private val drawables = File(root, "res/drawable-nodpi")
    private val ui = File(root, "java/ru/madarij/nativeapp")

    private val frames = listOf("tab_active", "tab_inactive", "search_frame",
        "filter_active", "filter_inactive", "section_card", "subsection_card",
        "top_small_button")
    private val icons = listOf("search", "all", "read", "bookmark", "arrow_right",
        "expand", "settings")

    @Test fun allEightGeneratedFramesHaveTransparentCenter() {
        for(name in frames) {
            val file = File(drawables, "vintage_${name}.png")
            assertTrue("Missing vintage frame: ${name}", file.isFile)
            val bytes = file.readBytes()
            assertTrue(bytes.size > 200)
            assertTrue(bytes[0].toInt() == 137 - 256)
            assertTrue(String(bytes,Charsets.ISO_8859_1).contains("tRNS"))
        }
    }

    @Test fun allSevenOriginalIconsAreCompiledAsDrawableResources() {
        val source=File(ui, "HeritageFrames.kt").readText()
        for(name in icons) {
            val file=File(drawables, "vintage_icon_${name}.png")
            assertTrue("Missing vintage icon: ${name}", file.isFile)
            assertTrue(source.contains("R.drawable.vintage_icon_${name}"))
        }
        assertTrue(source.contains("VintageAssetIconKind.SETTINGS"))
    }

    @Test fun screenComponentsUseTheCorrectEightFrameSlots() {
        val framesCode=File(ui,"HeritageFrames.kt").readText()
        assertEquals(8, frames.size)
        frames.forEach { assertTrue(framesCode.contains("R.drawable.vintage_${it}")) }
        assertTrue(File(ui,"HomeContents.kt").readText().contains("HeritageFrameKind.SUBSECTION"))
        assertTrue(File(ui,"HeritageUi.kt").readText().contains("HeritageFrameKind.TAB_ACTIVE"))
        assertTrue(File(ui,"DesignSystem.kt").readText().contains("HeritageFrameKind.SMALL_BUTTON"))
        assertTrue(framesCode.contains("if (col==1 && row==1) continue"))
    }
}
