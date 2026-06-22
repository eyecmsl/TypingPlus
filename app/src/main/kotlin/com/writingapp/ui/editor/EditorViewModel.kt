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

    var textFieldValue by mutableStateOf(TextFieldValue())
        private set

    val wordCount: Int get() = _wordCount
    val charCount: Int get() = _charCount

    private var _wordCount = 0
    private var _charCount = 0

    var title by mutableStateOf("")
        private set

    private var documentId: Long = 0L
    private var saveJob: Job? = null

    fun loadDocument(id: Long) {
        documentId = id
        viewModelScope.launch {
            documentRepository.getDocumentById(id).collect { doc ->
                if (doc != null) {
                    textFieldValue = TextFieldValue(doc.content)
                    title = doc.title
                    documentId = doc.id
                    updateCounts(doc.content)
                }
            }
        }
    }

    fun updateTextFieldValue(value: TextFieldValue) {
        textFieldValue = value
        updateCounts(value.text)
        autoSave()
    }

    fun updateTitle(newTitle: String) {
        title = newTitle
    }

    fun formatAction(action: FormatAction) {
        val tv = textFieldValue
        val text = tv.text
        val selection = tv.selection
        val selectedText = if (selection.start < selection.end) {
            text.substring(selection.start, selection.end)
        } else ""

        val (newText, newSelection) = when (action) {
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
                    Pair(newText, TextRange(cursorPos))
                } else {
                    val start = text.substring(0, selection.start)
                    val end = text.substring(selection.end)
                    val newText = "$start[link text](url)$end"
                    val cursorPos = selection.start + 1
                    Pair(newText, TextRange(cursorPos))
                }
            }
            FormatAction.HorizontalRule -> {
                val start = text.substring(0, selection.start)
                val end = text.substring(selection.end)
                val newText = "$start\n\n---\n\n$end"
                val cursorPos = selection.start + 6
                Pair(newText, TextRange(cursorPos))
            }
            FormatAction.Undo, FormatAction.Redo -> Pair(text, selection)
        }

        val finalText = if (action == FormatAction.NumberedList) {
            applyNumberedList(newText, selection) ?: newText
        } else newText

        textFieldValue = TextFieldValue(
            text = finalText,
            selection = newSelection
        )
        updateCounts(finalText)
    }

    private fun wrapInline(text: String, selection: TextRange, wrapper: String, selected: String): Pair<String, TextRange> {
        val start = text.substring(0, selection.start)
        val end = text.substring(selection.end)
        val wrapped = if (selected.isEmpty()) "$wrapper$wrapper" else "$wrapper$selected$wrapper"
        val newText = "$start$wrapped$end"
        val cursorPos = if (selected.isEmpty()) selection.start + wrapper.length else selection.start + wrapped.length
        return Pair(newText, TextRange(cursorPos))
    }

    private fun wrapBlock(text: String, selection: TextRange, open: String, close: String): Pair<String, TextRange> {
        val start = text.substring(0, selection.start)
        val end = text.substring(selection.end)
        val newText = "$start$open$close$end"
        val cursorPos = selection.start + open.length
        return Pair(newText, TextRange(cursorPos))
    }

    private fun prependToLine(text: String, selection: TextRange, prefix: String): Pair<String, TextRange> {
        val lineStart = text.lastIndexOf('\n', selection.start - 1) + 1
        val start = text.substring(0, lineStart)
        val end = text.substring(lineStart)
        val newText = "$start$prefix$end"
        val cursorPos = selection.start + prefix.length
        return Pair(newText, TextRange(cursorPos))
    }

    private fun applyNumberedList(text: String, selection: TextRange): String? = null

    private fun updateCounts(text: String) {
        _charCount = text.length
        _wordCount = text.trim().split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .size
    }

    private fun autoSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1500)
            if (documentId > 0) {
                saveDocumentUseCase(
                    com.writingapp.domain.model.Document(
                        id = documentId,
                        title = title,
                        content = textFieldValue.text
                    )
                )
            }
        }
    }
}
