package ru.madarij.nativeapp

/**
 * Russian-only presentation of the existing bilingual author corpus.
 *
 * Only removes Arabic glyphs from DISPLAY / copy / search previews. The
 * original Arabic and the full Russian translation stay unchanged in assets
 * and Room so editorial collation and personal reading positions are safe.
 */
private val arabicInBraces = Regex(
    """\{[^{}\n]*[\u0600-\u06FF\u0750-\u077F\u0870-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF][^{}\n]*\}"""
)
private val arabicGlyphs = Regex(
    """[\u0600-\u06FF\u0750-\u077F\u0870-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]+"""
)
private val emptyQuotation = Regex("""«[ \t]*»|„[ \t]*“""")
private val emptyBrackets = Regex("""\([ \t]*\)|\[[ \t]*]""")
private val orphanedDashBeforeRussianQuote = Regex("""(:)[ \t]*[—–][ \t]*(?=[«„"])""")
private val orphanedDashAfterArabic = Regex("""[ \t]+[—–][ \t]*(?=[,;:.!?])""")
private val spacesBeforePunctuation = Regex("""[ \t]+([,;:.!?])""")
private val extraHorizontalSpaces = Regex("""[ \t]{2,}""")

internal fun russianOnlyText(text: String): String {
    if (!arabicGlyphs.containsMatchIn(text)) return text
    return text
        .replace(arabicInBraces, "")
        .replace(arabicGlyphs, "")
        .replace(emptyQuotation, "")
        .replace(emptyBrackets, "")
        .replace(orphanedDashBeforeRussianQuote, "$1 ")
        .replace(orphanedDashAfterArabic, "")
        .replace(spacesBeforePunctuation, "$1")
        .replace(extraHorizontalSpaces, " ")
        .trim()
}
