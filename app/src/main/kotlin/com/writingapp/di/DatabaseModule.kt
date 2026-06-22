package com.writingapp.di

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.writingapp.data.local.db.AppDatabase
import com.writingapp.data.local.db.DocumentDao
import com.writingapp.data.local.db.DocumentVersionDao
import com.writingapp.data.local.db.RulebookDao
import com.writingapp.data.local.db.WordlineDao
import com.writingapp.data.local.datastore.SettingsDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        val migration1to2 = Migration(1, 2) { db ->
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS document_versions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    documentId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    content TEXT NOT NULL,
                    savedAt INTEGER NOT NULL,
                    versionNumber INTEGER NOT NULL
                )
            """.trimIndent())
        }

        val migration2to3 = Migration(2, 3) { db ->
            db.execSQL("ALTER TABLE documents ADD COLUMN charCount INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE documents ADD COLUMN paragraphCount INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE documents ADD COLUMN sentenceCount INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE documents ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE documents ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE documents ADD COLUMN isDailyNote INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE documents ADD COLUMN dailyNoteDate TEXT NOT NULL DEFAULT ''")
            db.execSQL("""
                CREATE VIRTUAL TABLE IF NOT EXISTS documents_fts USING fts4(
                    title, content,
                    content=documents,
                    tokenizer=unicode61
                )
            """.trimIndent())
        }

        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "writing_app.db"
        ).addMigrations(migration1to2, migration2to3)
            .fallbackToDestructiveMigration()
            .build()
    }

    single { get<AppDatabase>().documentDao() }
    single { get<AppDatabase>().documentVersionDao() }
    single { get<AppDatabase>().rulebookDao() }
    single { get<AppDatabase>().wordlineDao() }

    single { SettingsDataStore(androidContext()) }
}
