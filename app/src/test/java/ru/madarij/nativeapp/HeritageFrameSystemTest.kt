package ru.madarij.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HeritageFrameSystemTest {
    private val source = File("src/main/java/ru/madarij/nativeapp")
    private fun code(file: String) = File(source, file).readText()

    @Test fun oneNativeEtchingIsSharedByBothClickableAndStaticCards() {
        val shared = code("HeritageFrames.kt")
        val cards = code("AgedPaperCard.kt")
        assertTrue(shared.contains("Modifier.heritageEtching("))
        assertTrue(shared.contains("drawWithCache"))
        assertTrue(shared.contains("onDrawWithContent"))
        assertTrue(shared.contains("drawRoundRect"))
        assertEquals(2, cards.split("modifier.heritageEtching(kind = frameKind)").size - 1)
        assertTrue(shared.contains("R.drawable.vintage_section_card"))
        assertTrue(shared.contains("ImageBitmap.imageResource("))
        assertTrue(shared.contains("srcOffset=IntOffset("))
        assertTrue(shared.contains("dstSize=IntSize("))
        assertTrue(shared.contains("col==1 && row==1"))
        assertTrue(shared.contains("filterQuality=FilterQuality.Low"))
        assertFalse(shared.contains("PreloadedBookImage("))
    }

    @Test fun coversMainSectionsAndTheirIndependentContainers() {
        val design = code("DesignSystem.kt")
        val home = code("HomeContents.kt")
        val heritage = code("HeritageUi.kt")
        val fields = code("LibraryScreens.kt")
        val learning = code("Learning.kt")
        assertTrue(design.contains(".heritageEtching())"))
        assertTrue(home.contains("heritageEtching(cornerRadius = 17.dp)"))
        assertTrue(home.contains("heritageEtching(cornerRadius = 14.dp"))
        assertTrue(heritage.contains("HeritageFrameKind.FILTER_ACTIVE"))
        assertTrue(fields.contains("HeritageFrameTokens.fieldRadius"))
        assertTrue(learning.contains("HeritageFrameTokens.fieldRadius"))
        assertTrue(code("StudyChapterPicker.kt").contains("HeritageFrameTokens.fieldRadius"))
    }

    @Test fun readingControlsAndDialogsKeepWorkingAndShowTheSameTrim() {
        val reader = code("Reader.kt")
        val backup = code("BackupScreen.kt")
        assertTrue(reader.contains("AlertDialog(modifier = Modifier.heritageEtching"))
        assertTrue(reader.contains("ModalBottomSheet(modifier = Modifier.heritageEtching"))
        assertTrue(backup.contains("AlertDialog(modifier = Modifier.heritageEtching"))
        assertTrue(code("DesignSystem.kt").contains("R.drawable.navigation_leather"))
        assertTrue(code("AgedPaperCard.kt").contains("R.drawable.card_paper"))
    }

    @Test fun frameHasDayAndNightColorsButNoMutableOrAnimatedNoise() {
        val frame = code("HeritageFrames.kt")
        assertTrue(frame.contains("BookColors.nightBackground"))
        assertTrue(frame.contains("nightExterior"))
        assertTrue(frame.contains("cornerRadius.toPx()"))
        assertTrue(frame.contains("if (night)"))
        assertFalse(frame.contains("Random("))
        assertFalse(frame.contains("rememberInfiniteTransition"))
    }
}
