package com.writingapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    Documents("documents", Icons.Filled.Edit, "Documents"),
    Search("search", Icons.Filled.Search, "Search"),
    Settings("settings", Icons.Filled.Settings, "Settings")
}
