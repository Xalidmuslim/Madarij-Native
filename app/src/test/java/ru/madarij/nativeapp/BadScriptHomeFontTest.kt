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
        val fontBytes = File("src/main/res/font/bad_script.ttf").readBytes()
        val russian = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" +
            "абвгдеёжзийклмнопрстуфхцчшщъыьэюя"
        val missing = russian.filterNot { hasTrueTypeGlyph(fontBytes, it.code) }
        assertTrue("Missing Cyrillic glyphs in Bad Script: $missing", missing.isEmpty())
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
        assertTrue(home.contains("fontSize = CompactUi.homeQuoteSizeSp.sp"))
        assertTrue(home.contains("lineHeight = CompactUi.homeQuoteLineHeightSp.sp"))
        assertTrue(home.contains("fontWeight = FontWeight.Bold"))
        assertFalse(home.contains("Font(R.font.caveat_handwriting)"))
        assertTrue(animation.contains("ReedPenCharacterTimeline"))
        assertTrue(animation.contains("drawWithContent"))
    }
    // AWT is unavailable on the Android JVM bootstrap classpath.
    // Read the standard TrueType Unicode cmap directly; no font-rendering
    // framework, emulator, additional dependency or network access needed.
    private fun hasTrueTypeGlyph(data: ByteArray, unicode: Int): Boolean {
        fun u16(index: Int) = (
            ((data[index].toInt() and 255) shl 8) or
                (data[index + 1].toInt() and 255)
        )
        fun u32(index: Int) = (
            ((data[index].toLong() and 255) shl 24) or
                ((data[index + 1].toLong() and 255) shl 16) or
                ((data[index + 2].toLong() and 255) shl 8) or
                (data[index + 3].toLong() and 255)
        )
        require(data.size >= 12) { "Invalid TTF header" }
        val count = u16(4)
        val table = (0 until count).map { 12 + it * 16 }
            .firstOrNull { offset ->
                offset + 16 <= data.size &&
                    String(data, offset, 4, Charsets.US_ASCII) == "cmap"
            } ?: error("No Unicode character map in TTF")
        val cmap = u32(table + 8).toInt()
        require(cmap + 4 <= data.size)
        val subtables = u16(cmap + 2)
        for (n in 0 until subtables) {
            val rec = cmap + 4 + n * 8
            val platform = u16(rec)
            val encoding = u16(rec + 2)
            if (platform != 0 && !(platform == 3 && encoding in setOf(1, 10))) continue
            val start = cmap + u32(rec + 4).toInt()
            when (u16(start)) {
                4 -> {
                    val segments = u16(start + 6) / 2
                    val ends = start + 14
                    val starts = ends + 2 * segments + 2
                    val deltas = starts + 2 * segments
                    val ranges = deltas + 2 * segments
                    for (i in 0 until segments) {
                        val first = u16(starts + 2 * i)
                        val last = u16(ends + 2 * i)
                        if (unicode !in first..last) continue
                        val delta = u16(deltas + 2 * i)
                        val offset = u16(ranges + 2 * i)
                        val glyph = if (offset == 0) (unicode + delta) and 0xffff
                        else {
                            val address = ranges + 2 * i + offset + 2 * (unicode - first)
                            if (address < 0 || address + 2 > data.size) 0
                            else (u16(address) + delta) and 0xffff
                        }
                        if (glyph != 0) return true
                    }
                }
                12 -> {
                    val groups = u32(start + 12).toInt()
                    for (i in 0 until groups) {
                        val entry = start + 16 + 12 * i
                        val first = u32(entry).toInt()
                        val last = u32(entry + 4).toInt()
                        if (unicode in first..last && u32(entry + 8) + unicode - first != 0L)
                            return true
                    }
                }
            }
        }
        return false
    }

}
