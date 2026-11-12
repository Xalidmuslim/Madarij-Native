package ru.madarij.nativeapp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guard the four-volume TOC design and the optical center of the home CTA. */
class HomeContentsLayoutTest {
    private val source = File("src/main/java/ru/madarij/nativeapp/HomeContents.kt").readText()

    @Test fun settingsGearRemainsRaisedAndFunctional() {
        val homeHeader = source.substringBefore("item(key = \"knowledge-reminder\")")
        assertTrue(homeHeader.contains(".offset(y = (-24).dp)"))
        assertTrue(homeHeader.contains("painterResource(R.drawable.settings_gear_brass)"))
        assertTrue(homeHeader.contains("navigate(\"settings\")"))
    }

    @Test fun continueReadingUsesOneOpticallyCenteredContentRow() {
        val cta = source.substringAfter("item(key = \"knowledge-reminder\")")
            .substringBefore("item(key = \"home-quick-access\")")
        assertTrue(cta.contains(".height(58.dp).vintageFrame(VintageFrameKind.DARK_WIDE)"))
        assertTrue(cta.contains("Modifier.fillMaxSize().padding(horizontal=22.dp)"))
        assertTrue(cta.contains(".offset(y = (-4).dp)"))
        assertTrue(cta.contains("verticalAlignment=Alignment.CenterVertically"))
        assertFalse(cta.contains("Modifier.weight(1f).offset("))
        assertTrue(cta.contains("Text(\"Продолжить чтение\""))
    }

    @Test fun identicalSectionChapterAndTopicComponentsServeAllFourVolumes() {
        val content = source.substringAfter("internal fun ContentsScreen(")
        assertTrue(content.contains("availableVolumes.forEach { volume ->"))
        assertTrue(content.contains("chapter.chapterId in selectedChapterIds"))
        val bookRows = content.substringAfter("matching.forEach { group ->")
        assertTrue(bookRows.contains("TocSectionRow("))
        assertTrue(bookRows.contains("TocChapterRow("))
        assertTrue(bookRows.contains("TocTopicRow("))
        assertFalse(bookRows.contains("AgedPaperCard("))
        assertFalse(bookRows.contains(".vintageFrame("))
        assertTrue(bookRows.contains("open(chapter.chapterId, chapter.entryParagraphId)"))
        assertTrue(bookRows.contains("open(chapter.chapterId, topic.paragraphId)"))
        assertTrue(bookRows.contains("chapterPageLabel(chapter)"))
        assertTrue(bookRows.contains("groupPageLabel(group)"))
    }

    @Test fun compactUnframedBronze3dGlyphsMatchNavigationStyling() {
        val components = File("src/main/java/ru/madarij/nativeapp/TocRows.kt").readText()
        val glyph = components.substringAfter("private fun HeritageBronzeGlyph(")
            .substringBefore("private fun TocReadButton(")
        assertTrue(glyph.contains("Brush.verticalGradient("))
        assertTrue(glyph.contains("Modifier.offset("))
        assertTrue(glyph.contains("fontFamily = BookSerif"))
        assertTrue(glyph.contains("size = 30.dp"))
        assertFalse(glyph.contains("Canvas("))
        assertFalse(glyph.contains(".background("))
        assertFalse(glyph.contains(".border("))
        assertFalse(components.contains("HeritageBookplateBadge("))
        assertFalse(components.contains("HeritageCircleBadge("))
        assertFalse(components.contains("bookplatePath("))
        assertTrue(components.contains("HeritageBronzeGlyph(number.toString()"))
        assertTrue(components.contains("HeritageBronzeGlyph(if (expanded)"))
        assertTrue(components.contains("HeritageBronzeGlyph(marker"))
    }

    @Test fun readingAndExpansionRemainSeparateAndUnframed() {
        val components = File("src/main/java/ru/madarij/nativeapp/TocRows.kt").readText()
        val readButton = components.substringAfter("private fun TocReadButton(")
            .substringBefore("internal fun TocSectionRow(")
        assertTrue(readButton.contains("Role.Button"))
        assertTrue(readButton.contains("contentDescription = \"Читать\""))
        assertFalse(readButton.contains(".background("))
        assertFalse(readButton.contains(".border("))
        assertTrue(components.contains("TocReadButton(onClick = onRead)"))
        assertTrue(components.contains("onToggle"))
        assertTrue(components.contains("HorizontalDivider("))
        assertTrue(components.contains("if (percent > 0) LinearProgressIndicator("))
    }

    @Test fun volumeTabsSearchAndFiltersStayOutsideRedesign() {
        val controls = source.substringAfter("internal fun ContentsScreen(")
            .substringBefore("matching.forEach { group ->")
        assertTrue(controls.contains("HeritageTocVolumeTab("))
        assertTrue(controls.contains("VintageSearchField("))
        assertTrue(controls.contains("HeritageTocFilterTab("))
        assertTrue(controls.contains("HeritageSectionHeading(\"Разделы книги\""))
        assertTrue(controls.contains("Продолжить с места остановки"))
    }
}
