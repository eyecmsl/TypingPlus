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

    override suspend fun saveDocument(document: Document): Long {
        return dao.insert(document.toEntity())
    }

    override suspend fun deleteDocument(id: Long) {
        dao.delete(DocumentEntity(id = id, title = "", content = ""))
    }

    override suspend fun updateContent(id: Long, content: String) {
        dao.updateContent(id, content)
    }

    private fun DocumentEntity.toDomain() = Document(
        id = id, title = title, content = content,
        createdAt = createdAt, updatedAt = updatedAt, wordCount = wordCount
    )

    private fun Document.toEntity() = DocumentEntity(
        id = id, title = title, content = content,
        createdAt = createdAt, updatedAt = updatedAt, wordCount = wordCount
    )
}
