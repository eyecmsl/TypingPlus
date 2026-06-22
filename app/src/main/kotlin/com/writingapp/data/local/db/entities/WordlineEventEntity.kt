package com.writingapp.data.local.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wordline_events")
data class WordlineEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val chapter: Int = 0,
    val orderIndex: Int = 0,
    val characters: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
