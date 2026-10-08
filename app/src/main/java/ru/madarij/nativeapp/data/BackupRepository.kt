package ru.madarij.nativeapp.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Personal data only; importing never replaces the authored corpus. */
class BackupRepository(context: Context, private val db: BookDatabase, private val settings: SettingsRepository) {
    private val dao = db.dao()
    suspend fun export(): JSONObject = withContext(Dispatchers.IO) {
        val result = JSONObject().put("format", "madarij-personal").put("version", 1)
        db.withTransaction {
            result.put("positions", JSONArray().apply { dao.allPositions().forEach { put(JSONObject().put("chapterId", it.chapterId).put("paragraphId", it.paragraphId).put("offset", it.offset).put("updatedAt", it.updatedAt)) } })
            result.put("read", JSONArray().apply { dao.allRead().forEach { put(JSONObject().put("chapterId", it.chapterId).put("readAt", it.readAt)) } })
            result.put("bookmarks", JSONArray().apply { dao.allBookmarks().forEach { put(JSONObject().put("id", it.id).put("chapterId", it.chapterId).put("paragraphId", it.paragraphId ?: JSONObject.NULL).put("title", it.title).put("note", it.note)) } })
            result.put("notes", JSONArray().apply { dao.notesSnapshot().forEach { put(JSONObject().put("id", it.id).put("chapterId", it.chapterId).put("paragraphId", it.paragraphId ?: JSONObject.NULL).put("text", it.text).put("updatedAt", it.updatedAt)) } })
            result.put("history", JSONArray().apply { dao.historySnapshot().forEach { put(JSONObject().put("id", it.id).put("date", it.date).put("chapterId", it.chapterId).put("paragraphId", it.paragraphId).put("seconds", it.seconds)) } })
        }
        val s = settings.settings.first()
        result.put("settings", JSONObject().put("theme", s.theme).put("russianSize", s.russianSize).put("arabicSize", s.arabicSize).put("lineHeight", s.lineHeight).put("russianFont", s.russianFont).put("showArabic", s.showArabic).put("showNotes", s.showNotes).put("textWidth", s.textWidth).put("alignment", s.alignment).put("arabicFont", s.arabicFont).put("brightness", s.brightness).put("reducedMotion", s.reducedMotion))
        result
    }
    suspend fun import(json: JSONObject): Int = withContext(Dispatchers.IO) {
        require(json.optString("format") == "madarij-personal" && json.optInt("version") == 1) { "Unsupported backup format" }
        var count = 0
        val chapters = dao.allChapters().map { it.id }.toSet()
        suspend fun valid(c: String, p: String?): Boolean = c in chapters && (p == null || dao.paragraph(p)?.chapterId == c)
        fun paragraph(o: JSONObject): String? = if (o.isNull("paragraphId")) null else o.optString("paragraphId").takeIf { it.isNotBlank() }
        suspend fun each(key: String, action: suspend (JSONObject) -> Unit) {
            val arr = json.optJSONArray(key) ?: return
            require(arr.length() <= 100000) { "Backup too large" }
            for (i in 0 until arr.length()) action(arr.getJSONObject(i))
        }
        db.withTransaction {
            each("positions") { o -> val c = o.getString("chapterId"); val p = paragraph(o)
                if (p != null && valid(c,p)) { val v = Position(c,p,o.optInt("offset").coerceAtLeast(0),o.optLong("updatedAt").coerceAtLeast(0)); if ((dao.position(c)?.updatedAt ?: -1) <= v.updatedAt) { dao.position(v); count++ } } }
            each("read") { o -> val c=o.getString("chapterId"); if(valid(c,null)) { dao.read(ReadChapter(c,o.optLong("readAt").coerceAtLeast(0))); count++ } }
            each("bookmarks") { o -> val c=o.getString("chapterId"); val p=paragraph(o); val id=o.getString("id"); if(id.isNotBlank() && valid(c,p)) { dao.bookmark(Bookmark(id,c,p,o.optString("title").take(1000),o.optString("note").take(100000))); count++ } }
            each("notes") { o -> val c=o.getString("chapterId"); val p=paragraph(o); val id=o.getString("id"); val time=o.optLong("updatedAt").coerceAtLeast(0); if(id.isNotBlank() && valid(c,p) && o.optString("text").isNotBlank() && (dao.noteById(id)?.updatedAt ?: -1) <= time) { dao.note(Note(id,c,p,o.getString("text").take(100000),time)); count++ } }
            each("history") { o -> val c=o.getString("chapterId"); val p=paragraph(o); val date=o.optString("date"); val id=o.getString("id"); if(id.isNotBlank() && p!=null && valid(c,p) && date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) && o.optInt("seconds")>0) { val dailyId = "daily:$date:$c"
                    val existing = dao.historyById(dailyId)
                    val incoming = o.getInt("seconds").coerceAtMost(86400)
                    dao.history(ReadingHistory(dailyId,date,c,if ((existing?.seconds ?: 0) > incoming) existing!!.paragraphId else p,maxOf(existing?.seconds ?: 0,incoming))); count++ } }
        }
        json.optJSONObject("settings")?.let { o ->
            val old=settings.settings.first()
            settings.update(old.copy(theme=o.optString("theme",old.theme), russianSize=o.optDouble("russianSize",old.russianSize.toDouble()).toFloat(), arabicSize=o.optDouble("arabicSize",old.arabicSize.toDouble()).toFloat(), lineHeight=o.optDouble("lineHeight",old.lineHeight.toDouble()).toFloat(), russianFont=o.optString("russianFont",old.russianFont), showArabic=o.optBoolean("showArabic",old.showArabic), showNotes=o.optBoolean("showNotes",old.showNotes), textWidth=o.optDouble("textWidth",old.textWidth.toDouble()).toFloat(), alignment=o.optString("alignment",old.alignment), arabicFont=o.optString("arabicFont",old.arabicFont), brightness=o.optDouble("brightness",old.brightness.toDouble()).toFloat(), reducedMotion=o.optBoolean("reducedMotion",old.reducedMotion)))
        }
        count
    }
}
