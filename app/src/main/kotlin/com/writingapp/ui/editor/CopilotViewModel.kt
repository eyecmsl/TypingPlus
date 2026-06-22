package com.writingapp.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.writingapp.domain.model.AiConfig
import com.writingapp.domain.model.ChatMessage
import com.writingapp.domain.repository.AiRepository
import com.writingapp.domain.repository.DocumentRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SuggestionState(
    val originalText: String,
    val modifiedText: String,
    val instruction: String,
    val isStreaming: Boolean = true
)

class CopilotViewModel(
    private val aiRepository: AiRepository,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _suggestion = MutableStateFlow<SuggestionState?>(null)
    val suggestion: StateFlow<SuggestionState?> = _suggestion.asStateFlow()

    private val _ghostText = MutableStateFlow<String?>(null)
    val ghostText: StateFlow<String?> = _ghostText.asStateFlow()

    private val _isCopilotOpen = MutableStateFlow(false)
    val isCopilotOpen: StateFlow<Boolean> = _isCopilotOpen.asStateFlow()

    val aiConfig: StateFlow<AiConfig> = aiRepository.getAiConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiConfig())

    var documentContent: String = ""
        private set
    var documentTitle: String = ""
        private set

    private var streamingJob: Job? = null
    private var ghostTextJob: Job? = null

    fun updateDocumentContext(content: String) {
        documentContent = content
    }

    fun toggleCopilot() {
        _isCopilotOpen.value = !_isCopilotOpen.value
        if (!_isCopilotOpen.value) {
            _suggestion.value = null
            _ghostText.value = null
        }
    }

    fun loadDocument(documentId: Long) {
        viewModelScope.launch {
            documentRepository.getDocumentById(documentId).first()?.let { doc ->
                documentTitle = doc.title
                documentContent = doc.content
                _messages.value = listOf(
                    ChatMessage(
                        role = "system",
                        content = "Current document: ${doc.title}\n\n${doc.content.take(3000)}"
                    )
                )
            }
        }
    }

    fun sendChatMessage(text: String, fullEditorContent: String) {
        streamingJob?.cancel()
        documentContent = fullEditorContent
        streamingJob = viewModelScope.launch {
            val userMsg = ChatMessage(role = "user", content = text)
            _messages.value = _messages.value + userMsg
            _isLoading.value = true

            val config = aiConfig.value
            val systemMessages = _messages.value.filter { it.role == "system" }
            val context = systemMessages.joinToString("\n") { it.content }

            val assistantMsg = ChatMessage(role = "assistant", content = "")
            _messages.value = _messages.value + assistantMsg

            val mutableMessages = _messages.value.toMutableList()
            val assistantIndex = mutableMessages.size - 1

            try {
                aiRepository.sendMessage(
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

    fun requestSelectionAction(instruction: String, selectedText: String, fullContent: String) {
        streamingJob?.cancel()
        _suggestion.value = SuggestionState(
            originalText = selectedText,
            modifiedText = "",
            instruction = instruction,
            isStreaming = true
        )

        documentContent = fullContent
        streamingJob = viewModelScope.launch {
            val config = aiConfig.value
            val result = StringBuilder()
            try {
                aiRepository.sendSelectionAction(
                    selectedText = selectedText,
                    instruction = instruction,
                    documentContext = fullContent,
                    config = config
                ) { chunk ->
                    result.append(chunk)
                    _suggestion.value = _suggestion.value?.copy(
                        modifiedText = result.toString()
                    )
                }
                _suggestion.value = _suggestion.value?.copy(isStreaming = false)
            } catch (e: Exception) {
                _suggestion.value = _suggestion.value?.copy(
                    modifiedText = "Error: ${e.message}",
                    isStreaming = false
                )
            }
        }
    }

    fun acceptSuggestion(): Pair<String, String>? {
        val s = _suggestion.value ?: return null
        val newContent = documentContent.replace(s.originalText, s.modifiedText)
        _suggestion.value = null
        return Pair(newContent, s.modifiedText)
    }

    fun rejectSuggestion() {
        _suggestion.value = null
    }

    fun requestGhostText(textBeforeCursor: String, textAfterCursor: String) {
        if (!aiConfig.value.ghostTextEnabled) return
        ghostTextJob?.cancel()
        ghostTextJob = viewModelScope.launch {
            delay(600)
            val config = aiConfig.value
            if (!config.ghostTextEnabled) return@launch
            val result = StringBuilder()
            try {
                aiRepository.requestGhostCompletion(
                    textBeforeCursor = textBeforeCursor,
                    textAfterCursor = textAfterCursor,
                    config = config
                ) { chunk ->
                    result.append(chunk)
                    _ghostText.value = result.toString()
                }
            } catch (_: Exception) {
                _ghostText.value = null
            }
        }
    }

    fun acceptGhostText(): String? {
        val g = _ghostText.value
        _ghostText.value = null
        return g
    }

    fun dismissGhostText() {
        _ghostText.value = null
        ghostTextJob?.cancel()
    }

    fun cancelStreaming() {
        streamingJob?.cancel()
        _isLoading.value = false
    }

    fun clearMessages() {
        _messages.value = _messages.value.filter { it.role == "system" }
    }
}
