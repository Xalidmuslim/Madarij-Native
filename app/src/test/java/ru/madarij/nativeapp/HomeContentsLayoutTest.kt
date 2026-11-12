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

    @Test fun bronzeMarkerIsSharedByGroupNumbersChaptersAndContinuationArrows() {
        val components = File("src/main/java/ru/madarij/nativeapp/TocRows.kt").readText()
        val bronzeBadge = components.substringAfter("private fun HeritageCircleBadge(")
            .substringBefore("internal fun TocSectionRow(")
        assertTrue(bronzeBadge.contains("Brush.verticalGradient("))
        assertTrue(bronzeBadge.contains("CircleShape"))
        assertTrue(components.contains("TocSectionRow("))
        assertTrue(components.contains("TocChapterRow("))
        assertTrue(components.contains("TocTopicRow("))
        assertTrue(components.contains("HeritageCircleBadge("))
        assertTrue(components.contains("HorizontalDivider("))
        assertTrue(components.contains("TextButton(onClick = onRead"))
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
