package com.writingapp.domain.usecase

import com.writingapp.domain.model.AiConfig
import com.writingapp.domain.model.ChatMessage
import com.writingapp.domain.repository.AiRepository

class SendAiMessageUseCase(private val repository: AiRepository) {
    suspend operator fun invoke(
        messages: List<ChatMessage>,
        context: String,
        config: AiConfig,
        onChunk: (String) -> Unit
    ) = repository.sendMessage(messages, context, config, onChunk)
}
