package ru.madarij.nativeapp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderSearchTargetTest {
    @Test fun titleHitAtStandaloneFaslNeverWaitsForMissingBodyHighlight() {
        assertFalse(readerSearchTargetExists("вкушение", "فصل", "Раздел"))
    }

    @Test fun unorderedFullTextHitStillOpensEvenWithoutAnExactPhrase() {
        assertFalse(readerSearchTargetExists("любовь знание", "", "Знание и любовь идут рядом."))
    }

    @Test fun russianBodyMatchKeepsExactLocatorEnabled() {
        assertTrue(readerSearchTargetExists("ВКУШЕНИЯ", "فصل", "К ним относится и ступень вкушения."))
    }

    @Test fun ArabicSourceMatchDoesNotEnableInvisibleLocator() {
        assertFalse(readerSearchTargetExists("العلم", "العِلْم", "Знание"))
        assertFalse(readerSearchTargetExists("   ", "العلم", "Знание"))
    }
}
