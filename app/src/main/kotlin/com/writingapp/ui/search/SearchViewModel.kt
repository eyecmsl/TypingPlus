package com.writingapp.ui.search

import androidx.lifecycle.ViewModel
import com.writingapp.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.writingapp.domain.model.Document

class SearchViewModel(
    private val documentRepository: DocumentRepository
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<Document>>(emptyList())
    val results: StateFlow<List<Document>> = _results.asStateFlow()

    fun search(query: String) {
        _query.value = query
        if (query.isBlank()) {
            _results.value = emptyList()
            return
        }
        viewModelScope.launch {
            documentRepository.getAllDocuments().collect { docs ->
                _results.value = docs.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.content.contains(query, ignoreCase = true)
                }
            }
        }
    }
}
