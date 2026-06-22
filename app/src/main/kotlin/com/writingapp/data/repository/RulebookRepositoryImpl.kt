package com.writingapp.data.repository

import com.writingapp.data.local.db.RulebookDao
import com.writingapp.data.local.db.entities.RulebookEntity
import com.writingapp.domain.model.Rulebook
import com.writingapp.domain.repository.RulebookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RulebookRepositoryImpl(private val dao: RulebookDao) : RulebookRepository {
    override fun getAllRules(): Flow<List<Rulebook>> =
        dao.getAllRules().map { it.map { e -> e.toDomain() } }

    override fun getRulesByCategory(category: String): Flow<List<Rulebook>> =
        dao.getRulesByCategory(category).map { it.map { e -> e.toDomain() } }

    override suspend fun saveRule(rulebook: Rulebook): Long =
        dao.insert(rulebook.toEntity())

    override suspend fun deleteRule(id: Long) {
        dao.delete(RulebookEntity(id = id, title = "", content = ""))
    }

    private fun RulebookEntity.toDomain() = Rulebook(
        id = id, title = title, content = content,
        category = category, createdAt = createdAt, updatedAt = updatedAt
    )

    private fun Rulebook.toEntity() = RulebookEntity(
        id = id, title = title, content = content,
        category = category, createdAt = createdAt, updatedAt = updatedAt
    )
}
