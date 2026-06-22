package com.writingapp.ui.editor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

data class EditorTheme(
    val name: String,
    val fontFamily: FontFamily = FontFamily.Default,
    val fontSize: TextUnit = 16.sp,
    val lineHeight: TextUnit = 28.sp,
    val backgroundColor: Long = 0xFFFFFBFE,
    val textColor: Long = 0xFF1C1B1F,
    val isDark: Boolean = false
)

object EditorThemes {
    val defaultLight = EditorTheme(name = "Default Light", fontFamily = FontFamily.Default, fontSize = 16.sp, lineHeight = 28.sp)
    val serif = EditorTheme(name = "Serif", fontFamily = FontFamily.Serif, fontSize = 17.sp, lineHeight = 30.sp)
    val mono = EditorTheme(name = "Monospace", fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 24.sp)
    val sansSerif = EditorTheme(name = "Sans Serif", fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 28.sp)
    val compact = EditorTheme(name = "Compact", fontFamily = FontFamily.Default, fontSize = 14.sp, lineHeight = 22.sp)
    val relaxed = EditorTheme(name = "Relaxed", fontFamily = FontFamily.Default, fontSize = 18.sp, lineHeight = 34.sp)

    val all = listOf(defaultLight, serif, mono, sansSerif, compact, relaxed)
}
