package com.writingapp.data.local.db

import androidx.room.*
import com.writingapp.data.local.db.entities.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    fun getDocumentById(id: Long): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE isPinned = 1 ORDER BY updatedAt DESC")
    fun getPinnedDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE tags LIKE '%' || :tag || '%' ORDER BY updatedAt DESC")
    fun getDocumentsByTag(tag: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isDailyNote = 1 AND dailyNoteDate = :date LIMIT 1")
    suspend fun getDailyNote(date: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE isDailyNote = 1 ORDER BY dailyNoteDate DESC")
    fun getAllDailyNotes(): Flow<List<DocumentEntity>>

    @Query("SELECT DISTINCT tags FROM documents WHERE tags != ''")
    fun getAllTags(): Flow<List<String>>

    @Query("SELECT * FROM documents WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun search(query: String): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: DocumentEntity): Long

    @Delete
    suspend fun delete(document: DocumentEntity)

    @Query("UPDATE documents SET content = :content, wordCount = :wordCount, charCount = :charCount, paragraphCount = :paragraphCount, sentenceCount = :sentenceCount, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateContent(id: Long, content: String, wordCount: Int, charCount: Int, paragraphCount: Int, sentenceCount: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("UPDATE documents SET tags = :tags WHERE id = :id")
    suspend fun setTags(id: Long, tags: String)
}
