package ru.madarij.nativeapp

import kotlin.math.abs

internal val primaryTabRoutes = listOf("home", "contents", "study", "more")
internal enum class ReaderPageSwipe { PREVIOUS, NEXT }
internal enum class ReaderEntryKind { CONTINUE, CHAPTER, TOPIC }
internal enum class PrimaryBackAction { STAY, HOME, POP }

internal fun readerEntryParagraph(
    kind: ReaderEntryKind,
    firstParagraphId: String?,
    selectedParagraphId: String?,
    discussionStartParagraphId: String?,
    savedParagraphId: String?
): String? = when (kind) {
    ReaderEntryKind.CONTINUE -> savedParagraphId ?: firstParagraphId
    ReaderEntryKind.CHAPTER -> discussionStartParagraphId ?: firstParagraphId
    ReaderEntryKind.TOPIC -> discussionStartParagraphId ?: selectedParagraphId ?: firstParagraphId
}

internal fun primaryBackAction(route: String): PrimaryBackAction = when {
    route == "home" -> PrimaryBackAction.STAY
    route in primaryTabRoutes -> PrimaryBackAction.HOME
    else -> PrimaryBackAction.POP
}

internal fun shouldOpenContentsRoot(route: String): Boolean = route != "contents"

internal fun selectedMainTab(route: String): String = when {
    route == "home" -> "home"
    route == "contents" || route == "search" || route.startsWith("read/") -> "contents"
    route.startsWith("study") -> "study"
    else -> "more"
}

internal fun classifyReaderSwipe(startX: Float, width: Float, deltaX: Float, deltaY: Float, edgeInset: Float, minDistance: Float): ReaderPageSwipe? {
    if (width <= 0f || !startX.isFinite() || !deltaX.isFinite() || !deltaY.isFinite()) return null
    if (startX <= edgeInset || startX >= width - edgeInset) return null
    if (abs(deltaX) < minDistance || abs(deltaX) < abs(deltaY) * 1.2f) return null
    return if (deltaX < 0f) ReaderPageSwipe.NEXT else ReaderPageSwipe.PREVIOUS
}
