package com.writingapp.domain.model

data class WordlineEvent(
    val id: Long = 0,
    val title: String,
    val description: String,
    val chapter: Int = 0,
    val orderIndex: Int = 0,
    val characters: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
