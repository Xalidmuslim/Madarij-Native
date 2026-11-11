package ru.madarij.nativeapp

/** Title searches and unordered FTS matches need not have a literal body match. */
@Suppress("UNUSED_PARAMETER")
internal fun readerSearchTargetExists(query: String, arabic: String, russian: String): Boolean {
    val needle = normalizedText(query).text
    return needle.isNotBlank() &&
        normalizedText(russianOnlyText(russian)).text.contains(needle)
}
