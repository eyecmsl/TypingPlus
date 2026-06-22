package com.writingapp.domain.usecase

import com.writingapp.domain.model.Document
import com.writingapp.domain.repository.DocumentRepository

class SaveDocumentUseCase(private val repository: DocumentRepository) {
    suspend operator fun invoke(document: Document): Long {
        val wordCount = document.content.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
        return repository.saveDocument(document.copy(wordCount = wordCount))
    }
}
