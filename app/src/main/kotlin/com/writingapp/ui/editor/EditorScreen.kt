package com.writingapp.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.InsertLink
import androidx.compose.material.icons.filled.Looks3
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.LooksTwo
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.writingapp.ui.components.MarkdownDisplay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File
import android.os.Environment
import android.widget.Toast
import com.writingapp.ui.editor.MarkdownExport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    documentId: Long,
    onBack: () -> Unit,
    onAiAssistant: () -> Unit,
    viewModel: EditorViewModel = koinViewModel()
) {
    var isPreview by remember { mutableStateOf(false) }
    var showFileExplorer by remember { mutableStateOf(false) }
    var mdFiles by remember { mutableStateOf(listOf<File>()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    LaunchedEffect(showFileExplorer) {
        if (showFileExplorer) {
            mdFiles = scanMdFiles()
        }
    }

    val syntaxColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val headingColor = MaterialTheme.colorScheme.primary
    val linkColor = MaterialTheme.colorScheme.tertiary
    val codeColor = MaterialTheme.colorScheme.secondary
    val quoteColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    val syntaxTransformation = remember(syntaxColor, headingColor, linkColor, codeColor, quoteColor) {
        MarkdownVisualTransformation(
            syntaxColor = syntaxColor,
            headingColor = headingColor,
            linkColor = linkColor,
            codeColor = codeColor,
            quoteColor = quoteColor
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.8f)) {
                Text(
                    text = "File Explorer",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(
                            text = "${mdFiles.size} .md files found",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(mdFiles) { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { /* open file */ }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.InsertDriveFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column {
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = file.parent ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
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
                        Row {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            IconButton(onClick = {
                                scope.launch { drawerState.open() }
                            }) {
                                Icon(Icons.Outlined.FolderOpen, contentDescription = "File Explorer")
                            }
                        }
                    },
                    actions = {
                        var showExportMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Export")
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Export as HTML") },
                                onClick = {
                                    showExportMenu = false
                                    MarkdownExport.shareAsHtml(
                                        context,
                                        viewModel.title,
                                        viewModel.textFieldValue.text
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Export as PDF") },
                                onClick = {
                                    showExportMenu = false
                                    MarkdownExport.exportAsPdf(
                                        context,
                                        viewModel.title,
                                        viewModel.textFieldValue.text
                                    )
                                },
                                leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, contentDescription = null) }
                            )
                        }
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
                        markdown = renderWikilinks(viewModel.textFieldValue.text),
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

                    OutlinedTextField(
                        value = viewModel.textFieldValue,
                        onValueChange = { viewModel.updateTextFieldValue(it) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 28.sp
                        ),
                        visualTransformation = syntaxTransformation,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0f),
                            focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0f)
                        )
                    )

                    StatusBar(
                        wordCount = viewModel.wordCount,
                        charCount = viewModel.charCount
                    )
                }
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
                icon = Icons.Default.FormatQuote,
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

private fun scanMdFiles(rootDir: File = File("/")): List<File> {
    val result = mutableListOf<File>()
    val searchDirs = listOf(
        android.os.Environment.getExternalStorageDirectory(),
        android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
    )
    for (dir in searchDirs) {
        if (dir.exists()) {
            scanDir(dir, result)
        }
    }
    return result
}

private fun scanDir(dir: File, result: MutableList<File>, depth: Int = 0) {
    if (depth > 3) return
    val files = dir.listFiles() ?: return
    for (file in files) {
        if (file.isDirectory && !file.name.startsWith(".")) {
            scanDir(file, result, depth + 1)
        } else if (file.name.endsWith(".md", ignoreCase = true)) {
            result.add(file)
        }
    }
}
