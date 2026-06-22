package com.writingapp.data.repository

import com.google.gson.Gson
import com.writingapp.data.local.datastore.SettingsDataStore
import com.writingapp.data.remote.AiApiService
import com.writingapp.data.remote.AuthInterceptor
import com.writingapp.data.remote.dto.ChatRequest
import com.writingapp.domain.model.AiConfig
import com.writingapp.domain.model.ChatMessage
import com.writingapp.domain.repository.AiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.BufferedReader
import java.io.InputStreamReader

class AiRepositoryImpl(
    private val apiService: AiApiService,
    private val settingsDataStore: SettingsDataStore,
    private val gson: Gson,
    private val authInterceptor: AuthInterceptor
) : AiRepository {

    override fun getAiConfig(): Flow<AiConfig> = settingsDataStore.aiConfigFlow

    override suspend fun saveAiConfig(config: AiConfig) {
        authInterceptor.apiKey = config.apiKey
        settingsDataStore.saveAiConfig(config)
    }

    override fun isDarkMode(): Flow<Boolean> = settingsDataStore.darkModeFlow

    override suspend fun setDarkMode(enabled: Boolean) {
        settingsDataStore.setDarkMode(enabled)
    }

    override suspend fun sendMessage(
        messages: List<ChatMessage>,
        context: String,
        config: AiConfig,
        onChunk: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        authInterceptor.apiKey = config.apiKey
        val chatMessages = buildChatMessages(config, context, messages)

        val request = ChatRequest(
            model = config.model,
            messages = chatMessages,
            stream = true,
            temperature = config.temperature,
            maxTokens = config.maxTokens,
            topP = config.topP
        )

        val url = "${config.baseUrl.trimEnd('/')}/chat/completions"
        val response = apiService.chatCompletionStream(url, request)
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string() ?: "Unknown error"
            throw Exception("API error ${response.code()}: $errorBody")
        }

        val body = response.body() ?: throw Exception("Empty response body")
        parseStreamingResponse(body, onChunk)
    }

    override suspend fun sendSelectionAction(
        selectedText: String,
        instruction: String,
        documentContext: String,
        config: AiConfig,
        onChunk: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        authInterceptor.apiKey = config.apiKey
        val messages = mutableListOf(
            ChatRequest.Message("system", buildSelectionSystemPrompt(config, documentContext)),
            ChatRequest.Message("user", buildSelectionUserPrompt(selectedText, instruction))
        )

        val request = ChatRequest(
            model = config.model,
            messages = messages,
            stream = true,
            temperature = config.temperature,
            maxTokens = config.maxTokens,
            topP = config.topP
        )

        val url = "${config.baseUrl.trimEnd('/')}/chat/completions"
        val response = apiService.chatCompletionStream(url, request)
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string() ?: "Unknown error"
            throw Exception("API error ${response.code()}: $errorBody")
        }

        val body = response.body() ?: throw Exception("Empty response body")
        parseStreamingResponse(body, onChunk)
    }

    override suspend fun requestGhostCompletion(
        textBeforeCursor: String,
        textAfterCursor: String,
        config: AiConfig,
        onChunk: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        authInterceptor.apiKey = config.apiKey
        val messages = listOf(
            ChatRequest.Message("system", "You are a writing copilot. Continue the text naturally. Return ONLY the continuation text, no explanations, no markdown."),
            ChatRequest.Message("user", "Continue from where I left off. Write the next few words or sentence.\n\n${textBeforeCursor.takeLast(500)}")
        )

        val request = ChatRequest(
            model = config.model,
            messages = messages,
            stream = true,
            temperature = 0.5,
            maxTokens = 50,
            topP = 0.9
        )

        val url = "${config.baseUrl.trimEnd('/')}/chat/completions"
        val response = apiService.chatCompletionStream(url, request)
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string() ?: "Unknown error"
            return@withContext ""
        }

        val body = response.body() ?: return@withContext ""
        parseStreamingResponse(body, onChunk)
    }

    private fun buildChatMessages(config: AiConfig, context: String, messages: List<ChatMessage>): List<ChatRequest.Message> {
        val chatMessages = mutableListOf<ChatRequest.Message>()
        val systemContent = if (context.isNotBlank()) {
            "${config.systemPrompt}\n\nContext:\n$context"
        } else {
            config.systemPrompt
        }
        chatMessages.add(ChatRequest.Message("system", systemContent))
        messages.forEach { msg ->
            chatMessages.add(ChatRequest.Message(msg.role, msg.content))
        }
        return chatMessages
    }

    private fun buildSelectionSystemPrompt(config: AiConfig, documentContext: String): String {
        val ctx = if (documentContext.isNotBlank()) "\n\nDocument context:\n$documentContext" else ""
        return "${config.systemPrompt}$ctx\n\nYou are helping the user edit a specific selection. Follow the instruction precisely. Return ONLY the modified text, no explanations."
    }

    private fun buildSelectionUserPrompt(selectedText: String, instruction: String): String {
        return """$instruction

Selected text:
```
$selectedText
```"""
    }

    private fun parseStreamingResponse(
        body: ResponseBody,
        onChunk: (String) -> Unit
    ): String {
        val reader = BufferedReader(InputStreamReader(body.byteStream()))
        val result = StringBuilder()
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            val currentLine = line ?: continue
            if (currentLine.startsWith("data: ")) {
                val data = currentLine.removePrefix("data: ")
                if (data == "[DONE]") break
                try {
                    val streamResponse = gson.fromJson(data, com.writingapp.data.remote.dto.ChatResponse::class.java)
                    val content = streamResponse.choices?.firstOrNull()?.delta?.content ?: ""
                    if (content.isNotEmpty()) {
                        result.append(content)
                        onChunk(content)
                    }
                } catch (_: Exception) { }
            }
        }
        reader.close()
        return result.toString()
    }
}
