package com.writingapp.ui.wordline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.writingapp.domain.model.WordlineEvent
import com.writingapp.domain.repository.WordlineRepository
import com.writingapp.domain.usecase.SaveWordlineEventUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WordlineViewModel(
    private val wordlineRepository: WordlineRepository,
    private val saveWordlineEventUseCase: SaveWordlineEventUseCase
) : ViewModel() {

    val events: StateFlow<List<WordlineEvent>> = wordlineRepository.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEvent(title: String, description: String, chapter: Int, characters: String) {
        viewModelScope.launch {
            val currentEvents = events.value
            saveWordlineEventUseCase(
                WordlineEvent(
                    title = title,
                    description = description,
                    chapter = chapter,
                    orderIndex = currentEvents.size,
                    characters = characters
                )
            )
        }
    }

    fun deleteEvent(id: Long) {
        viewModelScope.launch {
            wordlineRepository.deleteEvent(id)
        }
    }

    fun reorderEvents(reorderedEvents: List<WordlineEvent>) {
        viewModelScope.launch {
            wordlineRepository.reorderEvents(reorderedEvents)
        }
    }
}
