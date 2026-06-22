package com.writingapp.domain.model

data class AiConfig(
    val baseUrl: String = "http://localhost:8080/v1",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val systemPrompt: String = "You are a creative writing assistant. Help the user write, edit, and improve their text."
)
