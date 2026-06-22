package com.writingapp.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Editor : Screen("editor/{documentId}") {
        fun createRoute(documentId: Long) = "editor/$documentId"
    }
    data object Assistant : Screen("assistant/{documentId}") {
        fun createRoute(documentId: Long) = "assistant/$documentId"
    }
    data object Rulebook : Screen("rulebook")
    data object Wordline : Screen("wordline")
    data object Settings : Screen("settings")
    data object Search : Screen("search")
}
