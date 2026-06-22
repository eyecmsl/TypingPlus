package com.writingapp.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.writingapp.ui.editor.EditorScreen
import com.writingapp.ui.rulebook.RulebookScreen
import com.writingapp.ui.wordline.WordlineScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToEditor = { docId ->
                    navController.navigate(Screen.Editor.createRoute(docId))
                },
                onNavigateToRulebook = {
                    navController.navigate(Screen.Rulebook.route)
                },
                onNavigateToWordline = {
                    navController.navigate(Screen.Wordline.route)
                },
                rootNavController = navController
            )
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(navArgument("documentId") { type = NavType.LongType }),
            enterTransition = { scaleIn(tween(300)) + fadeIn(tween(300)) },
            exitTransition = { scaleOut(tween(300)) + fadeOut(tween(300)) }
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getLong("documentId") ?: 0L
            EditorScreen(
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
    }
}
