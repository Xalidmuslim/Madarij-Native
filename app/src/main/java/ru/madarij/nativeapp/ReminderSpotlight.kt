package ru.madarij.nativeapp

/** A navigation-only, ephemeral decoration; never written back to the book. */
internal fun reminderSpotlightRange(text: String, reminder: String): IntRange? {
    val quote = reminder.trim()
    if (quote.isEmpty() || text.isEmpty()) return null
    val exact = text.indexOf(quote, ignoreCase = true)
    if (exact >= 0) return exact until (exact + quote.length)
    // Book rendering sometimes joins adjacent printed pages into one text block.
    // A unique prefix still identifies the same source passage accurately.
    val prefix = quote.take(64).trimEnd()
    if (prefix.length < 24) return null
    val start = text.indexOf(prefix, ignoreCase = true)
    if (start < 0 || text.indexOf(prefix, start + 1, ignoreCase = true) >= 0) return null
    return start until (start + prefix.length)
}
