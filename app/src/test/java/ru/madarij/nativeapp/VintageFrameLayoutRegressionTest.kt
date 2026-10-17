package ru.madarij.nativeapp

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VintageFrameLayoutRegressionTest {
    private fun source(name: String) =
        File("src/main/java/ru/madarij/nativeapp/$name").readText()

    @Test fun contentsAndHomeDrawLiveLabelsAfterFrameLayer() {
        val home = source("HomeContents.kt")
        assertTrue(home.contains("Box(Modifier.matchParentSize().heritageEtching(cornerRadius = 17.dp"))
        assertTrue(home.contains("Column(Modifier.padding(top = 7.dp, bottom = 9.dp))"))
        val cards = source("AgedPaperCard.kt")
        assertTrue(cards.contains("Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp))"))
    }

    @Test fun moreMenuRowsHaveSeparateClickableAgedFrames() {
        val menu = source("HeritageUi.kt").substringAfter("internal fun HeritageMenuGroup(")
            .substringBefore("internal fun HeritageProgressRing(")
        assertTrue(menu.contains("items.forEach { item ->"))
        assertTrue(menu.contains("AgedPaperCard("))
        assertTrue(menu.contains("onClick = { navigate(item.first) }"))
        assertTrue(menu.contains("frameKind = HeritageFrameKind.SUBSECTION"))
        assertTrue(!menu.contains("HorizontalDivider("))
    }

    @Test fun graphicBorderThicknessIsBoundedForCompactCards() {
        val styles = source("HeritageFrames.kt")
        val weights = Regex("""HeritageFrameKind[.](SECTION|SUBSECTION) *-> *([0-9.]+)f""")
            .findAll(styles).associate { it.groupValues[1] to it.groupValues[2].toFloat() }
        val section = requireNotNull(weights["SECTION"]) { "SECTION weight missing" }
        val subsection = requireNotNull(weights["SUBSECTION"]) { "SUBSECTION weight missing" }
        // Antique detail may be stronger than the old flat trim, but may not
        // become opaque or obscure short labels. Subsections remain quieter.
        assertTrue("Section ornament has too much contrast", section in .70f.. .84f)
        assertTrue("Subsection ornament has too much contrast", subsection in .66f.. .82f)
        assertTrue("Subsections must not overpower sections", subsection <= section)
        assertTrue(styles.contains("15.dp.toPx().roundToInt()"))
    }
}
