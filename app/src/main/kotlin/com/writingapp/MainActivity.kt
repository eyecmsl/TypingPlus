package com.writingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.writingapp.data.local.datastore.SettingsDataStore
import com.writingapp.ui.navigation.NavGraph
import com.writingapp.ui.onboarding.OnboardingScreen
import com.writingapp.ui.theme.TypingPlusTheme
import com.writingapp.domain.repository.AiRepository
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.get

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val aiRepository = get<AiRepository>(AiRepository::class.java)
            val isDarkMode by aiRepository.isDarkMode().collectAsState(initial = false)
            val settingsDataStore = remember { get<SettingsDataStore>(SettingsDataStore::class.java) }
            val onboardingCompleted by settingsDataStore.onboardingCompletedFlow.collectAsState(initial = null)
            val scope = rememberCoroutineScope()

            TypingPlusTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (onboardingCompleted) {
                        null -> {}
                        false -> OnboardingScreen(
                            onComplete = {
                                scope.launch {
                                    settingsDataStore.setOnboardingCompleted()
                                }
                            }
                        )
                        true -> {
                            val navController = rememberNavController()
                            NavGraph(navController = navController)
                        }
                    }
                }
            }
        }
    }
}
