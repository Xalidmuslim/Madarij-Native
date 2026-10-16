package ru.madarij.nativeapp

/**
 * Source-preserving classification for DISPLAY ONLY. Never alter the Arabic
 * original, Russian translation, source references or database paragraph IDs.
 *
 * Quran references take precedence over indefinite "Он сказал" / "передача".
 * A statement attributed to a named human is NEVER inferred to be a hadith.
 */
internal enum class ReaderCitationKind { VERSE, PROPHETIC_REPORT, COMPANION_SAYING, SAYING }

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

private val sourceSurahRef = Regex(
    """(?i)\(\s*сура\s+[«„"][^»“"]{2,90}[»“"]\s*,\s*\d{1,3}\s*:\s*\d{1,3}(?:\s*[–—-]\s*\d{1,3})?\s*\)"""
)
// Full Quran source markers may have editorial qualifications after the
// ayah range, e.g. [аль-Аср, 103:1–3; перевод смысла] or
// [«Аль-Маида», 5:60; цитируется часть аята]. Never leave those words
// behind in author prose; the WHOLE bracket belongs to the source marker.
private val bracketSurahRef = Regex(
    """\[\s*(?:[«„“"][^»“”"\]\n]{2,90}[»“”"]|[^,\[\]\n]{2,90})\s*,\s*\d{1,3}\s*:\s*\d{1,3}(?:\s*[–—-]\s*\d{1,3})?(?:\s*;\s*[^\[\]\n;]{2,100})?\s*]"""
)
private val colonSurahRef = Regex(
    """\[\s*[^:\[\]\n]{2,65}:\s*\d{1,3}(?:[–—-]\d{1,3})?\s*]"""
)
private val bareSurahRef = Regex(
    """\(\s*сура\s+[«„"][^»“"]{2,90}[»“"]\s*,\s*\d{1,3}\s*:\s*\d{1,3}(?:[–—-]\d{1,3})?\s*\)""",
    RegexOption.IGNORE_CASE
)
private val bareSurahWord = Regex("""(?i)(?:\bсура\b|^\s*[«„"]?(?:аль-|ан-|ат-|аш-|аз-|ар-|ас-|ад-|ал-))""")
private val misleadingHadithReference = Regex(
    """(?i)Бухари|Муслим|Тирмизи|Насаи|Абу\s+Дауд|Ибн\s+Маджа|Ахмад|хадис"""
)
private val explicitDivineLead = Regex(
    """(?i)^(?:и\s+|также\s+|затем\s+|после\s+этого\s+)?(?:Всевышний(?:\s+Аллах)?|Аллах|Господь(?:\s+миров)?|Всемогущий(?:\s+и\s+Великий)?\s+Аллах|Он)\s+(?:также\s+)?(?:сказал|говорит|повелел)\s*:\s*$"""
)
private val namedDivineWithoutReference = Regex(
    """(?i)^(?:и\s+|также\s+|затем\s+)?(?:Всевышний(?:\s+Аллах)?|Аллах|Господь(?:\s+миров)?|Всемогущий(?:\s+и\s+Великий)?\s+Аллах)\s+(?:также\s+)?(?:сказал|говорит|повелел)\s*:\s*$"""
)
private val explicitPropheticLead = Regex(
    """(?i)^(?:и\s+|также\s+)?(?:Пророк(?:\s*,[^:]{0,65})?|Посланник\s+Аллаха(?:\s*,[^:]{0,65})?)\s+(?:сказал|сообщил|передал)\s*:\s*$"""
)
private val speakerLead = Regex("""(?i)^(.{2,190}?)\b(сказал|сказала|передал|говорил|объяснил)\s*:\s*$""")
private val knownHuman = Regex(
    """(?i)(?:\bИбн\s+[А-ЯЁа-яё-]+|\bАль-[А-ЯЁа-яё-]+|\bаль-[А-ЯЁа-яё-]+|\bИмам\s+[А-ЯЁа-яё-]+|\bМуджахид\b|\bУмар\b|\bАли\b|\bАбу\s+[А-ЯЁа-яё-]+|\bАбу\s+Хурайра\b|\bШейх\b)"""
)
private val ibnAbbas = Regex("""(?i)^\s*Ибн\s+Аббас\b""")
private val readerNumberIntro = Regex(
    """(?i)^(Первое|Второе|Третье|Четвёртое|Четвертое|Пятое|Шестое|Седьмое|Восьмое|Девятое|Десятое|Одиннадцатое|Двенадцатое|Во-первых|Во-вторых|В-третьих|Во-четвёртых|Во-пятых|\d{1,3}[.)])(?:\s*[:.]\s*|\s+)(.+)$"""
)
private val readerHeader = Regex(
    """(?i)^(?:глава|раздел|степень|ступень|смысл|польза|правило|основа|вопрос|ответ|положение|сторона)(?:\s+[^.!?…]{0,82})?$"""
)

/**
 * Sentence boundaries outside guillemets. Regex split on question marks used
 * to break Quran quotations at "?" inside «…», incorrectly treating the rest
 * of the same verse as a statement by a scholar.
 */
private fun splitOutsideQuotedText(text: String): List<String> {
    val parts = mutableListOf<String>()
    var from = 0
    var guillemetDepth = 0
    var curlyDepth = 0
    var lowCurlyDepth = 0
    for (i in text.indices) {
        when (text[i]) {
            '«' -> guillemetDepth++
            '»' -> guillemetDepth = (guillemetDepth - 1).coerceAtLeast(0)
            '„' -> lowCurlyDepth++
            // Russian nested quotation uses „…“; English uses “…”.
            '“' -> if (lowCurlyDepth > 0) lowCurlyDepth-- else curlyDepth++
            '”' -> curlyDepth = (curlyDepth - 1).coerceAtLeast(0)
        }
        if (guillemetDepth != 0 || curlyDepth != 0 || lowCurlyDepth != 0 ||
            text[i] !in ".!?…»”])") continue
        if (i + 1 >= text.length || !text[i + 1].isWhitespace()) continue
        var next = i + 1
        while (next < text.length && text[next].isWhitespace()) next++
        if (next >= text.length) continue
        if (!text[next].isUpperCase() && text[next] != '«') continue
        // Keep the immediately following scripture source with its quotation.
        if (text[i] == '»' && text.substring(next).startsWith("сура ", true)) continue
        parts += text.substring(from, i + 1).trim()
        from = next
    }
    if (from < text.length) parts += text.substring(from).trim()
    return parts.filter(String::isNotBlank)
}

private fun verseReference(sentence: String): String? {
    val candidates = listOfNotNull(
        sourceSurahRef.find(sentence)?.value,
        bracketSurahRef.find(sentence)?.value,
        colonSurahRef.find(sentence)?.value
    )
    return candidates.firstOrNull { match ->
        !misleadingHadithReference.containsMatchIn(match) &&
            (sourceSurahRef.matches(match) ||
                bareSurahWord.containsMatchIn(match.drop(1)) ||
                match.contains('«') ||
                // "Юсуф: 106" and similar printed Quran references.
                colonSurahRef.matches(match))
    }
}

private fun hasVerseQuotedBeforeReference(sentence: String, reference: String): Boolean {
    val pos = sentence.indexOf(reference)
    if (pos <= 0) return false
    val close = maxOf(sentence.lastIndexOf('»', pos), sentence.lastIndexOf('”', pos))
    if (close < 0) return false
    val intervening = sentence.substring(close + 1, pos)
    return intervening.all { it.isWhitespace() || it in ".,;:!?" } &&
        (sentence.lastIndexOf('«', close) >= 0 || sentence.lastIndexOf('“', close) >= 0)
}

/**
 * Locate ONLY the scripture quote + its printed surah reference. A whole
 * author sentence may contain text both BEFORE and AFTER that citation:
 * "Что же до ... «аят» [«Сура», 89:14], то различие ..."
 * That commentary must never be painted inside a Quran block.
 */
private data class ScriptureSpan(val first: Int, val endExclusive: Int, val reference: String)

private fun findScriptureSpan(text: String): ScriptureSpan? {
    val references = (sourceSurahRef.findAll(text) +
        bracketSurahRef.findAll(text) + colonSurahRef.findAll(text))
        .sortedBy { it.range.first }
        .distinctBy { it.range }
    for (hit in references) {
        val ref = hit.value
        if (misleadingHadithReference.containsMatchIn(ref)) continue
        val validRef = sourceSurahRef.matches(ref) ||
            bareSurahWord.containsMatchIn(ref.drop(1)) ||
            ref.contains('«') || ref.contains('“') || colonSurahRef.matches(ref)
        if (!validRef) continue
        val close = maxOf(text.lastIndexOf('»', hit.range.first - 1),
            text.lastIndexOf('”', hit.range.first - 1))
        if (close < 0) continue
        val between = text.substring(close + 1, hit.range.first)
        // Anything except adjacent punctuation means the reference might
        // describe the AUTHOR'S words rather than the nearest quotation.
        if (!between.all { it.isWhitespace() || it in ".,;:!?" }) continue
        val opening = if (text[close] == '»')
            text.lastIndexOf('«', close) else text.lastIndexOf('“', close)
        if (opening < 0) continue
        val preceding = text.substring(0, opening).trim()
        if (humanIntro(preceding)) continue
        var end = hit.range.last + 1
        // Sentence punctuation immediately after the citation stays next
        // to it. The author's continuation remains a separate paragraph.
        while (end < text.length && text[end] in ".,;:!?") end++
        return ScriptureSpan(opening, end, ref)
    }
    return null
}

private fun humanIntro(prefix: String): Boolean {
    val lead = speakerLead.matchEntire(prefix) ?: return false
    // A relative phrase "сподвижникам Пророка — Он сказал" is not a prophetic
    // narration, and "Он также сказал" does not identify a human at all.
    val clean = lead.groupValues[1].trim()
    return knownHuman.containsMatchIn(clean) && !explicitPropheticLead.matches(prefix)
}

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
        val flat = paragraph.replace(Regex("""[ \t]*\n[ \t]*"""), " ")
            .replace(Regex("""[ \t]+"""), " ").trim()
        val plain = mutableListOf<String>()
        fun flush() {
            if (plain.isNotEmpty()) {
                result += ReaderRichSegment.Paragraph(plain.joinToString(" "))
                plain.clear()
            }
        }
        fun consumePiece(piece: String) {
            if (piece.isBlank()) return
            // Extract a citation *within* an explanatory sentence, leaving
            // its author introduction and conclusion outside the Quran card.
            val cited = findScriptureSpan(piece)
            if (cited != null) {
                flush()
                val prefix = piece.substring(0, cited.first).trimEnd()
                val divine = explicitDivineLead.matches(prefix)
                val citation = piece.substring(cited.first, cited.endExclusive)
                if (prefix.isNotBlank() && !divine) {
                    result += ReaderRichSegment.Paragraph(prefix)
                }
                result += ReaderRichSegment.Quote(
                    raw = if (divine) piece.substring(0, cited.endExclusive) else citation,
                    label = "", body = citation,
                    reference = cited.reference, kind = ReaderCitationKind.VERSE
                )
                val trailing = piece.substring(cited.endExclusive).trimStart()
                if (trailing.isNotBlank()) consumePiece(trailing)
                return
            }

            val quoteAt = listOf(piece.indexOf('«'), piece.indexOf('“'))
                .filter { it >= 0 }.minOrNull() ?: -1
            val quoteEnd = maxOf(piece.lastIndexOf('»'), piece.lastIndexOf('”'))
            val prefix = if (quoteAt >= 0) piece.substring(0, quoteAt).trim() else ""
            val namedHuman = humanIntro(prefix)
            val clearProphetic = explicitPropheticLead.matches(prefix)
            // With no traceable printed source, only an explicit divine
            // attribution may receive Quran styling; vague "Он сказал" isn't
            // sufficient and a named person must not become an ayah.
            val clearDivine = !namedHuman && namedDivineWithoutReference.matches(prefix)
            val numberMatch = readerNumberIntro.matchEntire(piece)
            when {
                quoteAt >= 0 && quoteEnd > quoteAt && clearDivine -> {
                    flush()
                    val quotePart = piece.substring(quoteAt, quoteEnd + 1)
                    result += ReaderRichSegment.Quote(
                        raw = piece.substring(0, quoteEnd + 1),
                        label = "", body = quotePart,
                        reference = null, kind = ReaderCitationKind.VERSE
                    )
                    val after = piece.substring(quoteEnd + 1).trimStart()
                    if (after.isNotBlank()) consumePiece(after)
                }
                quoteAt >= 0 && quoteEnd > quoteAt && clearProphetic -> {
                    flush()
                    result += ReaderRichSegment.Quote(
                        raw = piece, label = prefix, body = piece.substring(quoteAt),
                        reference = null, kind = ReaderCitationKind.PROPHETIC_REPORT
                    )
                }
                quoteAt >= 0 && quoteEnd > quoteAt && namedHuman -> {
                    flush()
                    result += ReaderRichSegment.Quote(
                        raw = piece, label = prefix, body = piece.substring(quoteAt),
                        reference = null,
                        kind = if (ibnAbbas.containsMatchIn(prefix))
                            ReaderCitationKind.COMPANION_SAYING else ReaderCitationKind.SAYING
                    )
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
        for (piece in splitOutsideQuotedText(flat)) {
            consumePiece(piece)
        }
        flush()
    }
    return result
}
