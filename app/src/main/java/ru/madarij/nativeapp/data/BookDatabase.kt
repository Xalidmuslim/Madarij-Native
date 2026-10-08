package ru.madarij.nativeapp.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chapters")
data class Chapter(@PrimaryKey val id: String, val volume: Int, val ordinal: Int, val title: String, val source: String)
@Entity(tableName = "paragraphs", indices = [Index("chapterId")])
data class Paragraph(@PrimaryKey val id: String, val chapterId: String, val ordinal: Int, val ar: String, val ru: String, val role: String, val source: String)
@Fts4
@Entity(tableName = "paragraph_search")
data class SearchIndex(val paragraphId: String, val text: String)
@Entity(tableName = "positions")
data class Position(@PrimaryKey val chapterId: String, val paragraphId: String, val offset: Int, val updatedAt: Long)
@Entity(tableName = "read_chapters")
data class ReadChapter(@PrimaryKey val chapterId: String, val readAt: Long)
@Entity(tableName = "bookmarks")
data class Bookmark(@PrimaryKey val id: String, val chapterId: String, val paragraphId: String?, val title: String, val note: String)
@Entity(tableName = "notes", indices = [Index("chapterId")])
data class Note(@PrimaryKey val id: String, val chapterId: String, val paragraphId: String?, val text: String, val updatedAt: Long)
@Entity(tableName = "content_version")
data class ContentVersion(@PrimaryKey val singleton: Int = 1, val sha256: String)

@Entity(tableName = "reading_history")
data class ReadingHistory(@PrimaryKey val id: String, val date: String, val chapterId: String, val paragraphId: String, val seconds: Int)

@Dao
interface BookDao {
    @Query("SELECT * FROM chapters") suspend fun allChapters(): List<Chapter>
    @Query("SELECT * FROM positions") suspend fun allPositions(): List<Position>
    @Query("SELECT * FROM positions") fun positions(): Flow<List<Position>>
    @Query("SELECT * FROM read_chapters") suspend fun allRead(): List<ReadChapter>
    @Query("SELECT * FROM bookmarks") suspend fun allBookmarks(): List<Bookmark>
    @Query("SELECT * FROM notes") suspend fun notesSnapshot(): List<Note>
    @Query("SELECT * FROM notes WHERE id = :id") suspend fun noteById(id: String): Note?
    @Query("SELECT * FROM reading_history ORDER BY date DESC") fun history(): Flow<List<ReadingHistory>>
    @Query("SELECT * FROM reading_history") suspend fun historySnapshot(): List<ReadingHistory>
    @Query("SELECT * FROM reading_history WHERE id = :id") suspend fun historyById(id: String): ReadingHistory?
    @Upsert suspend fun history(value: ReadingHistory)
    @Query("SELECT * FROM chapters ORDER BY ordinal") fun chapters(): Flow<List<Chapter>>
    @Query("SELECT * FROM paragraphs WHERE id = :id LIMIT 1") suspend fun paragraph(id: String): Paragraph?
    @Query("SELECT id FROM paragraphs WHERE chapterId = :chapterId ORDER BY ordinal LIMIT 1") suspend fun firstParagraphId(chapterId: String): String?
    @Query("SELECT * FROM paragraphs WHERE chapterId = :chapterId ORDER BY ordinal") suspend fun paragraphs(chapterId: String): List<Paragraph>
    @Query("SELECT * FROM positions ORDER BY updatedAt DESC LIMIT 1") fun lastPosition(): Flow<Position?>
    @Query("SELECT * FROM positions WHERE chapterId = :chapterId") suspend fun position(chapterId: String): Position?
    @Upsert suspend fun position(value: Position)
    @Upsert suspend fun read(value: ReadChapter)
    @Query("DELETE FROM read_chapters WHERE chapterId = :id") suspend fun unread(id: String)
    @Query("SELECT * FROM read_chapters") fun readChapters(): Flow<List<ReadChapter>>
    @Upsert suspend fun bookmark(value: Bookmark)
    @Query("SELECT * FROM bookmarks ORDER BY title") fun bookmarks(): Flow<List<Bookmark>>
    @Query("DELETE FROM bookmarks WHERE id = :id") suspend fun deleteBookmark(id: String)
    @Upsert suspend fun note(value: Note)
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC") fun allNotes(): Flow<List<Note>>
    @Query("DELETE FROM notes WHERE id = :id") suspend fun deleteNote(id: String)
    @Query("SELECT * FROM notes WHERE chapterId = :chapterId ORDER BY updatedAt DESC") fun notes(chapterId: String): Flow<List<Note>>
    @Query("SELECT paragraphs.* FROM paragraphs JOIN paragraph_search ON paragraphs.id = paragraph_search.paragraphId WHERE paragraph_search MATCH :query ORDER BY paragraphs.chapterId, paragraphs.ordinal") suspend fun search(query: String): List<Paragraph>
    @Insert suspend fun insertChapters(values: List<Chapter>)
    @Insert suspend fun insertParagraphs(values: List<Paragraph>)
    @Insert suspend fun insertIndex(values: List<SearchIndex>)
    @Query("DELETE FROM chapters") suspend fun clearChapters()
    @Query("DELETE FROM paragraphs") suspend fun clearParagraphs()
    @Query("DELETE FROM paragraph_search") suspend fun clearIndex()
    @Query("SELECT * FROM content_version WHERE singleton = 1") suspend fun contentVersion(): ContentVersion?
    @Upsert suspend fun contentVersion(value: ContentVersion)
}

@Database(entities = [Chapter::class, Paragraph::class, SearchIndex::class, Position::class, ReadChapter::class, Bookmark::class, Note::class, ContentVersion::class, ReadingHistory::class], version = 2, exportSchema = true)
abstract class BookDatabase : RoomDatabase() {
    abstract fun dao(): BookDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS reading_history (id TEXT NOT NULL PRIMARY KEY, date TEXT NOT NULL, chapterId TEXT NOT NULL, paragraphId TEXT NOT NULL, seconds INTEGER NOT NULL)")
            }
        }
    }
}
