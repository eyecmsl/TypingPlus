package com.writingapp.domain.usecase

import com.writingapp.domain.model.WordlineEvent
import com.writingapp.domain.repository.WordlineRepository

class SaveWordlineEventUseCase(private val repository: WordlineRepository) {
    suspend operator fun invoke(event: WordlineEvent): Long = repository.saveEvent(event)
}
