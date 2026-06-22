package com.writingapp

import android.app.Application
import com.writingapp.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TypingPlusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TypingPlusApp)
            modules(appModules)
        }
    }
}
