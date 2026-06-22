package com.writingapp.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.writingapp.data.local.db.entities.DocumentEntity
import com.writingapp.data.local.db.entities.RulebookEntity
import com.writingapp.data.local.db.entities.WordlineEventEntity

@Database(
    entities = [DocumentEntity::class, RulebookEntity::class, WordlineEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun rulebookDao(): RulebookDao
    abstract fun wordlineDao(): WordlineDao
}
