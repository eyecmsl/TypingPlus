package com.writingapp.domain.model

data class Document(
    val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val wordCount: Int = 0,
    val charCount: Int = 0,
    val paragraphCount: Int = 0,
    val sentenceCount: Int = 0,
    val isPinned: Boolean = false,
    val tags: List<String> = emptyList(),
    val isDailyNote: Boolean = false,
    val dailyNoteDate: String = ""
)
