package ru.madarij.nativeapp

/**
 * A topic (chapter) selection takes precedence over the volume tab.
 * Empty topic uses the selected volume; volume 0 means both volumes.
 * This is shared by all Study tabs and remains independent of progress.
 */
internal fun studyScopeMatches(
    candidateChapterId: String,
    selectedChapterId: String,
    selectedVolume: Int,
    chapterVolumes: Map<String, Int>
): Boolean {
    if (selectedChapterId.isNotEmpty()) return candidateChapterId == selectedChapterId
    return selectedVolume == 0 || chapterVolumes[candidateChapterId] == selectedVolume
}

/** Choosing a different volume clears only a topic outside that volume. */
internal fun selectedStudyChapterAfterVolumeChange(
    selectedChapterId: String,
    newVolume: Int,
    chapterVolumes: Map<String, Int>
): String = selectedChapterId.takeIf {
    it.isNotEmpty() && newVolume in 1..2 && chapterVolumes[it] == newVolume
} ?: ""
