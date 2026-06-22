package com.writingapp.domain.repository

import com.writingapp.domain.model.Document
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun getAllDocuments(): Flow<List<Document>>
    fun getDocumentById(id: Long): Flow<Document?>
    fun getPinnedDocuments(): Flow<List<Document>>
    fun getDocumentsByTag(tag: String): Flow<List<Document>>
    fun getAllTags(): Flow<List<String>>
    fun search(query: String): Flow<List<Document>>
    suspend fun getDailyNote(date: String): Document?
    fun getAllDailyNotes(): Flow<List<Document>>
    suspend fun saveDocument(document: Document): Long
    suspend fun deleteDocument(id: Long)
    suspend fun updateContent(id: Long, content: String, wordCount: Int = 0, charCount: Int = 0, paragraphCount: Int = 0, sentenceCount: Int = 0)
    suspend fun setPinned(id: Long, pinned: Boolean)
    suspend fun setTags(id: Long, tags: List<String>)
}
