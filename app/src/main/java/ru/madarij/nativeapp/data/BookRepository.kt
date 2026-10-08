package ru.madarij.nativeapp.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale

class BookRepository(private val context: Context, private val db: BookDatabase) {
    val dao = db.dao()
    var importedDraft: Boolean = false
        private set
    suspend fun importCorpus(): Boolean = withContext(Dispatchers.IO) {
        val bytes = context.assets.open("corpus.json").use { it.readBytes() }
        val structureBytes = try { context.assets.open("structure.json").use { it.readBytes() } } catch (_: java.io.FileNotFoundException) { ByteArray(0) }
        val titles = HashMap<String, String>()
        if (structureBytes.isNotEmpty()) {
            val groups = JSONObject(structureBytes.toString(Charsets.UTF_8)).getJSONArray("groups")
            for (g in 0 until groups.length()) {
                val items = groups.getJSONObject(g).getJSONArray("items")
                for (n in 0 until items.length()) { val item = items.getJSONObject(n); titles[item.getString("chapterId")] = item.getString("title") }
            }
        }
        val root = JSONObject(bytes.toString(Charsets.UTF_8))
        val edition = root.getJSONObject("edition")
        val chapters = root.getJSONArray("chapters")
        val draft = edition.optBoolean("draft")
        val reviewed = edition.optBoolean("structure_verified") && edition.optString("rights_status") == "cleared"
        val authorizedDraft = draft && edition.optString("author_text_rights") == "public_domain" &&
            edition.optString("translation_rights") == "own_draft_not_independently_checked"
        if (chapters.length() == 0 || (!reviewed && !authorizedDraft)) return@withContext false
        importedDraft = draft
        val hash = sha(bytes + structureBytes)
        if (dao.contentVersion()?.sha256 == hash) return@withContext true
        val expected = root.getJSONArray("expected_chapter_ids")
        require(expected.length() == chapters.length()) { "Chapter manifest mismatch" }
        val chapterRows = ArrayList<Chapter>()
        val paragraphRows = ArrayList<Paragraph>()
        val indexRows = ArrayList<SearchIndex>()
        val chapterIds = HashSet<String>()
        val paragraphIds = HashSet<String>()
        for (i in 0 until chapters.length()) {
            val c = chapters.getJSONObject(i)
            val id = c.getString("id")
            require(id == expected.getString(i) && c.getInt("order") == i && chapterIds.add(id)) { "Invalid chapter manifest at $i" }
            val chapter = Chapter(id, c.getInt("volume"), i, titles[id] ?: c.getString("title"), c.getString("source"))
            chapterRows.add(chapter)
            val ps = c.getJSONArray("paragraphs")
            val expectedPs = c.getJSONArray("expected_paragraph_ids")
            require(ps.length() > 0 && ps.length() == expectedPs.length()) { "Paragraph manifest mismatch at $i" }
            for (j in 0 until ps.length()) {
                val p = ps.getJSONObject(j)
                val pid = p.getString("id")
                require(pid == expectedPs.getString(j) && p.getInt("order") == j && paragraphIds.add(pid)) { "Invalid paragraph manifest at $i/$j" }
                val ar = p.getString("ar"); val ru = p.getString("ru")
                require(ru.isNotBlank() && (ar.isNotBlank() || (draft && p.optString("role") == "editor_note"))) { "Empty text at $i/$j" }
                if (!draft) {
                    require(p.getString("status") == "reviewed" && p.getString("reviewer").isNotBlank())
                    require(sha((ar + "\u0000" + ru).toByteArray(Charsets.UTF_8)) == p.getString("reviewed_sha256"))
                } else {
                    require(p.optString("status") in setOf("draft_unchecked", "draft_self_checked", "literary_reviewed_v1", "reviewed")) { "Unknown editorial status at $i/$j" }
                }
                paragraphRows.add(Paragraph(pid, id, j, ar, ru, p.getString("role"), p.getString("source")))
                indexRows.add(SearchIndex(pid, normalize(chapter.title + " " + ar + " " + ru)))
            }
        }
        db.withTransaction {
            dao.clearIndex(); dao.clearParagraphs(); dao.clearChapters()
            dao.insertChapters(chapterRows)
            paragraphRows.chunked(500).forEach { dao.insertParagraphs(it) }
            indexRows.chunked(500).forEach { dao.insertIndex(it) }
            // Stable IDs keep all personal records intact across corpus updates.
            dao.contentVersion(ContentVersion(sha256 = hash))
        }
        true
    }
    suspend fun search(text: String, filter: String = "all"): List<Paragraph> = withContext(Dispatchers.IO) {
        val tokens = normalize(text).split(Regex("[^\\p{L}\\p{N}]+")).filter(String::isNotBlank).take(20)
        if (tokens.isEmpty()) return@withContext emptyList()
        if (filter == "title" || filter == "titles") {
            return@withContext dao.allChapters().filter { c -> tokens.all { normalize(c.title).contains(it) } }
                .flatMap { dao.paragraphs(it.id).take(1) }
        }
        dao.search(tokens.joinToString(" AND ") { "\"$it\"*" }).filter { p ->
            when (filter) {
                "note", "notes" -> p.role == "editor_note"
                "ayat", "quran" -> p.role != "editor_note" && (p.ar.contains("قال تعالى") || p.ar.contains("قوله تعالى") || p.ar.contains("قال الله") || p.ru.contains("Всевышний сказал", true))
                "hadith" -> p.role != "editor_note" && (p.ar.contains("رسول الله") || p.ar.contains("النبي") || p.ru.contains("Посланник Аллаха", true) || p.ru.contains("Пророк", true))
                else -> true
            }
        }
    }
    private fun normalize(text: String): String = text.lowercase(Locale.ROOT)
        .replace(Regex("[\u064B-\u065F\u0670\u0640\u06D6-\u06ED]"), "")
        .replace(Regex("[أإآٱ]"), "ا").replace('ё', 'е')
    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
