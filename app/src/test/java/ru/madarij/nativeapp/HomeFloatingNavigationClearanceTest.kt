package ru.madarij.nativeapp

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Keep the home dashboard readable above the floating leather navigation. */
class HomeFloatingNavigationClearanceTest {
    private val home = File("src/main/java/ru/madarij/nativeapp/HomeContents.kt")

    @Test fun reserveScrollSpaceForFloatingBarAndSystemNavigation() {
        val source = home.readText()
        assertTrue(source.contains("calculateBottomPadding() + 68.dp"))
        assertTrue(source.contains("contentPadding = PaddingValues(start = 0.dp, end = 0.dp, bottom = 16.dp)"))
    }

    @Test fun compactOnlyTheGapAndCardsRatherThanScalingDownTypography() {
        val source = home.readText()
        assertTrue(source.contains("verticalArrangement = Arrangement.spacedBy(6.dp)"))
        assertTrue(source.contains("modifier = modifier.heightIn(min = 62.dp)"))
        assertTrue(source.contains("Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 3.dp)"))
        assertTrue(source.contains("fontSize = 15.sp, lineHeight = 19.sp"))
        assertTrue(source.contains("fontSize = 10.sp, lineHeight = 13.sp"))
    }

    @Test fun leaveHomeHeroAndReminderAnimationsUntouched() {
        val source = home.readText()
        assertTrue(source.contains("height(258.dp)"))
        assertTrue(source.contains("includeFontPadding = true"))
        assertTrue(source.contains("animate = !inkFinished"))
        assertTrue(source.contains("onFinished = onInkFinished"))
    }
}
