package com.writingapp.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.writingapp.domain.model.ChatMessage
import com.writingapp.ui.components.MarkdownDisplay

@Composable
fun CopilotPanel(
    viewModel: CopilotViewModel,
    fullEditorContent: String,
    selectedText: String?,
    onAcceptSuggestion: (newContent: String, modifiedText: String) -> Unit,
    onRejectSuggestion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOpen by viewModel.isCopilotOpen.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val suggestion by viewModel.suggestion.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .width(340.dp)
                .fillMaxHeight(),
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                CopilotHeader(
                    suggestionActive = suggestion != null,
                    onClose = { viewModel.toggleCopilot() },
                    onClear = { viewModel.clearMessages() }
                )

                HorizontalDivider()

                if (suggestion != null) {
                    SuggestionCard(
                        suggestion = suggestion!!,
                        onAccept = {
                            val result = viewModel.acceptSuggestion()
                            if (result != null) {
                                onAcceptSuggestion(result.first, result.second)
                            }
                        },
                        onReject = {
                            viewModel.rejectSuggestion()
                            onRejectSuggestion()
                        }
                    )
                }

                if (selectedText != null && selectedText.isNotBlank()) {
                    SelectionActions(
                        selectedText = selectedText,
                        onAction = { instruction ->
                            viewModel.requestSelectionAction(instruction, selectedText, fullEditorContent)
                        }
                    )
                }

                // Chat messages
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages.filter { it.role != "system" }) { msg ->
                        MessageBubble(msg)
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
                }

                HorizontalDivider()

                // Input
                Row(
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask AI...", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    if (isLoading) {
                        IconButton(onClick = { viewModel.cancelStreaming() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendChatMessage(inputText, fullEditorContent)
                                    inputText = ""
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CopilotHeader(suggestionActive: Boolean, onClose: () -> Unit, onClear: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Copilot", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        if (!suggestionActive) {
            ClearButton(onClick = onClear)
        }
        IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SelectionActions(
    selectedText: String,
    onAction: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Selection actions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ActionChip("Rewrite") { onAction("Rewrite the following text to be clearer and more engaging") }
                ActionChip("Expand") { onAction("Expand this text with more details and examples") }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ActionChip("Shorten") { onAction("Shorten this text while keeping the key points") }
                ActionChip("Fix Grammar") { onAction("Fix grammar and spelling errors") }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ActionChip("Formal") { onAction("Rewrite in formal tone") }
                ActionChip("Casual") { onAction("Rewrite in casual tone") }
            }
        }
    }
}

@Composable
private fun ActionChip(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        modifier = Modifier.height(28.dp),
        contentPadding = ButtonDefaults.TextButtonContentPadding,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SuggestionCard(
    suggestion: SuggestionState,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text("Suggestion: ${suggestion.instruction}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.height(4.dp))
            Text(
                suggestion.modifiedText.ifEmpty { if (suggestion.isStreaming) "Thinking..." else "No suggestion" },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 5
            )
            if (suggestion.isStreaming) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAccept,
                    enabled = !suggestion.isStreaming && suggestion.modifiedText.isNotBlank(),
                    modifier = Modifier.weight(1f).height(28.dp),
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Text("Accept", style = MaterialTheme.typography.labelSmall)
                }
                Button(
                    onClick = onReject,
                    enabled = !suggestion.isStreaming,
                    modifier = Modifier.weight(1f).height(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Text("Reject", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isUser) 12.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 12.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (isUser) {
                Text(
                    msg.content,
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                MarkdownDisplay(
                    markdown = msg.content,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun ClearButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(),
        contentPadding = ButtonDefaults.TextButtonContentPadding
    ) {
        Text("Clear", style = MaterialTheme.typography.labelSmall)
    }
}
