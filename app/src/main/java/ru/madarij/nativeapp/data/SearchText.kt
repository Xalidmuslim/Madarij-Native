package ru.madarij.nativeapp.data

import java.util.Locale

// Import must rebuild FTS entries when normalization changes, even if book bytes do not.
internal const val SEARCH_INDEX_REVISION = "fts4-unicode-boundaries-v2"

internal fun normalizeSearchText(text: String): String = text.lowercase(Locale.ROOT)
    .replace(Regex("[\u064B-\u065F\u0670\u0640\u06D6-\u06ED]"), "")
    .replace(Regex("[أإآٱ]"), "ا").replace('ё', 'е')
    // FTS's default simple tokenizer treats many non-ASCII punctuation marks
    // as part of a word. Give index and query the same plain word boundaries.
    .replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
