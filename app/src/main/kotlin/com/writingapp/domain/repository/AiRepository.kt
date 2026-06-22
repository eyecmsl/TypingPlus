package com.writingapp.domain.repository

import com.writingapp.domain.model.AiConfig
import com.writingapp.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface AiRepository {
    suspend fun sendMessage(
        messages: List<ChatMessage>,
        context: String = "",
        config: AiConfig,
        onChunk: (String) -> Unit
    ): String
    fun getAiConfig(): Flow<AiConfig>
    suspend fun saveAiConfig(config: AiConfig)
    fun isDarkMode(): Flow<Boolean>
    suspend fun setDarkMode(enabled: Boolean)
}
