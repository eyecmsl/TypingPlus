package com.writingapp.ui.editor

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FindReplace
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
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.writingapp.ui.components.MarkdownDisplay
import com.writingapp.ui.components.PermissionHelper
import com.writingapp.ui.components.rememberPermissionLauncher
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    documentId: Long,
    onBack: () -> Unit,
    onAiAssistant: () -> Unit,
    viewModel: EditorViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isPreview by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    val safFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch { snackbarHostState.showSnackbar("Folder selected for file browsing") }
        }
    }

    val permissionLauncher = rememberPermissionLauncher(
        onGranted = {
            scope.launch { snackbarHostState.showSnackbar("Storage access granted") }
        },
        onDenied = {
            scope.launch { snackbarHostState.showSnackbar("Storage permission denied — file browsing limited") }
        }
    )

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
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
                                if (PermissionHelper.hasStoragePermission(context)) {
                                    safFilePicker.launch(null)
                                } else {
                                    permissionLauncher.launch(PermissionHelper.getRequiredPermissions())
                                }
                            }) {
                                Icon(Icons.Outlined.FolderOpen, contentDescription = "Open file browser")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.togglePin() }) {
                            Icon(
                                if (viewModel.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (viewModel.isPinned) "Unpin document" else "Pin document",
                                tint = if (viewModel.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showStats = !showStats }) {
                            Icon(Icons.Default.BarChart, contentDescription = "Show document statistics")
                        }
                        IconButton(onClick = { viewModel.toggleFindReplace() }) {
                            Icon(Icons.Default.FindReplace, contentDescription = "Find and replace")
                        }
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Export document")
                        }
                        DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Export as HTML") },
                                onClick = {
                                    showExportMenu = false
                                    MarkdownExport.shareAsHtml(context, viewModel.title, viewModel.textFieldValue.text)
                                    scope.launch { snackbarHostState.showSnackbar("Opening HTML share sheet") }
                                },
                                leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as PDF") },
                                onClick = {
                                    showExportMenu = false
                                    val result = MarkdownExport.exportAsPdf(context, viewModel.title, viewModel.textFieldValue.text)
                                    result.onSuccess { uri ->
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share PDF"))
                                    }.onFailure { e ->
                                        scope.launch { snackbarHostState.showSnackbar("PDF export failed: ${e.message}") }
                                    }
                                },
                                leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as .md file") },
                                onClick = {
                                    showExportMenu = false
                                    val result = MarkdownExport.exportToFile(context, viewModel.title, viewModel.textFieldValue.text)
                                    result.onSuccess { uri ->
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/markdown"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share .md file"))
                                    }.onFailure { e ->
                                        scope.launch { snackbarHostState.showSnackbar("Export failed: ${e.message}") }
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) }
                            )
                        }
                        IconButton(onClick = onAiAssistant) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI writing assistant")
                        }
                        IconButton(onClick = { isPreview = !isPreview }) {
                            Icon(
                                if (isPreview) Icons.Default.Edit else Icons.Default.Visibility,
                                contentDescription = if (isPreview) "Switch to editor" else "Switch to preview"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )

                AnimatedVisibility(visible = viewModel.showFindReplace) {
                    FindReplaceBar(viewModel = viewModel, snackbarHostState = snackbarHostState)
                }

                AnimatedVisibility(visible = showStats) {
                    StatsBar(stats = viewModel.stats)
                }
            }
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
                    modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
                )
            } else {
                if (!viewModel.isFocusMode) {
                    EditorToolbar(onFormatAction = { viewModel.formatAction(it) })
                    Spacer(Modifier.height(4.dp))
                }

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = viewModel.textFieldValue,
                        onValueChange = { viewModel.updateTextFieldValue(it) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = if (viewModel.isFocusMode) 0.dp else 16.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = if (viewModel.isFocusMode) 32.sp else 28.sp
                        ),
                        visualTransformation = syntaxTransformation,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0f),
                            focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0f)
                        )
                    )
                }

                if (!viewModel.isFocusMode) {
                    StatusBar(
                        wordCount = viewModel.stats.wordCount,
                        charCount = viewModel.stats.charCount
                    )
                }
            }
        }
    }
}

@Composable
private fun FindReplaceBar(viewModel: EditorViewModel, snackbarHostState: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    Surface(tonalElevation = 2.dp, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.findQuery,
                    onValueChange = { viewModel.performFind(it) },
                    placeholder = { Text("Find") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).height(48.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
                Text(
                    text = if (viewModel.findMatchCount > 0) "${viewModel.currentFindIndex + 1}/${viewModel.findMatchCount}" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = { viewModel.findPrevious() }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous match", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { viewModel.findNext() }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next match", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { viewModel.toggleFindReplace() }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close find and replace", modifier = Modifier.size(18.dp))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.replaceQuery,
                    onValueChange = { viewModel.replaceQuery = it },
                    placeholder = { Text("Replace with") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).height(48.dp),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                AssistChip(
                    onClick = {
                        viewModel.replaceCurrent(viewModel.replaceQuery)
                        scope.launch { snackbarHostState.showSnackbar("Replaced 1 occurrence") }
                    },
                    label = { Text("Replace", style = MaterialTheme.typography.labelSmall) }
                )
                AssistChip(
                    onClick = {
                        viewModel.replaceAll(viewModel.findQuery, viewModel.replaceQuery)
                        scope.launch { snackbarHostState.showSnackbar("Replaced all occurrences") }
                    },
                    label = { Text("All", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}

@Composable
private fun StatsBar(stats: DocumentStats) {
    Surface(tonalElevation = 1.dp, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("Words", "${stats.wordCount}")
            StatItem("Chars", "${stats.charCount}")
            StatItem("No Spaces", "${stats.charCountNoSpaces}")
            StatItem("Paragraphs", "${stats.paragraphCount}")
            StatItem("Sentences", "${stats.sentenceCount}")
            StatItem("Reading", "${stats.readingTimeMinutes} min")
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EditorToolbar(onFormatAction: (FormatAction) -> Unit) {
    Surface(tonalElevation = 1.dp, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarButton(Icons.Default.FormatBold, "Bold (Ctrl+B)") { onFormatAction(FormatAction.Bold) }
            ToolbarButton(Icons.Default.FormatItalic, "Italic (Ctrl+I)") { onFormatAction(FormatAction.Italic) }
            ToolbarButton(Icons.Default.FormatStrikethrough, "Strikethrough") { onFormatAction(FormatAction.Strikethrough) }
            ToolbarSeparator()
            ToolbarButton(Icons.Default.Title, "Heading 1") { onFormatAction(FormatAction.Heading1) }
            ToolbarButton(Icons.Default.LooksOne, "Heading 2") { onFormatAction(FormatAction.Heading2) }
            ToolbarButton(Icons.Default.LooksTwo, "Heading 3") { onFormatAction(FormatAction.Heading3) }
            ToolbarSeparator()
            ToolbarButton(Icons.AutoMirrored.Filled.FormatListBulleted, "Bullet List") { onFormatAction(FormatAction.BulletList) }
            ToolbarButton(Icons.Default.FormatListNumbered, "Numbered List") { onFormatAction(FormatAction.NumberedList) }
            ToolbarSeparator()
            ToolbarButton(Icons.Default.FormatQuote, "Blockquote") { onFormatAction(FormatAction.Blockquote) }
            ToolbarButton(Icons.Default.Code, "Code") { onFormatAction(FormatAction.Code) }
            ToolbarButton(Icons.Default.InsertLink, "Link") { onFormatAction(FormatAction.Link) }
            ToolbarButton(Icons.Default.HorizontalRule, "Horizontal Rule") { onFormatAction(FormatAction.HorizontalRule) }
            ToolbarSeparator()
            ToolbarButton(Icons.AutoMirrored.Filled.Undo, "Undo (Ctrl+Z)") { onFormatAction(FormatAction.Undo) }
            ToolbarButton(Icons.AutoMirrored.Filled.Redo, "Redo (Ctrl+Y)") { onFormatAction(FormatAction.Redo) }
        }
    }
}

@Composable
private fun ToolbarButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.width(40.dp).height(40.dp)
    ) {
        Icon(imageVector = icon, contentDescription = description, modifier = Modifier.width(20.dp).height(20.dp))
    }
}

@Composable
private fun ToolbarSeparator() {
    HorizontalDivider(modifier = Modifier.width(1.dp).height(24.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun StatusBar(wordCount: Int, charCount: Int) {
    Surface(tonalElevation = 1.dp, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$wordCount words", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Text("$charCount characters", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
