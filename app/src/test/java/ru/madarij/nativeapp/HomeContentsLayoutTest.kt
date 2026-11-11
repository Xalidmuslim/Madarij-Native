package ru.madarij.nativeapp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Regression checks for the approved home and contents reference. */
class HomeContentsLayoutTest {
    private val source = File("src/main/java/ru/madarij/nativeapp/HomeContents.kt").readText()

    @Test fun settingsGearIsRaisedWithoutMovingTheBookArtwork() {
        val homeHeader = source.substringBefore("item(key = \"knowledge-reminder\")")
        assertTrue(homeHeader.contains(".offset(y = (-24).dp)"))
        assertTrue(homeHeader.contains("painterResource(R.drawable.settings_gear_brass)"))
        assertTrue(homeHeader.contains("navigate(\"settings\")"))
    }

    @Test fun continueReadingContentsAreCenteredWithinTheirOwnFrame() {
        val continueReading = source.substringAfter("item(key = \"knowledge-reminder\")")
            .substringBefore("item(key = \"home-quick-access\")")
        assertTrue(continueReading.contains(".height(58.dp).vintageFrame(VintageFrameKind.DARK_WIDE)"))
        assertTrue(continueReading.contains("Modifier.fillMaxSize().padding(horizontal=22.dp)"))
        assertTrue(continueReading.contains("verticalAlignment=Alignment.CenterVertically"))
    }

    @Test fun onlyBookSectionRowsLoseTheirFramedCards() {
        val controls = source.substringAfter("internal fun ContentsScreen(")
            .substringBefore("matching.forEach { group ->")
        assertTrue(controls.contains("HeritageTocVolumeTab("))
        assertTrue(controls.contains("VintageSearchField("))
        assertTrue(controls.contains("HeritageTocFilterTab("))
        assertTrue(controls.contains("HeritageSectionHeading(\"Разделы книги\""))
        val bookRows = source.substringAfter("matching.forEach { group ->")
        assertFalse(bookRows.contains("VintageFrameKind.SECTION_NUMBERED"))
        assertFalse(bookRows.contains(".vintageFrame(VintageFrameKind.LIGHT_WIDE)"))
        assertFalse(bookRows.contains("AgedPaperCard("))
        assertTrue(bookRows.contains("HorizontalDivider("))
        assertTrue(bookRows.contains("HeritageManuscriptNumber("))
        assertTrue(bookRows.contains("TextButton(onClick = { open(chapter.chapterId, chapter.entryParagraphId) }"))
        assertTrue(bookRows.contains("open(chapter.chapterId, topic.paragraphId)"))
    }
}
