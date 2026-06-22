package com.writingapp.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.writingapp.domain.repository.DocumentRepository
import com.writingapp.domain.usecase.SaveDocumentUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

enum class FormatAction {
    Bold, Italic, Strikethrough,
    Heading1, Heading2, Heading3,
    BulletList, NumberedList,
    Blockquote, Code, Link, HorizontalRule,
    Undo, Redo
}

class EditorViewModel(
    private val documentRepository: DocumentRepository,
    private val saveDocumentUseCase: SaveDocumentUseCase
) : ViewModel() {

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _wordCount = MutableStateFlow(0)
    val wordCount: StateFlow<Int> = _wordCount.asStateFlow()

    private val _charCount = MutableStateFlow(0)
    val charCount: StateFlow<Int> = _charCount.asStateFlow()

    var title by mutableStateOf("")
        private set

    private var documentId: Long = 0L
    private var saveJob: Job? = null
    private var currentTextFieldValue = TextFieldValue()

    fun loadDocument(id: Long) {
        documentId = id
        viewModelScope.launch {
            documentRepository.getDocumentById(id).collect { doc ->
                if (doc != null) {
                    _content.value = doc.content
                    title = doc.title
                    documentId = doc.id
                    updateCounts(doc.content)
                    currentTextFieldValue = TextFieldValue(doc.content)
                }
            }
        }
    }

    fun updateContent(newContent: String) {
        _content.value = newContent
        updateCounts(newContent)
        autoSave()
    }

    fun updateTitle(newTitle: String) {
        title = newTitle
    }

    fun setTextFieldValue(value: TextFieldValue) {
        currentTextFieldValue = value
    }

    fun formatAction(action: FormatAction) {
        val tv = currentTextFieldValue
        val text = tv.text
        val selection = tv.selection
        val selectedText = if (selection.start < selection.end) {
            text.substring(selection.start, selection.end)
        } else ""

        val result = when (action) {
            FormatAction.Bold -> wrapInline(text, selection, "**", selectedText)
            FormatAction.Italic -> wrapInline(text, selection, "*", selectedText)
            FormatAction.Strikethrough -> wrapInline(text, selection, "~~", selectedText)
            FormatAction.Heading1 -> prependToLine(text, selection, "# ")
            FormatAction.Heading2 -> prependToLine(text, selection, "## ")
            FormatAction.Heading3 -> prependToLine(text, selection, "### ")
            FormatAction.BulletList -> prependToLine(text, selection, "- ")
            FormatAction.NumberedList -> prependToLine(text, selection, "1. ")
            FormatAction.Blockquote -> prependToLine(text, selection, "> ")
            FormatAction.Code -> {
                if (selectedText.contains("\n")) {
                    wrapBlock(text, selection, "```\n", "\n```")
                } else {
                    wrapInline(text, selection, "`", selectedText)
                }
            }
            FormatAction.Link -> {
                if (selectedText.isNotBlank()) {
                    val start = text.substring(0, selection.start)
                    val end = text.substring(selection.end)
                    val newText = "$start[$selectedText](url)$end"
                    val cursorPos = selection.start + selectedText.length + 3 + 5
                    Triple(newText, TextRange(cursorPos), null)
                } else {
                    val start = text.substring(0, selection.start)
                    val end = text.substring(selection.end)
                    val newText = "$start[link text](url)$end"
                    val cursorPos = selection.start + 1
                    Triple(newText, TextRange(cursorPos), null)
                }
            }
            FormatAction.HorizontalRule -> {
                val start = text.substring(0, selection.start)
                val end = text.substring(selection.end)
                val newText = "$start\n\n---\n\n$end"
                val cursorPos = selection.start + 6
                Triple(newText, TextRange(cursorPos), null)
            }
            FormatAction.Undo -> Triple(text, selection, null)
            FormatAction.Redo -> Triple(text, selection, null)
        }

        val (newText, newSelection, _) = result
        val lines = newText?.let {
            if (action == FormatAction.NumberedList) {
                applyNumberedList(it, selection)
            } else null
        }

        val finalText = lines ?: newText
        val finalSelection = lines?.let {
            val pos = it.length - (text.length - selection.start)
            TextRange(pos.coerceIn(0, it.length))
        } ?: newSelection

        if (finalText != null) {
            currentTextFieldValue = TextFieldValue(
                text = finalText,
                selection = finalSelection
            )
            _content.value = finalText
            updateCounts(finalText)
        }
    }

    private fun wrapInline(text: String, selection: TextRange, wrapper: String, selected: String): Triple<String, TextRange, String?> {
        val start = text.substring(0, selection.start)
        val end = text.substring(selection.end)
        val wrapped = if (selected.isEmpty()) "$wrapper$wrapper" else "$wrapper$selected$wrapper"
        val newText = "$start$wrapped$end"
        val cursorPos = if (selected.isEmpty()) selection.start + wrapper.length else selection.start + wrapped.length
        return Triple(newText, TextRange(cursorPos), null)
    }

    private fun wrapBlock(text: String, selection: TextRange, open: String, close: String): Triple<String, TextRange, String?> {
        val start = text.substring(0, selection.start)
        val end = text.substring(selection.end)
        val newText = "$start$open$close$end"
        val cursorPos = selection.start + open.length
        return Triple(newText, TextRange(cursorPos), null)
    }

    private fun prependToLine(
        text: String,
        selection: TextRange,
        prefix: String
    ): Triple<String, TextRange, String?> {
        val lineStart = text.lastIndexOf('\n', selection.start - 1) + 1
        val start = text.substring(0, lineStart)
        val end = text.substring(lineStart)
        val newText = "$start$prefix$end"
        val cursorPos = selection.start + prefix.length
        return Triple(newText, TextRange(cursorPos), null)
    }

    private fun applyNumberedList(text: String, selection: TextRange): String? {
        return null
    }

    private fun updateCounts(text: String) {
        _charCount.value = text.length
        _wordCount.value = text.trim().split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .size
    }

    private fun autoSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1500)
            if (documentId > 0) {
                val currentTitle = title
                val currentContent = _content.value
                saveDocumentUseCase(
                    com.writingapp.domain.model.Document(
                        id = documentId,
                        title = currentTitle,
                        content = currentContent
                    )
                )
            }
        }
    }
}
