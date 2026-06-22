package com.writingapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.writingapp.domain.model.Document
import com.writingapp.domain.repository.DocumentRepository
import com.writingapp.domain.usecase.DeleteDocumentUseCase
import com.writingapp.domain.usecase.SaveDocumentUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val documentRepository: DocumentRepository,
    private val saveDocumentUseCase: SaveDocumentUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase
) : ViewModel() {

    private val _pinnedDocuments: StateFlow<List<Document>> = documentRepository.getPinnedDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documents: StateFlow<List<Document>> = documentRepository.getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createNewDocument(title: String = "Untitled") {
        viewModelScope.launch {
            val doc = Document(title = title, content = "")
            saveDocumentUseCase(doc)
        }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch {
            deleteDocumentUseCase(id)
        }
    }

    fun togglePin(document: Document) {
        viewModelScope.launch {
            documentRepository.setPinned(document.id, !document.isPinned)
        }
    }
}
