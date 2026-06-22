package com.writingapp.data.repository

import com.writingapp.data.local.db.WordlineDao
import com.writingapp.data.local.db.entities.WordlineEventEntity
import com.writingapp.domain.model.WordlineEvent
import com.writingapp.domain.repository.WordlineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WordlineRepositoryImpl(private val dao: WordlineDao) : WordlineRepository {
    override fun getAllEvents(): Flow<List<WordlineEvent>> =
        dao.getAllEvents().map { it.map { e -> e.toDomain() } }

    override suspend fun saveEvent(event: WordlineEvent): Long =
        dao.insert(event.toEntity())

    override suspend fun deleteEvent(id: Long) {
        dao.delete(WordlineEventEntity(id = id, title = "", description = ""))
    }

    override suspend fun reorderEvents(events: List<WordlineEvent>) {
        events.forEachIndexed { index, event ->
            dao.updateOrder(event.id, index)
        }
    }

    private fun WordlineEventEntity.toDomain() = WordlineEvent(
        id = id, title = title, description = description,
        chapter = chapter, orderIndex = orderIndex, characters = characters, createdAt = createdAt
    )

    private fun WordlineEvent.toEntity() = WordlineEventEntity(
        id = id, title = title, description = description,
        chapter = chapter, orderIndex = orderIndex, characters = characters, createdAt = createdAt
    )
}
