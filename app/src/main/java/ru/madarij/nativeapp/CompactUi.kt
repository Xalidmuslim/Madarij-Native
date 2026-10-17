package ru.madarij.nativeapp

/**
 * Density targets for non-reader navigation and dashboard screens.
 *
 * These values deliberately do not affect the actual Russian/Arabic book text,
 * accessibility font scaling, saved reader preferences or leather bottom bar.
 * Cards use minimum heights: longer text may grow naturally.
 */
internal object CompactUi {
    const val headerTitleSp = 26
    const val sectionTitleSp = 17
    const val contentsGroupMinHeightDp = 72
    const val tocMedallionDp = 42
    const val tocManuscriptIconDp = 32
    const val tocVolumeMinHeightDp = 48
    const val tocFilterMinHeightDp = 48
    const val homeQuoteSizeSp = 18
    const val homeQuoteLineHeightSp = 29
    const val menuRowMinHeightDp = 60
    const val progressRingDp = 106
    const val activityGraphDp = 118
    const val listGapDp = 8
    const val choiceMinHeightDp = 48
    const val actionMinHeightDp = 48
    const val settingsSwitchMinHeightDp = 48
}
