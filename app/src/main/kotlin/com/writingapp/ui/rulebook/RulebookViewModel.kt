package com.writingapp.ui.rulebook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.writingapp.domain.model.Rulebook
import com.writingapp.domain.repository.RulebookRepository
import com.writingapp.domain.usecase.SaveRulebookUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RulebookViewModel(
    private val rulebookRepository: RulebookRepository,
    private val saveRulebookUseCase: SaveRulebookUseCase
) : ViewModel() {

    val rules: StateFlow<List<Rulebook>> = rulebookRepository.getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val filteredRules: StateFlow<List<Rulebook>> = combine(
        rules,
        _selectedCategory
    ) { allRules, category ->
        if (category == "all") allRules
        else allRules.filter { it.category == category }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = rules.map { list ->
        list.map { it.category }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun createRule(title: String, content: String, category: String) {
        viewModelScope.launch {
            saveRulebookUseCase(Rulebook(title = title, content = content, category = category))
        }
    }

    fun deleteRule(id: Long) {
        viewModelScope.launch {
            rulebookRepository.deleteRule(id)
        }
    }
}
