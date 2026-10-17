package ru.madarij.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.zip.InflaterInputStream

/** No java.awt or ImageIO, which are unavailable on Android's JVM classpath. */
class AntiqueFrameArtworkTest {
    private val root = File("src/main/res/drawable-nodpi")
    private val names = listOf("tab_active", "tab_inactive", "search_frame",
        "filter_active", "filter_inactive", "section_card",
        "subsection_card", "top_small_button")

    private fun png(file: File): Triple<Int, Int, ByteArray> {
        val bytes = file.readBytes()
        assertTrue(bytes.size > 60)
        assertEquals(137 - 256, bytes[0].toInt())
        assertEquals(3, bytes[25].toInt()) // 8-bit indexed colour
        val width = ByteBuffer.wrap(bytes, 16, 4).int
        val height = ByteBuffer.wrap(bytes, 20, 4).int
        var offset = 8
        var transparent = false
        val compressed = ByteArrayOutputStream()
        while (offset + 12 <= bytes.size) {
            val length = ByteBuffer.wrap(bytes, offset, 4).int
            assertTrue(length >= 0 && offset + length + 12 <= bytes.size)
            val kind = String(bytes, offset + 4, 4, Charsets.US_ASCII)
            if (kind == "IDAT") compressed.write(bytes, offset + 8, length)
            if (kind == "tRNS") {
                transparent = true
                assertEquals(0, bytes[offset + 8].toInt())
            }
            offset += length + 12
            if (kind == "IEND") break
        }
        assertTrue("Expected indexed alpha for the transparent center", transparent)
        val pixels = InflaterInputStream(ByteArrayInputStream(compressed.toByteArray())).use {
            it.readBytes()
        }
        assertEquals(height * (width + 1), pixels.size)
        return Triple(width, height, pixels)
    }

    private fun at(pixels: ByteArray, width: Int, x: Int, y: Int) =
        pixels[y * (width + 1) + x + 1].toInt() and 255

    @Test fun originalNineSliceFramesHaveTransparentCenterAndCompleteSides() {
        for (name in names) {
            val file = File(root, "vintage_" + name + ".png")
            assertTrue("Missing frame $name", file.isFile)
            val (width, height, pixels) = png(file)
            assertEquals(192, width)
            assertEquals(112, height)
            assertTrue((0 until height).all { pixels[it * (width + 1)].toInt() == 0 })
            assertEquals("The content area must be transparent: $name",
                0, at(pixels, width, width / 2, height / 2))
            assertTrue("Vertical trim too broken: $name",
                (8 until height - 8).count { at(pixels, width, 2, it) != 0 } > height / 2)
            assertTrue("Horizontal trim too broken: $name",
                (8 until width - 8).count { at(pixels, width, it, 2) != 0 } > width / 2)
        }
    }

    @Test fun framingsStayBehindNativeTextAndWorkAtNight() {
        val frames = File("src/main/java/ru/madarij/nativeapp/HeritageFrames.kt").readText()
        val cards = File("src/main/java/ru/madarij/nativeapp/AgedPaperCard.kt").readText()
        assertTrue(frames.contains("if (col==1 && row==1) continue"))
        assertTrue(frames.contains("alpha=if (night)"))
        assertFalse(frames.contains("rememberInfiniteTransition"))
        assertTrue(cards.contains("BELOW the live UI"))
    }
}
