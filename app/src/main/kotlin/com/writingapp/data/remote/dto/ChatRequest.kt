package com.writingapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<Message>,
    @SerializedName("stream") val stream: Boolean = true,
    @SerializedName("temperature") val temperature: Double = 0.7,
    @SerializedName("max_tokens") val maxTokens: Int = 4096,
    @SerializedName("top_p") val topP: Double = 0.9
) {
    data class Message(
        @SerializedName("role") val role: String,
        @SerializedName("content") val content: String
    )
}
