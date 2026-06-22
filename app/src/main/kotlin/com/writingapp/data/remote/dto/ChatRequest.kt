package com.writingapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<Message>,
    @SerializedName("stream") val stream: Boolean = true,
    @SerializedName("temperature") val temperature: Double = 0.7
) {
    data class Message(
        @SerializedName("role") val role: String,
        @SerializedName("content") val content: String
    )
}
