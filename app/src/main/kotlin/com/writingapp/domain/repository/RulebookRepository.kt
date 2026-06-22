package com.writingapp.domain.repository

import com.writingapp.domain.model.Rulebook
import kotlinx.coroutines.flow.Flow

interface RulebookRepository {
    fun getAllRules(): Flow<List<Rulebook>>
    fun getRulesByCategory(category: String): Flow<List<Rulebook>>
    suspend fun saveRule(rulebook: Rulebook): Long
    suspend fun deleteRule(id: Long)
}
