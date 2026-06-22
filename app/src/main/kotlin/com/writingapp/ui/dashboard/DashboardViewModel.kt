package com.writingapp.ui.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.writingapp.domain.model.Document
import com.writingapp.domain.repository.DocumentRepository
import com.writingapp.domain.usecase.DeleteDocumentUseCase
import com.writingapp.domain.usecase.SaveDocumentUseCase
import com.writingapp.ui.editor.DocumentTemplate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder { UPDATED_AT, CREATED_AT, TITLE, WORD_COUNT }

class DashboardViewModel(
    private val documentRepository: DocumentRepository,
    private val saveDocumentUseCase: SaveDocumentUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase
) : ViewModel() {

    private val rawDocuments: StateFlow<List<Document>> = documentRepository.getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var isLoading by mutableStateOf(true)
        private set
    var sortOrder by mutableStateOf(SortOrder.UPDATED_AT)
    var showPinnedOnly by mutableStateOf(false)
    var showTemplates by mutableStateOf(false)

    val documents: StateFlow<List<Document>> = combine(
        rawDocuments,
        MutableStateFlow(Unit)
    ) { docs, _ ->
        val sorted = when (sortOrder) {
            SortOrder.UPDATED_AT -> docs.sortedByDescending { it.updatedAt }
            SortOrder.CREATED_AT -> docs.sortedByDescending { it.createdAt }
            SortOrder.TITLE -> docs.sortedBy { it.title.lowercase() }
            SortOrder.WORD_COUNT -> docs.sortedByDescending { it.wordCount }
        }
        val filtered = if (showPinnedOnly) sorted.filter { it.isPinned } else sorted
        filtered.sortedByDescending { it.isPinned }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            rawDocuments.collect { isLoading = false }
        }
    }

    fun createNewDocument(title: String = "Untitled") {
        viewModelScope.launch {
            val doc = Document(title = title, content = "")
            saveDocumentUseCase(doc)
            _snackbarEvent.emit("Created \"$title\"")
        }
    }

    fun createFromTemplate(template: DocumentTemplate) {
        viewModelScope.launch {
            val doc = Document(title = template.name, content = template.content)
            saveDocumentUseCase(doc)
            showTemplates = false
            _snackbarEvent.emit("Started \"${template.name}\" template")
        }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch {
            deleteDocumentUseCase(id)
            _snackbarEvent.emit("Document deleted")
        }
    }

    fun togglePin(document: Document) {
        viewModelScope.launch {
            documentRepository.setPinned(document.id, !document.isPinned)
            _snackbarEvent.emit(if (!document.isPinned) "Pinned document" else "Unpinned document")
        }
    }
}
