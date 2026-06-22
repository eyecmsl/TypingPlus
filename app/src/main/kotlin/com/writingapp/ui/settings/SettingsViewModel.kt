package com.writingapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.writingapp.domain.model.AiConfig
import com.writingapp.domain.repository.AiRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val aiRepository: AiRepository
) : ViewModel() {

    val aiConfig: StateFlow<AiConfig> = aiRepository.getAiConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiConfig())

    val isDarkMode: StateFlow<Boolean> = aiRepository.isDarkMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun saveConfig(config: AiConfig) {
        viewModelScope.launch {
            aiRepository.saveAiConfig(config)
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            aiRepository.setDarkMode(enabled)
        }
    }
}
