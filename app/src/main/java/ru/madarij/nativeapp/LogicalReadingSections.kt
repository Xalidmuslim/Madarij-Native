package ru.madarij.nativeapp

import ru.madarij.nativeapp.data.Chapter

/**
 * Original draft corpus consists of 90 page slices, not 90 author chapters.
 * Only these 30 slices begin with the standalone source heading "Раздел".
 * All others are continuous parts of the preceding section.
 * IDs and database rows stay unchanged to preserve bookmarks and progress.
 */
internal object LogicalReadingSections {
    val starts: List<String> = listOf(
        "madarij-v1-opening-001-section",
        "madarij-v1-p021-draft-section",
        "madarij-v1-p028-draft-section",
        "madarij-v1-p035-draft-section",
        "madarij-v1-p057-draft-section",
        "madarij-v1-p061-draft-section",
        "madarij-v1-p075-draft-section",
        "madarij-v1-p115-draft-section",
        "madarij-v1-p132-draft-section",
        "madarij-v1-p159-draft-section",
        "madarij-v1-p188-draft-section",
        "madarij-v1-p224-draft-section",
        "madarij-v1-p242-draft-section",
        "madarij-v1-p244-draft-section",
        "madarij-v1-p255-draft-section",
        "madarij-v1-p259-draft-section",
        "madarij-v1-p314-draft-section",
        "madarij-v1-p318-draft-section",
        "madarij-v1-p380-draft-section",
        "madarij-v1-p390-draft-section",
        "madarij-v1-p391-draft-section",
        "madarij-v1-p409-draft-section",
        "madarij-v1-p424-draft-section",
        "madarij-v1-p444-draft-section",
        "madarij-v1-p456-draft-section",
        "madarij-v1-p479-draft-section",
        "madarij-v1-p535-draft-section",
        "madarij-v1-p553-draft-section",
        "madarij-v1-p566-draft-section",
        "madarij-v1-p609-draft-section"
    )
    private val pages: Map<String, Pair<Int,Int>> = mapOf(
        "madarij-v1-opening-001-section" to (3 to 21),
        "madarij-v1-p021-draft-section" to (21 to 27),
        "madarij-v1-p028-draft-section" to (28 to 34),
        "madarij-v1-p035-draft-section" to (35 to 56),
        "madarij-v1-p057-draft-section" to (57 to 60),
        "madarij-v1-p061-draft-section" to (61 to 74),
        "madarij-v1-p075-draft-section" to (75 to 114),
        "madarij-v1-p115-draft-section" to (115 to 131),
        "madarij-v1-p132-draft-section" to (132 to 158),
        "madarij-v1-p159-draft-section" to (159 to 187),
        "madarij-v1-p188-draft-section" to (188 to 223),
        "madarij-v1-p224-draft-section" to (224 to 241),
        "madarij-v1-p242-draft-section" to (242 to 243),
        "madarij-v1-p244-draft-section" to (244 to 254),
        "madarij-v1-p255-draft-section" to (255 to 258),
        "madarij-v1-p259-draft-section" to (259 to 313),
        "madarij-v1-p314-draft-section" to (314 to 317),
        "madarij-v1-p318-draft-section" to (318 to 379),
        "madarij-v1-p380-draft-section" to (380 to 389),
        "madarij-v1-p390-draft-section" to (390 to 390),
        "madarij-v1-p391-draft-section" to (391 to 408),
        "madarij-v1-p409-draft-section" to (409 to 423),
        "madarij-v1-p424-draft-section" to (424 to 443),
        "madarij-v1-p444-draft-section" to (444 to 455),
        "madarij-v1-p456-draft-section" to (456 to 478),
        "madarij-v1-p479-draft-section" to (479 to 534),
        "madarij-v1-p535-draft-section" to (535 to 552),
        "madarij-v1-p553-draft-section" to (553 to 565),
        "madarij-v1-p566-draft-section" to (566 to 608),
        "madarij-v1-p609-draft-section" to (609 to 610)
    )
    fun roots(chapters: List<Chapter>): List<Chapter> = chapters.filter { it.id in starts }
    fun members(chapters: List<Chapter>, requestedId: String): List<Chapter> {
        val index = chapters.indexOfFirst { it.id == requestedId }
        if (index < 0) return emptyList()
        val first = (index downTo 0).firstOrNull { chapters[it].id in starts } ?: 0
        val end = (indexOfNextStart(chapters, first + 1)).takeIf { it >= 0 } ?: chapters.size
        return chapters.subList(first, end)
    }
    private fun indexOfNextStart(chapters: List<Chapter>, start: Int): Int =
        (start until chapters.size).firstOrNull { chapters[it].id in starts } ?: -1
    fun pageRange(rootId: String): String? = pages[rootId]?.let { (a,b) -> "Страницы ${a}–${b}" }
}
