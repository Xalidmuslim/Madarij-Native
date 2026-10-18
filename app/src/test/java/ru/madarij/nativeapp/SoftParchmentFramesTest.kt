package ru.madarij.nativeapp

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Paper art regression checks: never restore hard rounded Card cropping. */
class SoftParchmentFramesTest {
    private val root = File("src/main/java/ru/madarij/nativeapp")
    @Test fun liveCardsPreserveIrregularAlphaEdges() {
        val cards = File(root, "AgedPaperCard.kt").readText()
        assertTrue(cards.contains("Box(modifier.vintageFrame(frameKind))"))
        assertTrue(cards.contains(".clickable(enabled = enabled, onClick = onClick)"))
        assertTrue(!cards.contains("Card("))
    }
    @Test fun everyPageUsesNativeParchmentImageSlices() {
        val renderer = File(root, "VintageFrame.kt").readText()
        assertTrue(renderer.contains("drawWithCache"))
        assertTrue(renderer.contains("drawImage("))
        assertTrue(renderer.contains("maxVerticalEdge"))
        assertTrue(renderer.contains("w.toFloat() / bitmap.width"))
        assertTrue(renderer.contains("VintageFrameKind.SECTION_NUMBERED"))
    }
    @Test fun allSevenUserAssetsExistAndAreNonempty() {
        val resources = File("src/main/res/drawable-nodpi")
        listOf("dark_compact","dark_wide","font_settings","light_compact",
            "light_wide","search","section_numbered").forEach {
            val item = File(resources,"frame_${it}.webp")
            assertTrue("${item.name} must exist", item.isFile && item.length()>500)
        }
    }
}
