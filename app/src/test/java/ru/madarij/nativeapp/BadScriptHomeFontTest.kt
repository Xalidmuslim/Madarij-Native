package ru.madarij.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class BadScriptHomeFontTest {
    @Test fun officialGoogleFontsBadScriptIsBundledLocally() {
        val file = File("src/main/res/font/bad_script.ttf")
        assertTrue("Bad Script TTF must be generated before tests and packaged as a resource", file.isFile)
        val bytes = file.readBytes()
        assertEquals(184216, bytes.size)
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update("blob ${bytes.size}\u0000".toByteArray(Charsets.UTF_8))
        digest.update(bytes)
        assertEquals(
            "6a2aabeb4b5f7b2d6885fb8b39241c310e383528",
            digest.digest().joinToString("") { "%02x".format(it) }
        )
        val license = File("src/main/assets/fonts/BadScript-OFL.txt").readText()
        assertTrue(license.contains("SIL OPEN FONT LICENSE Version 1.1"))
        assertTrue(license.contains("Bad Script Project Authors"))
    }

    @Test fun fontContainsAllRussianCyrillicGlyphsForQuotes() {
        val font = java.awt.Font.createFont(
            java.awt.Font.TRUETYPE_FONT,
            File("src/main/res/font/bad_script.ttf")
        )
        val russian = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" +
            "абвгдеёжзийклмнопрстуфхцчшщъыьэюя"
        assertEquals("Missing a Russian alphabet glyph in Bad Script",
            -1, font.canDisplayUpTo(russian))
        assertEquals(-1, font.canDisplayUpTo("«Покаяние — милость Аллаха»."))
    }

    @Test fun onlyTheAnimatedHomeQuoteReferencesBadScript() {
        val root = File("src/main/java/ru/madarij/nativeapp")
        val uses = root.walkTopDown().filter { it.isFile && it.extension == "kt" }
            .filter { it.readText().contains("R.font.bad_script") }.map { it.name }.toList()
        assertEquals(listOf("HomeContents.kt"), uses)
        val home = File(root, "HomeContents.kt").readText()
        val animation = File(root, "ReedPenWritingText.kt").readText()
        assertTrue(home.contains("ReedPenWritingText("))
        assertTrue(home.contains("fontFamily = FontFamily(Font(R.font.bad_script))"))
        assertTrue(home.contains("fontSize = 24.sp, lineHeight = 31.sp"))
        assertFalse(home.contains("Font(R.font.caveat_handwriting)"))
        assertTrue(animation.contains("ReedPenCharacterTimeline"))
        assertTrue(animation.contains("drawWithContent"))
    }
}
