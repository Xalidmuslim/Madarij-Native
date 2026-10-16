package ru.madarij.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
        // Inspect the real TrueType 'cmap' (format 4), independently from
        // Android graphics and unavailable java.awt classes.
        val bytes = File("src/main/res/font/bad_script.ttf").readBytes()
        val tt = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        fun u16(offset: Int) = tt.getShort(offset).toInt() and 0xffff

        var cmap = -1
        val tableCount = u16(4)
        for (index in 0 until tableCount) {
            val record = 12 + index * 16
            if (String(bytes, record, 4, Charsets.US_ASCII) == "cmap") {
                cmap = tt.getInt(record + 8)
                break
            }
        }
        assertTrue("TrueType cmap table required", cmap >= 0)

        val format4Tables = buildList {
            for (index in 0 until u16(cmap + 2)) {
                val subtable = cmap + tt.getInt(cmap + 4 + index * 8 + 4)
                if (u16(subtable) == 4) add(subtable)
            }
        }
        assertTrue("TrueType format-4 Unicode cmap required", format4Tables.isNotEmpty())

        fun glyphFor(char: Char, table: Int): Int {
            val code = char.code
            val count = u16(table + 6) / 2
            val ends = table + 14
            val starts = ends + count * 2 + 2
            val deltas = starts + count * 2
            val ranges = deltas + count * 2
            for (i in 0 until count) {
                val start = u16(starts + i * 2)
                val end = u16(ends + i * 2)
                if (code !in start..end) continue
                val delta = u16(deltas + i * 2)
                val rangeOffset = u16(ranges + i * 2)
                if (rangeOffset == 0) return (code + delta) and 0xffff
                val address = ranges + i * 2 + rangeOffset + (code - start) * 2
                if (address > bytes.size - 2) return 0
                val glyph = u16(address)
                return if (glyph == 0) 0 else (glyph + delta) and 0xffff
            }
            return 0
        }
        val russian = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя"
        val missing = russian.filter { char -> format4Tables.none { glyphFor(char, it) != 0 } }
        assertTrue("Bad Script lacks Cyrillic glyphs: $missing", missing.isEmpty())
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
