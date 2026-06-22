package com.writingapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.writingapp.ui.dashboard.DashboardScreen
import com.writingapp.ui.search.SearchScreen
import com.writingapp.ui.settings.SettingsScreen

@Composable
fun MainScreen(
    onNavigateToEditor: (Long) -> Unit,
    onNavigateToRulebook: () -> Unit,
    onNavigateToWordline: () -> Unit,
    onNavigateToAssistant: (Long) -> Unit,
    rootNavController: NavHostController
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavItem.entries.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Documents.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(BottomNavItem.Documents.route) {
                DashboardScreen(
                    onDocumentClick = onNavigateToEditor,
                    onNewDocument = onNavigateToEditor,
                    onSearchClick = {
                        navController.navigate(BottomNavItem.Search.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onRulebookClick = onNavigateToRulebook,
                    onWordlineClick = onNavigateToWordline,
                    onSettingsClick = {
                        navController.navigate(BottomNavItem.Settings.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(BottomNavItem.Search.route) {
                SearchScreen(
                    onBack = {
                        navController.navigate(BottomNavItem.Documents.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onDocumentClick = onNavigateToEditor
                )
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(
                    onBack = {
                        navController.navigate(BottomNavItem.Documents.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}
