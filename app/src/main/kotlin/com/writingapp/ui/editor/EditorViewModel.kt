package com.writingapp.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.writingapp.data.local.db.DocumentVersionDao
import com.writingapp.data.local.db.entities.DocumentVersionEntity
import com.writingapp.domain.repository.DocumentRepository
import com.writingapp.domain.usecase.SaveDocumentUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.util.Stack

enum class FormatAction {
    Bold, Italic, Strikethrough,
    Heading1, Heading2, Heading3,
    BulletList, NumberedList,
    Blockquote, Code, Link, HorizontalRule,
    Undo, Redo
}

class EditorViewModel(
    private val documentRepository: DocumentRepository,
    private val saveDocumentUseCase: SaveDocumentUseCase,
    private val documentVersionDao: DocumentVersionDao
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
    private var lastSavedContent: String = ""

    private val undoStack = Stack<TextFieldValue>()
    private val redoStack = Stack<TextFieldValue>()
    private var isUndoRedoAction = false

    private val autoFormatPrefixes = listOf("- ", "* ", "+ ", "> ")
    private val headingPrefixes = listOf("# ", "## ", "### ", "#### ", "##### ", "###### ")

    fun loadDocument(id: Long) {
        documentId = id
        viewModelScope.launch {
            documentRepository.getDocumentById(id).collect { doc ->
                if (doc != null) {
                    textFieldValue = TextFieldValue(doc.content)
                    title = doc.title
                    documentId = doc.id
                    lastSavedContent = doc.content
                    updateCounts(doc.content)
                    undoStack.clear()
                    redoStack.clear()
                }
            }
        }
    }

    fun updateTextFieldValue(value: TextFieldValue) {
        val oldText = textFieldValue.text

        if (!isUndoRedoAction && value.text.length == oldText.length + 1 && value.selection.start > 0) {
            val insertedChar = value.text[value.selection.start - 1]
            if (insertedChar == '\n') {
                val processed = processAutoFormat(value.text, value.selection.start)
                if (processed != null) {
                    pushUndo(oldText)
                    textFieldValue = TextFieldValue(text = processed.first, selection = TextRange(processed.second))
                    updateCounts(processed.first)
                    autoSave()
                    return
                }
            }
        }

        if (!isUndoRedoAction) {
            pushUndo(oldText)
        }

        textFieldValue = value
        updateCounts(value.text)
        autoSave()
    }

    private fun processAutoFormat(text: String, cursor: Int): Pair<String, Int>? {
        val lineStart = text.lastIndexOf('\n', cursor - 2) + 1
        val currentLine = text.substring(lineStart, cursor - 1)

        for (prefix in autoFormatPrefixes) {
            if (currentLine == prefix.trimEnd()) {
                val newText = text.substring(0, cursor) + text.substring(cursor)
                return Pair(newText, cursor)
            }
        }

        for (prefix in headingPrefixes) {
            if (currentLine == prefix.trimEnd()) {
                val newText = text.substring(0, cursor) + text.substring(cursor)
                return Pair(newText, cursor)
            }
        }

        for (prefix in autoFormatPrefixes) {
            if (currentLine.startsWith(prefix) && currentLine.length > prefix.length) {
                val newText = text.substring(0, cursor) + prefix + text.substring(cursor)
                return Pair(newText, cursor + prefix.length)
            }
        }

        val numberedMatch = Regex("^(\\d+)\\. ").find(currentLine)
        if (numberedMatch != null) {
            val prefix = numberedMatch.value
            if (currentLine.length > prefix.length) {
                val nextNum = numberedMatch.groupValues[1].toInt() + 1
                val nextPrefix = "$nextNum. "
                val newText = text.substring(0, cursor) + nextPrefix + text.substring(cursor)
                return Pair(newText, cursor + nextPrefix.length)
            }
        }

        val fenceMatch = Regex("^(`{3,}|~{3,})$").find(currentLine.trimEnd())
        if (fenceMatch != null && currentLine.trimEnd().length >= 3) {
            val fence = fenceMatch.groupValues[1]
            val before = text.substring(0, lineStart)
            val afterLineStart = text.indexOf('\n', cursor)
            val after = if (afterLineStart >= 0) text.substring(afterLineStart) else ""
            if (!after.startsWith("\n$fence")) {
                val newText = text.substring(0, cursor) + "\n$fence" + text.substring(cursor)
                return Pair(newText, cursor)
            }
        }

        return null
    }

    private fun pushUndo(oldText: String) {
        undoStack.push(TextFieldValue(oldText, TextRange(textFieldValue.selection.start)))
        redoStack.clear()
        if (undoStack.size > 100) {
            undoStack.removeAt(0)
        }
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

        if (action == FormatAction.Undo) {
            undo()
            return
        }
        if (action == FormatAction.Redo) {
            redo()
            return
        }

        pushUndo(text)

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

    private fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.push(textFieldValue)
            isUndoRedoAction = true
            textFieldValue = undoStack.pop()
            updateCounts(textFieldValue.text)
            isUndoRedoAction = false
        }
    }

    private fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.push(textFieldValue)
            isUndoRedoAction = true
            textFieldValue = redoStack.pop()
            updateCounts(textFieldValue.text)
            isUndoRedoAction = false
        }
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
                val currentContent = textFieldValue.text
                if (currentContent != lastSavedContent) {
                    saveDocumentUseCase(
                        com.writingapp.domain.model.Document(
                            id = documentId,
                            title = title,
                            content = currentContent
                        )
                    )
                    val versionCount = documentVersionDao.getVersionCount(documentId)
                    documentVersionDao.insert(
                        DocumentVersionEntity(
                            documentId = documentId,
                            title = title,
                            content = currentContent,
                            versionNumber = versionCount + 1
                        )
                    )
                    documentVersionDao.deleteOlderVersions(documentId, 50)
                    lastSavedContent = currentContent
                }
            }
        }
    }
}
