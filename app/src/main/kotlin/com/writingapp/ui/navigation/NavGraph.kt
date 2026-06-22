package com.writingapp.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.writingapp.ui.assistant.AssistantScreen
import com.writingapp.ui.dashboard.DashboardScreen
import com.writingapp.ui.editor.EditorScreen
import com.writingapp.ui.rulebook.RulebookScreen
import com.writingapp.ui.search.SearchScreen
import com.writingapp.ui.settings.SettingsScreen
import com.writingapp.ui.wordline.WordlineScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        enterTransition = { enterFromRight },
        exitTransition = { exitToLeft },
        popEnterTransition = { enterFromLeft },
        popExitTransition = { exitToRight }
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onDocumentClick = { docId ->
                    navController.navigate(Screen.Editor.createRoute(docId))
                },
                onNewDocument = { docId ->
                    navController.navigate(Screen.Editor.createRoute(docId))
                },
                onSearchClick = {
                    navController.navigate(Screen.Search.route)
                },
                onRulebookClick = { navController.navigate(Screen.Rulebook.route) },
                onWordlineClick = { navController.navigate(Screen.Wordline.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(navArgument("documentId") { type = NavType.LongType }),
            enterTransition = { scaleIn },
            exitTransition = { scaleOut }
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getLong("documentId") ?: 0L
            EditorScreen(
                documentId = documentId,
                onBack = { navController.popBackStack() },
                onAiAssistant = {
                    navController.navigate(Screen.Assistant.createRoute(documentId))
                }
            )
        }

        composable(
            route = Screen.Assistant.route,
            arguments = listOf(navArgument("documentId") { type = NavType.LongType }),
            enterTransition = { enterFromBottom },
            exitTransition = { exitToBottom }
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getLong("documentId") ?: 0L
            AssistantScreen(
                documentId = documentId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Rulebook.route) {
            RulebookScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Wordline.route) {
            WordlineScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onDocumentClick = { docId ->
                    navController.navigate(Screen.Editor.createRoute(docId))
                }
            )
        }

        composable(
            Screen.Settings.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
