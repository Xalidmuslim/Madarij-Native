package ru.madarij.nativeapp

import java.lang.Character.UnicodeScript

internal sealed interface ReaderTextBlock {
    data class Body(val text: String) : ReaderTextBlock
    data class Section(val label: String, val title: String? = null) : ReaderTextBlock
}

/** Splits only standalone source section markers; all author wording remains in order. */
internal fun splitReaderText(text: String, marker: String, label: String): List<ReaderTextBlock> {
    if (text.isBlank() || marker.isBlank()) return emptyList()
    val matches = Regex("(?m)^${Regex.escape(marker)}[ \\t]*$").findAll(text).toList()
    if (matches.isEmpty()) return listOf(ReaderTextBlock.Body(text))

    val result = mutableListOf<ReaderTextBlock>()
    var cursor = 0
    for (match in matches) {
        addBody(result, text.substring(cursor, match.range.first))

        var contentStart = match.range.last + 1
        while (contentStart < text.length && text[contentStart] in "\r\n") contentStart++
        val lineEnd = text.indexOf('\n', contentStart).let { if (it < 0) text.length else it }
        val possibleTitle = text.substring(contentStart, lineEnd).trimEnd('\r', ' ', '\t')
        val nextStart = (lineEnd + 1).coerceAtMost(text.length)
        val nextLine = if (lineEnd < text.length) {
            val rest = text.substring(nextStart).trimStart('\r', '\n', ' ', '\t')
            rest.substringBefore('\n').trimEnd('\r', ' ', '\t')
        } else ""
        val title = possibleTitle.takeIf { looksLikeHeading(it, nextLine) }
        result += ReaderTextBlock.Section(label, title)
        cursor = if (title != null) nextStart else match.range.last + 1
    }
    addBody(result, text.substring(cursor))
    return result
}

private fun looksLikeHeading(line: String, nextLine: String): Boolean {
    val title = line.trim()
    if (title.isEmpty() || title.length > 120 || title.split(Regex("\\s+")).size > 18) return false
    if (Regex("[.!?؟…]\\s*[»”\\\"]?$").containsMatchIn(title)) return false
    val isArabic = title.any { UnicodeScript.of(it.code) == UnicodeScript.ARABIC }
    if (isArabic) return true
    val nextInitial = nextLine.firstOrNull { it.isLetter() } ?: return nextLine.isBlank()
    return nextInitial.isUpperCase()
}

private fun addBody(target: MutableList<ReaderTextBlock>, text: String) {
    val body = text.trim('\r', '\n')
    if (body.isNotBlank()) target += ReaderTextBlock.Body(body)
}
