package com.writingapp.domain.repository

import com.writingapp.domain.model.WordlineEvent
import kotlinx.coroutines.flow.Flow

interface WordlineRepository {
    fun getAllEvents(): Flow<List<WordlineEvent>>
    suspend fun saveEvent(event: WordlineEvent): Long
    suspend fun deleteEvent(id: Long)
    suspend fun reorderEvents(events: List<WordlineEvent>)
}
