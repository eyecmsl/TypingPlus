package com.writingapp.data.local.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "document_versions")
data class DocumentVersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val title: String,
    val content: String,
    val savedAt: Long = System.currentTimeMillis(),
    val versionNumber: Int
)
