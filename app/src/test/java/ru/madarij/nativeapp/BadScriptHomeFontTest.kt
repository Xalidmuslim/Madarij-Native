package ru.madarij.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Font
import java.io.File
import java.security.MessageDigest

class BadScriptHomeFontTest {
    @Test fun officialBadScriptIsEmbeddedAndLicensed() {
        val fontFile = File("src/main/res/font/bad_script.ttf")
        assertTrue("Bad Script must be a local, bundled Android font", fontFile.isFile)
        val bytes = fontFile.readBytes()
        assertEquals(184216, bytes.size)
        val sha1 = MessageDigest.getInstance("SHA-1").apply {
            update("blob ${bytes.size}\\u0000".replace("\\u0000", "\u0000").toByteArray(Charsets.UTF_8))
            update(bytes)
        }.digest().joinToString("") { "%02x".format(it) }
        assertEquals("6a2aabeb4b5f7b2d6885fb8b39241c310e383528", sha1)

        val license = File("src/main/assets/fonts/BadScript-OFL.txt").readText()
        assertTrue(license.contains("SIL OPEN FONT LICENSE Version 1.1"))
        assertTrue(license.contains("The Bad Script Project Authors"))
    }

    @Test fun badScriptFontSupportsRussianCyrillic() {
        val file = File("src/main/res/font/bad_script.ttf")
        val font = Font.createFont(Font.TRUETYPE_FONT, file)
        val sample = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ абвгдеёжзийклмнопрстуфхцчшщъыьэюя"
        assertEquals("Missing Russian Cyrillic characters", -1, font.canDisplayUpTo(sample))
    }

    @Test fun onlyHomeAnimatedQuoteChangesFontAndPerformanceAnimationRemains() {
        val root = File("src/main/java/ru/madarij/nativeapp")
        val sources = root.walkTopDown().filter { it.isFile && it.extension == "kt" }
            .filter { it.readText().contains("Font(R.font.bad_script)") }.map { it.name }.toList()
        assertEquals(listOf("HomeContents.kt"), sources)
        val home = File(root, "HomeContents.kt").readText()
        val pen = File(root, "ReedPenWritingText.kt").readText()
        assertTrue(home.contains("fontFamily = FontFamily(Font(R.font.bad_script))"))
        assertTrue(home.contains("ReedPenWritingText("))
        assertTrue(home.contains("fontSize = 24.sp, lineHeight = 31.sp"))
        assertFalse(home.contains("fontFamily = FontFamily(Font(R.font.caveat_handwriting))"))
        assertTrue(pen.contains("ReedPenCharacterTimeline"))
        assertTrue(pen.contains("drawWithContent"))
    }
}
