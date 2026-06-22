package com.writingapp.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.writingapp.domain.model.AiConfig
import com.writingapp.domain.model.ChatMessage
import com.writingapp.domain.repository.AiRepository
import com.writingapp.domain.repository.DocumentRepository
import com.writingapp.domain.usecase.SendAiMessageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AssistantViewModel(
    private val aiRepository: AiRepository,
    private val documentRepository: DocumentRepository,
    private val sendAiMessageUseCase: SendAiMessageUseCase
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val aiConfig: StateFlow<AiConfig> = aiRepository.getAiConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiConfig())

    fun loadDocumentContext(documentId: Long) {
        viewModelScope.launch {
            documentRepository.getDocumentById(documentId).first()?.let { doc ->
                _messages.value = listOf(
                    ChatMessage(
                        role = "system",
                        content = "Current document: ${doc.title}\n\n${doc.content.take(2000)}"
                    )
                )
            }
        }
    }

    fun sendMessage(userMessage: String) {
        viewModelScope.launch {
            val userMsg = ChatMessage(role = "user", content = userMessage)
            _messages.value = _messages.value + userMsg
            _isLoading.value = true

            val config = aiConfig.value
            val contextMessages = _messages.value.filter { it.role == "system" }
            val context = contextMessages.joinToString("\n") { it.content }

            val assistantMsg = ChatMessage(role = "assistant", content = "")
            _messages.value = _messages.value + assistantMsg

            val mutableMessages = _messages.value.toMutableList()
            val assistantIndex = mutableMessages.size - 1

            try {
                sendAiMessageUseCase(
                    messages = _messages.value.filter { it.role != "system" },
                    context = context,
                    config = config
                ) { chunk ->
                    val current = mutableMessages[assistantIndex]
                    mutableMessages[assistantIndex] = current.copy(
                        content = current.content + chunk
                    )
                    _messages.value = mutableMessages.toList()
                }
            } catch (e: Exception) {
                val failed = mutableMessages[assistantIndex]
                mutableMessages[assistantIndex] = failed.copy(
                    content = "Error: ${e.message}"
                )
                _messages.value = mutableMessages.toList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }
}
