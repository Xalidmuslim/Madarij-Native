package ru.madarij.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompactUiTest {
    @Test fun compactTypographyKeepsHierarchyWithoutGiantTitles() {
        assertEquals(26, CompactUi.headerTitleSp)
        assertEquals(17, CompactUi.sectionTitleSp)
        assertTrue(CompactUi.headerTitleSp > CompactUi.sectionTitleSp)
        assertTrue(CompactUi.sectionTitleSp >= 16)
    }

    @Test fun listsUseDenserMeasuredCards() {
        assertEquals(72, CompactUi.contentsGroupMinHeightDp)
        assertEquals(42, CompactUi.tocMedallionDp)
        assertEquals(60, CompactUi.menuRowMinHeightDp)
        assertEquals(8, CompactUi.listGapDp)
    }

    @Test fun chartsAreUsefulNotOversized() {
        assertEquals(106, CompactUi.progressRingDp)
        assertEquals(118, CompactUi.activityGraphDp)
    }

    @Test fun manuscriptTableOfContentsUsesConsistentCompactAssets() {
        assertEquals(32, CompactUi.tocManuscriptIconDp)
        assertTrue(CompactUi.tocManuscriptIconDp <= CompactUi.tocMedallionDp)
        assertTrue(CompactUi.tocVolumeMinHeightDp >= 48)
        assertTrue(CompactUi.tocFilterMinHeightDp >= 48)
    }

    @Test fun handwritingIsSmallerButRemainsComfortablyReadable() {
        assertEquals(18, CompactUi.homeQuoteSizeSp)
        assertEquals(29, CompactUi.homeQuoteLineHeightSp)
        assertTrue(CompactUi.homeQuoteLineHeightSp > CompactUi.homeQuoteSizeSp)
    }

    @Test fun compactnessDoesNotSacrificeTouchableAreas() {
        assertTrue(CompactUi.choiceMinHeightDp >= 48)
        assertTrue(CompactUi.actionMinHeightDp >= 48)
        assertTrue(CompactUi.settingsSwitchMinHeightDp >= 48)
    }
}
