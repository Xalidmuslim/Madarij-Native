package ru.madarij.nativeapp

/** Title searches and unordered FTS matches need not have a literal body match. */
internal fun readerSearchTargetExists(query: String, arabic: String, russian: String): Boolean {
    val needle = normalizedText(query).text
    return needle.isNotBlank() &&
        (normalizedText(russian).text.contains(needle) || normalizedText(arabic).text.contains(needle))
}
