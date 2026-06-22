package com.writingapp.domain.repository

import com.writingapp.domain.model.Document
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun getAllDocuments(): Flow<List<Document>>
    fun getDocumentById(id: Long): Flow<Document?>
    suspend fun saveDocument(document: Document): Long
    suspend fun deleteDocument(id: Long)
    suspend fun updateContent(id: Long, content: String)
}
