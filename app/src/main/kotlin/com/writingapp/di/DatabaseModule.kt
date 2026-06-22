package com.writingapp.di

import androidx.room.Room
import com.writingapp.data.local.db.AppDatabase
import com.writingapp.data.local.db.DocumentDao
import com.writingapp.data.local.db.RulebookDao
import com.writingapp.data.local.db.WordlineDao
import com.writingapp.data.local.datastore.SettingsDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "writing_app.db"
        ).build()
    }

    single { get<AppDatabase>().documentDao() }
    single { get<AppDatabase>().rulebookDao() }
    single { get<AppDatabase>().wordlineDao() }

    single { SettingsDataStore(androidContext()) }
}
