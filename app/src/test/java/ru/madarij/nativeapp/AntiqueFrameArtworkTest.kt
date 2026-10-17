package ru.madarij.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

/** Regression guard for original nine-slice artwork generated before Gradle builds. */
class AntiqueFrameArtworkTest {
    private val root = File("src/main/res/drawable-nodpi")
    private val names = listOf(
        "tab_active", "tab_inactive", "search_frame", "filter_active",
        "filter_inactive", "section_card", "subsection_card", "top_small_button"
    )

    @Test fun allEightHaveRealWornBordersAndTransparentTextCenters() {
        for (name in names) {
            val file = File(root, "vintage_${name}.png")
            assertTrue("Missing $name", file.isFile)
            val image = ImageIO.read(file)
            assertNotNull("Unreadable $name", image)
            assertEquals(192, image.width)
            assertEquals(112, image.height)
            assertEquals("Text center must be transparent: $name",
                0, image.getRGB(image.width / 2, image.height / 2) ushr 24)
            assertTrue("The vertical edge is missing: $name",
                image.getRGB(2, image.height / 2) ushr 24 > 0)
            assertTrue("The top edge is missing: $name",
                image.getRGB(image.width / 2, 2) ushr 24 > 0)
        }
    }

    @Test fun framedSurfacesStayLiveAndRespectNativeNavigation() {
        val frames = File("src/main/java/ru/madarij/nativeapp/HeritageFrames.kt").readText()
        val cards = File("src/main/java/ru/madarij/nativeapp/AgedPaperCard.kt").readText()
        assertTrue(frames.contains("if (col==1 && row==1) continue"))
        assertTrue(frames.contains("alpha=if (night)"))
        assertFalse(frames.contains("rememberInfiniteTransition"))
        assertTrue(cards.contains("BELOW the live UI"))
    }
}
