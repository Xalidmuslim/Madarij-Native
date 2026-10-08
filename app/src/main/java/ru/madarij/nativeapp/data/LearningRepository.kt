package ru.madarij.nativeapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.learningStore by preferencesDataStore(name = "learning")
data class StudyOption(val text: String, val correct: Boolean, val explanation: String)
data class StudyQuestion(val id: String, val level: String, val prompt: String, val sourceParagraphId: String, val options: List<StudyOption>, val sourceExcerpt: String, val chapterId: String = "")
data class StudyTerm(val ar: String, val transcript: String, val definition: String, val sourceParagraphId: String, val id: String = "", val extendedDefinition: String = "", val relatedTerms: List<String> = emptyList(), val sourceParagraphIds: List<String> = emptyList())
data class StudyExercise(val id: String, val chapterId: String, val sourceParagraphId: String, val prompt: String, val hint: String)
data class StudyAttempt(val count: Int = 0, val errors: Int = 0, val streak: Int = 0, val due: Long = 0, val lowConfidence: Boolean = false, val latestCorrect: Boolean = false, val updatedAt: Long = 0, val sourceParagraphId: String? = null)
data class ChapterReview(val due: Long, val updatedAt: Long)
class LearningRepository(private val context: Context) {
    private val key = stringPreferencesKey("attempts")
    private val historyKey = stringPreferencesKey("history")
    private val chapterReviewKey = stringPreferencesKey("chapter_reviews")
    private val bank by lazy { JSONObject(context.assets.open("study.json").bufferedReader().use { it.readText() }) }
    val questions: List<StudyQuestion> by lazy {
        val a = bank.getJSONArray("questions")
        (0 until a.length()).map { i -> val q=a.getJSONObject(i); val o=q.getJSONArray("options")
            StudyQuestion(q.getString("id"),q.getString("level"),q.getString("prompt"),q.getString("sourceParagraphId"),(0 until o.length()).map { j -> val v=o.getJSONObject(j); StudyOption(v.getString("text"),v.getBoolean("correct"),v.getString("explanation")) },q.getString("sourceExcerpt"),q.optString("chapterId")) }
    }
    private fun strings(a: JSONArray?): List<String> = if(a==null) emptyList() else (0 until a.length()).map {a.getString(it)}
    val terms: List<StudyTerm> by lazy { val a=bank.getJSONArray("terms"); (0 until a.length()).map { i -> val t=a.getJSONObject(i); StudyTerm(t.getString("ar"),t.getString("transcript"),t.getString("definition"),t.getString("sourceParagraphId"),t.optString("id"),t.optString("extendedDefinition"),strings(t.optJSONArray("relatedTerms")),strings(t.optJSONArray("sourceParagraphIds"))) } }
    val exercises: List<StudyExercise> by lazy { val a=bank.optJSONArray("exercises") ?: JSONArray(); (0 until a.length()).map { i -> val e=a.getJSONObject(i); StudyExercise(e.getString("id"),e.getString("chapterId"),e.getString("sourceParagraphId"),e.getString("prompt"),e.getString("hint")) } }
    suspend fun load() = withContext(Dispatchers.IO) { questions.size; terms.size; exercises.size }
    private fun parse(raw: String): Map<String, StudyAttempt> { val obj=runCatching { JSONObject(raw) }.getOrDefault(JSONObject()); return obj.keys().asSequence().associateWith { id -> val v=obj.getJSONObject(id); StudyAttempt(v.optInt("count"),v.optInt("errors"),v.optInt("streak"),v.optLong("due"),v.optBoolean("lowConfidence"),if(v.has("latestCorrect")) v.optBoolean("latestCorrect") else v.optInt("streak")>0 && !v.optBoolean("lowConfidence"),v.optLong("updatedAt"),v.optString("sourceParagraphId").takeIf{it.isNotBlank()}) } }
    val attempts = context.learningStore.data.map { parse(it[key] ?: "{}") }
    val chapterReviews = context.learningStore.data.map { prefs ->
        val raw=JSONObject(prefs[chapterReviewKey] ?: "{}")
        raw.keys().asSequence().mapNotNull { id ->
            val item=raw.optJSONObject(id) ?: return@mapNotNull null
            val due=item.optLong("due")
            if(due>0L) id to ChapterReview(due,item.optLong("updatedAt")) else null
        }.toMap()
    }
    suspend fun scheduleChapterReview(chapterId: String, days: Int = 3) {
        require(chapterId.isNotBlank())
        context.learningStore.edit { prefs ->
            val raw=JSONObject(prefs[chapterReviewKey] ?: "{}")
            val now=System.currentTimeMillis()
            val updatedAt=maxOf(now,(raw.optJSONObject(chapterId)?.optLong("updatedAt") ?: 0L)+1L)
            raw.put(chapterId,JSONObject().put("due",now+days.coerceIn(1,365)*86400000L).put("updatedAt",updatedAt))
            prefs[chapterReviewKey]=raw.toString()
        }
    }
    suspend fun clearChapterReview(chapterId: String) {
        require(chapterId.isNotBlank())
        context.learningStore.edit { prefs ->
            val raw=JSONObject(prefs[chapterReviewKey] ?: "{}")
            val updatedAt=maxOf(System.currentTimeMillis(),(raw.optJSONObject(chapterId)?.optLong("updatedAt") ?: 0L)+1L)
            // Keep the tombstone so an older backup cannot resurrect a completed review.
            raw.put(chapterId,JSONObject().put("due",0L).put("updatedAt",updatedAt))
            prefs[chapterReviewKey]=raw.toString()
        }
    }
    suspend fun record(id: String, correct: Boolean, lowConfidence: Boolean) {
        load()
        context.learningStore.edit { prefs ->
            val raw=JSONObject(prefs[key] ?: "{}"); val old=parse(raw.toString())[id] ?: StudyAttempt()
            val now=System.currentTimeMillis(); val source=questions.find{it.id==id}?.sourceParagraphId
            val streak=if(correct && !lowConfidence) old.streak+1 else 0
            val days=if(!correct || lowConfidence) 1 else when(streak) { 1 -> 3; 2 -> 7; else -> 14 }
            raw.put(id,JSONObject().put("count",old.count+1).put("errors",old.errors+if(correct) 0 else 1).put("streak",streak).put("due",now+days*86400000L).put("lowConfidence",lowConfidence).put("latestCorrect",correct).put("updatedAt",now).put("sourceParagraphId",source))
            prefs[key]=raw.toString()
            val history=JSONArray(prefs[historyKey] ?: "[]")
            history.put(JSONObject().put("id",UUID.randomUUID().toString()).put("questionId",id).put("correct",correct).put("lowConfidence",lowConfidence).put("updatedAt",now).put("sourceParagraphId",source))
            prefs[historyKey]=history.toString()
        }
    }
    suspend fun exportState(): JSONObject = withContext(Dispatchers.IO) { val prefs=context.learningStore.data.first(); JSONObject().put("schemaVersion",3).put("attempts",JSONObject(prefs[key] ?: "{}")).put("history",JSONArray(prefs[historyKey] ?: "[]")).put("chapterReviews",JSONObject(prefs[chapterReviewKey] ?: "{}")) }
    suspend fun importState(state: JSONObject) = withContext(Dispatchers.IO) {
        val incoming=state.optJSONObject("attempts") ?: JSONObject()
        context.learningStore.edit { prefs ->
            val current=JSONObject(prefs[key] ?: "{}")
            incoming.keys().forEach { id -> val v=incoming.optJSONObject(id); if(v!=null && (!current.has(id) || v.optLong("updatedAt")>current.getJSONObject(id).optLong("updatedAt"))) current.put(id,v) }
            prefs[key]=current.toString()
            val chapterReviews=JSONObject(prefs[chapterReviewKey] ?: "{}")
            val restoredReviews=state.optJSONObject("chapterReviews") ?: JSONObject()
            restoredReviews.keys().forEach { id ->
                val incomingReview=restoredReviews.optJSONObject(id)
                val localReview=chapterReviews.optJSONObject(id)
                if(id.isNotBlank() && incomingReview!=null && incomingReview.optLong("due")>=0L && (localReview==null || incomingReview.optLong("updatedAt")>localReview.optLong("updatedAt"))) chapterReviews.put(id,incomingReview)
            }
            prefs[chapterReviewKey]=chapterReviews.toString()
            val merged=linkedMapOf<String,JSONObject>()
            listOf(JSONArray(prefs[historyKey] ?: "[]"),state.optJSONArray("history") ?: JSONArray()).forEach { a -> for(i in 0 until a.length()) { val v=a.optJSONObject(i) ?: continue; val id=v.optString("id"); if(id.isNotBlank() && (merged[id]==null || v.optLong("updatedAt")>(merged[id]?.optLong("updatedAt") ?: 0L))) merged[id]=v } }
            val history=JSONArray(); merged.values.sortedBy{it.optLong("updatedAt")}.forEach {history.put(it)}; prefs[historyKey]=history.toString()
        }
    }
}
