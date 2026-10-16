package ru.madarij.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class CaveatHomeFontTest {
    @Test fun officialCaveatFontIsBundledWithoutNetworkDependency() {
        val file = File("src/main/res/font/caveat_handwriting.ttf")
        assertTrue("Caveat must be packaged as an Android font resource", file.isFile)
        val bytes = file.readBytes()
        assertEquals(403648, bytes.size)
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update("blob ${bytes.size}\u0000".toByteArray(Charsets.UTF_8))
        digest.update(bytes)
        val gitSha = digest.digest().joinToString("") { "%02x".format(it) }
        assertEquals("f84acf2ce2d1b3329b194749530dbaec047e6df6", gitSha)
        val license = File("src/main/assets/fonts/Caveat-OFL.txt").readText()
        assertTrue(license.contains("SIL OPEN FONT LICENSE Version 1.1"))
        assertTrue(license.contains("The Caveat Project Authors"))
    }

    @Test fun previouslyBundledCaveatIsNotAppliedToAnimatedQuotes() {
        val home = File("src/main/java/ru/madarij/nativeapp/HomeContents.kt").readText()
        assertFalse(home.contains("Font(R.font.caveat_handwriting)"))
    }
}
