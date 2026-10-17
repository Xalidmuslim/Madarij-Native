package ru.madarij.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer

/** Checks that the APK packages an original translucent frame texture. */
class HeritageFrameTextureTest {
    @Test fun weatheredParchmentIsAValidTransparent96PixelFrame() {
        val file = File("src/main/res/drawable-nodpi/heritage_frame_parchment.png")
        assertTrue("Generated frame drawable must exist", file.isFile)
        val png = file.readBytes()
        val signature = byteArrayOf(137.toByte(),80,78,71,13,10,26,10)
        assertTrue("Not a valid PNG signature", png.take(8) == signature.toList())
        assertEquals(96, ByteBuffer.wrap(png,16,4).int)
        assertEquals(96, ByteBuffer.wrap(png,20,4).int)
        assertTrue("Indexed colors keep decorative art lightweight", png.size < 18000)
        assertTrue("Transparent center is required for live text", String(png, Charsets.ISO_8859_1).contains("tRNS"))
    }

    @Test fun cornersAreNotStretchedByPanelLayout() {
        val frames = File("src/main/java/ru/madarij/nativeapp/HeritageFrames.kt").readText()
        assertTrue(frames.contains("val sX = intArrayOf("))
        assertTrue(frames.contains("val dX = intArrayOf("))
        assertTrue(frames.contains("for (y in 0..2) for (x in 0..2)"))
        assertTrue(frames.contains("if (x == 1 && y == 1) continue"))
        assertTrue(frames.contains("R.drawable.heritage_frame_parchment"))
    }
}
