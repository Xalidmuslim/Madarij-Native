package ru.madarij.nativeapp

import android.app.Application
import android.util.Log
import java.util.UUID
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.room.withTransaction
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import ru.madarij.nativeapp.data.*

class BookViewModel(app: Application) : AndroidViewModel(app) {
    private val db = Room.databaseBuilder(app, BookDatabase::class.java, "madarij.db").addMigrations(BookDatabase.MIGRATION_1_2).build()
    val repository = BookRepository(app, db)
    private val preferences = SettingsRepository(app)
    val backup = BackupRepository(app, db, preferences)
    val history = repository.dao.history().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val positions = repository.dao.positions().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val settings = preferences.settings.stateIn(viewModelScope, SharingStarted.Eagerly, ReadingSettings())
    val chapters = repository.dao.chapters().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val read = repository.dao.readChapters().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val last = repository.dao.lastPosition().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val bookmarks = repository.dao.bookmarks().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val notes = repository.dao.allNotes().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val status = MutableStateFlow("Подготовка данных…")
    init { viewModelScope.launch { status.value = try {
        if (repository.importCorpus()) {
            if (repository.importedDraft) "Том 1 · литературная сверка завершена; источники требуют отдельной проверки" else "Проверенный корпус загружен"
        } else "Текст пока не включён"
    } catch (error: Exception) { if (error is CancellationException) throw error; Log.e("BookImport", "Corpus import failed", error); "Не удалось загрузить текст. Перезапустите приложение." } } }
    fun recordReading(chapterId: String, paragraphId: String, seconds: Int) {
        if (seconds <= 0) return
        viewModelScope.launch {
            val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date())
            val id = "daily:$date:$chapterId"
            db.withTransaction {
                val previous = repository.dao.historyById(id)
                val total = ((previous?.seconds ?: 0).toLong() + seconds).coerceAtMost(86400).toInt()
                repository.dao.history(ReadingHistory(id, date, chapterId, paragraphId, total))
            }
        }
    }
    fun editNote(id: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { repository.dao.noteById(id)?.let { repository.dao.note(it.copy(text = text.trim(), updatedAt = System.currentTimeMillis())) } }
    }
    fun editBookmark(id: String, title: String, note: String) {
        viewModelScope.launch { repository.dao.allBookmarks().find { it.id == id }?.let { repository.dao.bookmark(it.copy(title = title.trim().ifBlank { it.title }, note = note)) } }
    }
    fun markUnread(id: String) { viewModelScope.launch { repository.dao.unread(id) } }
    fun addNote(chapterId: String, paragraphId: String?, text: String) {
        val content = text.trim()
        if (content.isBlank()) return
        viewModelScope.launch { repository.dao.note(Note(UUID.randomUUID().toString(), chapterId, paragraphId, content, System.currentTimeMillis())) }
    }
    fun deleteNote(id: String) { viewModelScope.launch { repository.dao.deleteNote(id) } }
    fun deleteBookmark(id: String) { viewModelScope.launch { repository.dao.deleteBookmark(id) } }
    fun bookmarkParagraph(chapterId: String, paragraphId: String?, title: String, note: String) {
        viewModelScope.launch { repository.dao.bookmark(Bookmark(if (paragraphId == null) "chapter:$chapterId" else "paragraph:$chapterId:$paragraphId", chapterId, paragraphId, title, note)) }
    }
    fun settings(value: ReadingSettings) { viewModelScope.launch { preferences.update(value) } }
    fun markRead(id: String) { viewModelScope.launch { repository.dao.read(ReadChapter(id, System.currentTimeMillis())) } }
    fun savePosition(chapter: String, paragraph: String, offset: Int) { viewModelScope.launch { repository.dao.position(Position(chapter, paragraph, offset.coerceAtLeast(0), System.currentTimeMillis())) } }
    fun bookmark(chapter: Chapter) { viewModelScope.launch { repository.dao.bookmark(Bookmark("chapter:${chapter.id}", chapter.id, null, chapter.title, "")) } }
}
