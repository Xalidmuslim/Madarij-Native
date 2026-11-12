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

    @Test fun resumeButtonUsesDistinctSmallBronzeQuillAndInkpotIcon() {
        val cta = source.substringAfter("item(key = \\"knowledge-reminder\\")")
            .substringBefore("item(key = \\"home-quick-access\\")")
        assertTrue(cta.contains("ContinueReadingQuillIcon("))
        assertTrue(cta.contains("Modifier.size(28.dp).offset(y = 4.dp)"))
        assertFalse(cta.contains("NavigationGlyph(\\"contents\\",selected=true"))
        assertTrue(cta.contains("Text(\\"Продолжить чтение\\""))
        assertTrue(cta.contains(".height(58.dp).vintageFrame(VintageFrameKind.DARK_WIDE)"))
        val icon = File("src/main/java/ru/madarij/nativeapp/ContinueReadingQuillIcon.kt").readText()
        assertTrue(icon.contains("internal fun ContinueReadingQuillIcon("))
        assertTrue(icon.contains("Canvas("))
        assertTrue(icon.contains("Brush.verticalGradient("))
        assertTrue(icon.contains("drawPath("))
        assertTrue(icon.contains("drawOval("))
        assertFalse(icon.contains("NavigationGlyph(\\"contents\\""))
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

    @Test fun generatedBronzeAtlasIsUsedInsteadOfTextGradients() {
        val components = File("src/main/java/ru/madarij/nativeapp/TocRows.kt").readText()
        val glyph = components.substringAfter("private fun HeritageBronzeGlyph(")
            .substringBefore("private fun TocReadButton(")
        assertTrue(glyph.contains("ImageBitmap.imageResource(R.drawable.toc_bronze_sprite)"))
        assertTrue(glyph.contains("drawImage("))
        assertTrue(glyph.contains("srcOffset = IntOffset("))
        assertTrue(glyph.contains("srcSize = IntSize("))
        assertTrue(glyph.contains("FilterQuality.Medium"))
        assertFalse(glyph.contains("Brush.verticalGradient("))
        assertFalse(glyph.contains("Modifier.offset("))
        assertFalse(glyph.contains(".background("))
        assertFalse(glyph.contains(".border("))
        assertTrue(components.contains("HeritageBronzeGlyph(number.toString()"))
        assertTrue(components.contains("HeritageBronzeGlyph(if (expanded)"))
        assertTrue(components.contains("HeritageBronzeGlyph(marker"))
    }

    @Test fun generatedAtlasIncludesAllDigitsAndActionsForFourVolumes() {
        val file = File("src/main/res/drawable-nodpi/toc_bronze_sprite.png")
        assertTrue("The actual generated bronze artwork must be packaged", file.isFile)
        val bytes = file.readBytes()
        assertTrue("PNG header must be present", bytes.size > 24)
        assertTrue(bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte())
        val header = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.BIG_ENDIAN)
        assertTrue(header.getInt(16) == 384 && header.getInt(20) == 384)
        val components = File("src/main/java/ru/madarij/nativeapp/TocRows.kt").readText()
        assertTrue(components.contains("when (symbol)"))
        assertTrue(components.contains("'↳' -> 12"))
        assertTrue(components.contains("'⌄' -> 11"))
        assertTrue(components.contains("'›' -> 10"))
        assertTrue(components.contains("symbol - '0'"))
    }

    @Test fun numbersKeepOneAlignedColumnAndNaturalDigitWidths() {
        val components = File("src/main/java/ru/madarij/nativeapp/TocRows.kt").readText()
        assertTrue(components.contains("private val TOC_MARKER_WIDTH = 44.dp"))
        assertTrue(components.contains("private const val TOC_ATLAS_TILE = 96"))
        assertTrue(components.contains("private fun glyphSlotWidth("))
        assertTrue(components.contains("srcSize = IntSize(64, 80)"))
        assertTrue(components.contains("size = TOC_MARKER_WIDTH"))
        assertFalse(components.contains("size = if (number >= 10) 38.dp"))
        assertFalse(components.contains("size = if (marker.length > 1) 38.dp"))
        assertTrue(components.filterNot(Char::isWhitespace).contains("Box(Modifier.size(TOC_MARKER_WIDTH)"))
        assertTrue(components.contains("HorizontalDivider("))
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
