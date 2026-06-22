package com.writingapp.domain.usecase

import com.writingapp.domain.repository.DocumentRepository

class DeleteDocumentUseCase(private val repository: DocumentRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteDocument(id)
}
