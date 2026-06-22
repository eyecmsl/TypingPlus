package com.writingapp.data.repository

import com.google.gson.Gson
import com.writingapp.data.local.datastore.SettingsDataStore
import com.writingapp.data.remote.AiApiService
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
    private val gson: Gson
) : AiRepository {
    override fun getAiConfig(): Flow<AiConfig> = settingsDataStore.aiConfigFlow

    override suspend fun saveAiConfig(config: AiConfig) {
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
        val chatMessages = mutableListOf<ChatRequest.Message>()

        if (context.isNotBlank()) {
            chatMessages.add(ChatRequest.Message("system", "$config.systemPrompt\n\nContext:\n$context"))
        } else {
            chatMessages.add(ChatRequest.Message("system", config.systemPrompt))
        }

        messages.forEach { msg ->
            chatMessages.add(ChatRequest.Message(msg.role, msg.content))
        }

        val request = ChatRequest(
            model = config.model,
            messages = chatMessages,
            stream = true,
            temperature = 0.7
        )

        val response = apiService.chatCompletionStream(request)
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string() ?: "Unknown error"
            throw Exception("API error ${response.code()}: $errorBody")
        }

        val body = response.body() ?: throw Exception("Empty response body")
        val fullContent = parseStreamingResponse(body, onChunk)
        fullContent
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
