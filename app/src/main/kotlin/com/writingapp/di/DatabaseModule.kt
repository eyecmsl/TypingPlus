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

        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "writing_app.db"
        ).addMigrations(migration1to2)
            .fallbackToDestructiveMigration()
            .build()
    }

    single { get<AppDatabase>().documentDao() }
    single { get<AppDatabase>().documentVersionDao() }
    single { get<AppDatabase>().rulebookDao() }
    single { get<AppDatabase>().wordlineDao() }

    single { SettingsDataStore(androidContext()) }
}
