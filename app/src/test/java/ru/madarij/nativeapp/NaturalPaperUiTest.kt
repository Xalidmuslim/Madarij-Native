package ru.madarij.nativeapp

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Prevent accidental return of embossed image panels or oversized category rows. */
class NaturalPaperUiTest {
    private val root=File("src/main/java/ru/madarij/nativeapp")
    @Test fun generatedPaperNeverContainsStampsOrFrames() {
        val script=File("../tools/generate_soft_paper_frames.py").readText()
        assertTrue(script.contains("frame_light_wide"))
        assertTrue(script.contains("frame_section_numbered"))
        assertTrue(script.contains("frame_dark_compact"))
        assertTrue(script.contains("verify.getpixel((0, 0))[3] == 0"))
        assertTrue("Texture needs genuine edge patina",
            script.contains("sum(center_mean) - sum(edge_mean) > 33"))
        assertTrue("Texture needs paper fibers",
            script.contains("ImageStat.Stat(center).stddev[0] > 2.5"))
        assertTrue(script.contains("alpha_composite"))
        assertTrue(!script.contains("draw.rectangle("))
    }
    @Test fun chapterNumberAndArrowAreIndependentNativeElements() {
        val toc=File(root,"HomeContents.kt").readText()
        assertTrue(toc.contains("HeritageManuscriptNumber("))
        assertTrue(toc.contains("if (expanded) \"⌄\" else \"›\""))
        assertTrue(!toc.contains("Arrow is embedded"))
    }
    @Test fun menuCategoriesUseOnePaperSurface() {
        val ui=File(root,"HeritageUi.kt").readText()
        val content=ui.substringAfter("internal fun HeritageMenuGroup(")
            .substringBefore("internal fun HeritageProgressRing(")
        assertTrue(content.contains("items.forEachIndexed"))
        assertTrue(content.contains("heightIn(min = 61.dp)"))
        assertTrue(content.contains("HorizontalDivider"))
    }
    @Test fun searchAndFontControlsAreNotRasterizedPictograms() {
        val frame=File(root,"VintageFrame.kt").readText()
        val design=File(root,"DesignSystem.kt").readText()
        assertTrue(frame.contains("androidx.compose.foundation.Canvas"))
        assertTrue(frame.contains("Spacer(Modifier.width(13.dp))"))
        assertTrue(design.contains("Text(\"Aa\""))
        val home=File(root,"HomeContents.kt").readText()
        assertTrue(home.contains("Text(\"Aa\", fontFamily = BookSerif"))
        assertTrue(home.contains("contentDescription = \"Настройки чтения\""))
        assertTrue(home.contains("val ink = Color(0xFF725034)"))
    }
}
