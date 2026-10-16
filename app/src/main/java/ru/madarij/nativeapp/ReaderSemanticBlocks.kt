package ru.madarij.nativeapp

/**
 * Presentation-only semantic rendering. This parser never modifies the corpus.
 * We deliberately classify quotations conservatively: a generic "сказал" is
 * NOT evidence that a narration is a hadith.
 */
internal enum class ReaderCitationKind { VERSE, PROPHETIC_REPORT, SAYING }

internal sealed interface ReaderRichSegment {
    val raw: String
    data class Paragraph(override val raw: String) : ReaderRichSegment
    data class Quote(
        override val raw: String,
        val label: String,
        val body: String,
        val reference: String?,
        val kind: ReaderCitationKind
    ) : ReaderRichSegment
    data class Numbered(override val raw: String, val label: String, val body: String) : ReaderRichSegment
    data class Heading(override val raw: String) : ReaderRichSegment
    data class Poetry(override val raw: String) : ReaderRichSegment
}

private val readerSentenceGap = Regex("""(?<=[.!?…»\]])\s+(?=[А-ЯЁ«])""")
private val readerVerseReference = Regex("""\[[^\]]*\d{1,3}:\d{1,3}(?:[–-]\d{1,3})?[^\]]*]""")
private val readerLead = Regex("""^(.{2,135}?\bсказал(?:а|и)?\s*:)\s*(?=[«"])""", RegexOption.IGNORE_CASE)
private val readerVerseIntro = Regex(
    """(?i)(?:Всевышний|Аллах|в Коране|аят|сура)\b"""
)
private val readerProphetIntro = Regex(
    """(?i)(?:Пророк(?:а|ом|у)?|Посланник(?:а|ом|у)?\s+Аллаха|ﷺ)\b"""
)
private val readerNumberIntro = Regex(
    """(?i)^(Первое|Второе|Третье|Четвёртое|Четвертое|Пятое|Шестое|Седьмое|Восьмое|Девятое|Десятое|Одиннадцатое|Двенадцатое|Во-первых|Во-вторых|В-третьих|Во-четвёртых|Во-пятых|\d{1,3}[.)])(?:\s*[:.]\s*|\s+)(.+)$"""
)
private val readerHeader = Regex(
    """(?i)^(?:глава|раздел|степень|ступень|смысл|польза|правило|основа|вопрос|ответ|положение|сторона)(?:\s+[^.!?…]{0,82})?$"""
)

internal fun parseReaderRichSegments(text: String): List<ReaderRichSegment> {
    val paragraphs = text.replace("\r", "")
        .split(Regex("""\n[ \t]*\n+"""))
        .map(String::trim).filter(String::isNotBlank)
    val result = mutableListOf<ReaderRichSegment>()
    for (paragraph in paragraphs) {
        if (paragraph.startsWith("«") && paragraph.contains('\n')) {
            result += ReaderRichSegment.Poetry(paragraph)
            continue
        }
        if (paragraph.length <= 110 && readerHeader.matches(paragraph)) {
            result += ReaderRichSegment.Heading(paragraph)
            continue
        }
        // Preserve every word and all punctuation. Only formatting whitespace
        // between the blocks is normalized, as in the existing reader.
        val flat = paragraph.replace(Regex("""[ \t]*\n[ \t]*"""), " ")
            .replace(Regex("""[ \t]+"""), " ").trim()
        val pieces = readerSentenceGap.split(flat).filter(String::isNotBlank)
        val plain = mutableListOf<String>()
        fun flush() {
            if (plain.isNotEmpty()) {
                result += ReaderRichSegment.Paragraph(plain.joinToString(" "))
                plain.clear()
            }
        }
        for (piece in pieces) {
            val intro = readerLead.find(piece)
            val quoteAt = piece.indexOf('«')
            val quoteEnd = piece.lastIndexOf('»')
            val verseRef = readerVerseReference.find(piece)?.value
            val explicitVerse = readerVerseIntro.containsMatchIn(piece.substringBefore('«'))
            val isVerse = quoteAt >= 0 && quoteEnd > quoteAt &&
                ((verseRef != null && (explicitVerse || quoteAt == 0)) ||
                    (intro != null && readerVerseIntro.containsMatchIn(intro.groupValues[1]) &&
                        !readerProphetIntro.containsMatchIn(intro.groupValues[1])))
            val isReport = intro != null &&
                readerProphetIntro.containsMatchIn(intro.groupValues[1])
            val isSaying = intro != null && !isReport && !isVerse
            val numberMatch = readerNumberIntro.matchEntire(piece)
            when {
                (isVerse || isReport || isSaying) && quoteAt >= 0 && quoteEnd > quoteAt -> {
                    flush()
                    val kind = when {
                        isVerse -> ReaderCitationKind.VERSE
                        isReport -> ReaderCitationKind.PROPHETIC_REPORT
                        else -> ReaderCitationKind.SAYING
                    }
                    val label = intro?.groupValues?.get(1)?.trim().orEmpty().ifBlank {
                        if (isVerse) "Аят" else "Передаётся"
                    }
                    val body = if (intro != null) piece.substring(intro.range.last + 1).trimStart() else piece
                    result += ReaderRichSegment.Quote(piece, label, body, verseRef, kind)
                }
                numberMatch != null -> {
                    flush()
                    result += ReaderRichSegment.Numbered(
                        piece, numberMatch.groupValues[1], numberMatch.groupValues[2]
                    )
                }
                else -> plain += piece
            }
        }
        flush()
    }
    return result
}
