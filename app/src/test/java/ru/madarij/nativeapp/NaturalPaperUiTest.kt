package ru.madarij.nativeapp

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Prevent accidental return of embossed image panels or oversized category rows. */
class NaturalPaperUiTest {
    private val root=File("src/main/java/ru/madarij/nativeapp")
    @Test fun exactUserApprovedPaperImagesAreBundledAndReadable() {
        val script=File("../tools/generate_soft_paper_frames.py").readText()
        assertTrue(script.contains("tools/assets/approved_paper_sprite.webp"))
        assertTrue(script.contains("frame_light_wide"))
        assertTrue(script.contains("frame_section_numbered"))
        assertTrue(script.contains("frame_search"))
        assertTrue(script.contains("frame_font_settings"))
        assertTrue(script.contains("frame_dark_compact"))
        assertTrue(script.contains("verify_tile(name, card)"))
        assertTrue(script.contains("ImageStat.Stat(center).stddev[0] < 15"))
    }
    @Test fun chapterNumberAndArrowAreIndependentNativeElements() {
        val toc=File(root,"HomeContents.kt").readText()
        assertTrue(toc.contains("TocSectionRow("))
        assertTrue(File(root, "TocRows.kt").readText().contains("if (expanded) \"⌄\" else \"›\""))
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
        assertTrue(home.contains("painterResource(R.drawable.settings_gear_brass)"))
        assertTrue(home.contains("contentDescription = \"Настройки\""))
        assertTrue(home.contains("navigate(\"settings\")"))
        assertTrue(!home.contains(".vintageFrame(VintageFrameKind.FONT_SETTINGS)"))
    }
    @Test fun quickAccessIsFramelessTwoRowsWithSixSmallIcons() {
        val home = File(root, "HomeContents.kt").readText()
        val grid = home.substringAfter("item(key = \"home-quick-access\")")
            .substringBefore("item(key = \"home-reading-stats\")")
        assertTrue(grid.contains("HorizontalDivider"))
        assertTrue(grid.contains("VerticalDivider"))
        assertTrue(Regex("HomeQuickAction\\(").findAll(grid).count() == 6)
        val quick = home.substringAfter("private fun HomeQuickAction(")
            .substringBefore("// Printed-page labels")
        assertTrue(quick.contains("Modifier.size(28.dp)"))
        assertTrue(quick.contains(".clickable(onClick = onClick)"))
        assertTrue(!quick.contains("Soft3DPanel"))
    }

}
