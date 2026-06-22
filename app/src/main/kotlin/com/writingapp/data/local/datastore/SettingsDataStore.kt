package com.writingapp.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.writingapp.domain.model.AiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val BASE_URL = stringPreferencesKey("ai_base_url")
        val API_KEY = stringPreferencesKey("ai_api_key")
        val MODEL = stringPreferencesKey("ai_model")
        val SYSTEM_PROMPT = stringPreferencesKey("ai_system_prompt")
        val TEMPERATURE = doublePreferencesKey("ai_temperature")
        val MAX_TOKENS = intPreferencesKey("ai_max_tokens")
        val TOP_P = doublePreferencesKey("ai_top_p")
        val GHOST_TEXT_ENABLED = booleanPreferencesKey("ghost_text_enabled")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val aiConfigFlow: Flow<AiConfig> = context.dataStore.data.map { prefs ->
        AiConfig(
            baseUrl = prefs[Keys.BASE_URL] ?: "http://localhost:8080/v1",
            apiKey = prefs[Keys.API_KEY] ?: "",
            model = prefs[Keys.MODEL] ?: "gpt-4o-mini",
            systemPrompt = prefs[Keys.SYSTEM_PROMPT] ?: "You are a creative writing assistant.",
            temperature = prefs[Keys.TEMPERATURE] ?: 0.7,
            maxTokens = prefs[Keys.MAX_TOKENS] ?: 4096,
            topP = prefs[Keys.TOP_P] ?: 0.9,
            ghostTextEnabled = prefs[Keys.GHOST_TEXT_ENABLED] ?: false
        )
    }

    val darkModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DARK_MODE] ?: false
    }

    suspend fun saveAiConfig(config: AiConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BASE_URL] = config.baseUrl
            prefs[Keys.API_KEY] = config.apiKey
            prefs[Keys.MODEL] = config.model
            prefs[Keys.SYSTEM_PROMPT] = config.systemPrompt
            prefs[Keys.TEMPERATURE] = config.temperature
            prefs[Keys.MAX_TOKENS] = config.maxTokens
            prefs[Keys.TOP_P] = config.topP
            prefs[Keys.GHOST_TEXT_ENABLED] = config.ghostTextEnabled
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = enabled
        }
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = true
        }
    }
}
