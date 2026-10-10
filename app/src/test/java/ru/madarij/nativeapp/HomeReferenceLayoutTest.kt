package ru.madarij.nativeapp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Keeps the presentation-only UI changes in place without touching data stores. */
class HomeReferenceLayoutTest {
    private val root = File("src/main/java/ru/madarij/nativeapp/")
    @Test fun cardContentFillsCardAndUsesSharedLeatherTexture() {
        val source = File(root, "HomeContents.kt").readText()
        assertTrue(source.contains("Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 7.dp)"))
        assertTrue(source.contains("PreloadedBookImage(R.drawable.navigation_leather"))
    }
    @Test fun hiddenMenuItemsDoNotReappear() {
        val source = File(root, "LibraryScreens.kt").readText()
        assertFalse(source.contains("\"backup\" to (\"Резервная копия\""))
        assertFalse(source.contains("InfoCard(\"На вашем устройстве\""))
    }
}
