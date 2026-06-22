package com.writingapp.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.InsertLink
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.LooksTwo
import androidx.compose.material.icons.filled.Looks3
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.writingapp.ui.components.MarkdownDisplay
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    documentId: Long,
    onBack: () -> Unit,
    onAiAssistant: () -> Unit,
    viewModel: EditorViewModel = koinViewModel()
) {
    var isPreview by remember { mutableStateOf(false) }
    val content by viewModel.content.collectAsState()
    val wordCount by viewModel.wordCount.collectAsState()
    val charCount by viewModel.charCount.collectAsState()

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = viewModel.title,
                        onValueChange = { viewModel.updateTitle(it) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onAiAssistant) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant")
                    }
                    IconButton(onClick = { isPreview = !isPreview }) {
                        Icon(
                            if (isPreview) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = if (isPreview) "Edit" else "Preview"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isPreview) {
                MarkdownDisplay(
                    markdown = content,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                )
            } else {
                EditorToolbar(
                    onFormatAction = { action ->
                        viewModel.formatAction(action)
                    }
                )

                Spacer(Modifier.height(4.dp))

                var textFieldValue by remember(content) {
                    mutableStateOf(TextFieldValue(content))
                }

                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        viewModel.updateContent(newValue.text)
                        viewModel.setTextFieldValue(newValue)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 28.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0f),
                        focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0f)
                    )
                )

                StatusBar(
                    wordCount = wordCount,
                    charCount = charCount
                )
            }
        }
    }
}

@Composable
private fun EditorToolbar(
    onFormatAction: (FormatAction) -> Unit
) {
    Surface(
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarButton(
                icon = Icons.Default.FormatBold,
                description = "Bold",
                onClick = { onFormatAction(FormatAction.Bold) }
            )
            ToolbarButton(
                icon = Icons.Default.FormatItalic,
                description = "Italic",
                onClick = { onFormatAction(FormatAction.Italic) }
            )
            ToolbarButton(
                icon = Icons.Default.FormatStrikethrough,
                description = "Strikethrough",
                onClick = { onFormatAction(FormatAction.Strikethrough) }
            )

            ToolbarSeparator()

            ToolbarButton(
                icon = Icons.Default.Title,
                description = "Heading 1",
                onClick = { onFormatAction(FormatAction.Heading1) }
            )
            ToolbarButton(
                icon = Icons.Default.LooksOne,
                description = "Heading 2",
                onClick = { onFormatAction(FormatAction.Heading2) }
            )
            ToolbarButton(
                icon = Icons.Default.LooksTwo,
                description = "Heading 3",
                onClick = { onFormatAction(FormatAction.Heading3) }
            )

            ToolbarSeparator()

            ToolbarButton(
                icon = Icons.Default.FormatListBulleted,
                description = "Bullet List",
                onClick = { onFormatAction(FormatAction.BulletList) }
            )
            ToolbarButton(
                icon = Icons.Default.FormatListNumbered,
                description = "Numbered List",
                onClick = { onFormatAction(FormatAction.NumberedList) }
            )

            ToolbarSeparator()

            ToolbarButton(
                icon =                 Icons.Filled.FormatQuote,
                description = "Blockquote",
                onClick = { onFormatAction(FormatAction.Blockquote) }
            )
            ToolbarButton(
                icon = Icons.Default.Code,
                description = "Code",
                onClick = { onFormatAction(FormatAction.Code) }
            )
            ToolbarButton(
                icon = Icons.Default.InsertLink,
                description = "Link",
                onClick = { onFormatAction(FormatAction.Link) }
            )
            ToolbarButton(
                icon = Icons.Default.HorizontalRule,
                description = "Horizontal Rule",
                onClick = { onFormatAction(FormatAction.HorizontalRule) }
            )

            ToolbarSeparator()

            ToolbarButton(
                icon = Icons.AutoMirrored.Filled.Undo,
                description = "Undo",
                onClick = { onFormatAction(FormatAction.Undo) }
            )
            ToolbarButton(
                icon = Icons.AutoMirrored.Filled.Redo,
                description = "Redo",
                onClick = { onFormatAction(FormatAction.Redo) }
            )
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.width(40.dp).height(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.width(20.dp).height(20.dp)
        )
    }
}

@Composable
private fun ToolbarSeparator() {
    HorizontalDivider(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun StatusBar(
    wordCount: Int,
    charCount: Int
) {
    Surface(
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$wordCount words",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = "$charCount characters",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
