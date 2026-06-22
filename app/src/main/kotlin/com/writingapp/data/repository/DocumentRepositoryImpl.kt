package com.writingapp.data.repository

import com.writingapp.data.local.db.DocumentDao
import com.writingapp.data.local.db.entities.DocumentEntity
import com.writingapp.domain.model.Document
import com.writingapp.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DocumentRepositoryImpl(private val dao: DocumentDao) : DocumentRepository {
    override fun getAllDocuments(): Flow<List<Document>> =
        dao.getAllDocuments().map { entities -> entities.map { it.toDomain() } }

    override fun getDocumentById(id: Long): Flow<Document?> =
        dao.getDocumentById(id).map { it?.toDomain() }

    override fun getPinnedDocuments(): Flow<List<Document>> =
        dao.getPinnedDocuments().map { entities -> entities.map { it.toDomain() } }

    override fun getDocumentsByTag(tag: String): Flow<List<Document>> =
        dao.getDocumentsByTag(tag).map { entities -> entities.map { it.toDomain() } }

    override fun getAllTags(): Flow<List<String>> =
        dao.getAllTags().map { list -> list.flatMap { it.split(",").filter { tag -> tag.isNotBlank() } }.distinct() }

    override fun search(query: String): Flow<List<Document>> =
        dao.search(query).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getDailyNote(date: String): Document? =
        dao.getDailyNote(date)?.toDomain()

    override fun getAllDailyNotes(): Flow<List<Document>> =
        dao.getAllDailyNotes().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveDocument(document: Document): Long {
        return dao.insert(document.toEntity())
    }

    override suspend fun deleteDocument(id: Long) {
        dao.delete(DocumentEntity(id = id, title = "", content = ""))
    }

    override suspend fun updateContent(id: Long, content: String, wordCount: Int, charCount: Int, paragraphCount: Int, sentenceCount: Int) {
        dao.updateContent(id, content, wordCount, charCount, paragraphCount, sentenceCount)
    }

    override suspend fun setPinned(id: Long, pinned: Boolean) {
        dao.setPinned(id, pinned)
    }

    override suspend fun setTags(id: Long, tags: List<String>) {
        dao.setTags(id, tags.joinToString(","))
    }

    private fun DocumentEntity.toDomain() = Document(
        id = id, title = title, content = content,
        createdAt = createdAt, updatedAt = updatedAt,
        wordCount = wordCount, charCount = charCount,
        paragraphCount = paragraphCount, sentenceCount = sentenceCount,
        isPinned = isPinned, tags = tags.split(",").filter { it.isNotBlank() },
        isDailyNote = isDailyNote, dailyNoteDate = dailyNoteDate
    )

    private fun Document.toEntity() = DocumentEntity(
        id = id, title = title, content = content,
        createdAt = createdAt, updatedAt = updatedAt,
        wordCount = wordCount, charCount = charCount,
        paragraphCount = paragraphCount, sentenceCount = sentenceCount,
        isPinned = isPinned, tags = tags.joinToString(","),
        isDailyNote = isDailyNote, dailyNoteDate = dailyNoteDate
    )
}
