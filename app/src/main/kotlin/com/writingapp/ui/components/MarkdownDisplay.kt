package com.writingapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.m3.Markdown

@Composable
fun MarkdownDisplay(
    markdown: String,
    modifier: Modifier = Modifier
) {
    Markdown(
        content = markdown,
        modifier = modifier
    )
}
