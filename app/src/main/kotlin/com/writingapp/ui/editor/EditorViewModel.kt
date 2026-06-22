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

data class DocumentStats(
    val wordCount: Int = 0,
    val charCount: Int = 0,
    val charCountNoSpaces: Int = 0,
    val paragraphCount: Int = 0,
    val sentenceCount: Int = 0,
    val readingTimeMinutes: Int = 0
)

class EditorViewModel(
    private val documentRepository: DocumentRepository,
    private val saveDocumentUseCase: SaveDocumentUseCase,
    private val documentVersionDao: DocumentVersionDao
) : ViewModel() {

    var textFieldValue by mutableStateOf(TextFieldValue())
        private set

    var stats by mutableStateOf(DocumentStats())
        private set

    var title by mutableStateOf("")
        private set

    var isFocusMode by mutableStateOf(false)
        private set
    var showFindReplace by mutableStateOf(false)
        private set
    var findQuery by mutableStateOf("")
    var replaceQuery by mutableStateOf("")
    var findMatchCount by mutableStateOf(0)
        private set
    var currentFindIndex by mutableStateOf(0)
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
                    updateAllStats(doc.content)
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
                    updateAllStats(processed.first)
                    autoSave()
                    return
                }
            }
        }

        if (!isUndoRedoAction) {
            pushUndo(oldText)
        }

        textFieldValue = value
        updateAllStats(value.text)
        autoSave()
    }

    fun toggleFocusMode() {
        isFocusMode = !isFocusMode
    }

    fun toggleFindReplace() {
        showFindReplace = !showFindReplace
        if (!showFindReplace) {
            findQuery = ""
            replaceQuery = ""
            findMatchCount = 0
            currentFindIndex = 0
        }
    }

    fun performFind(query: String) {
        findQuery = query
        if (query.isBlank()) {
            findMatchCount = 0
            currentFindIndex = 0
            return
        }
        val text = textFieldValue.text
        val matches = findAllMatches(text, query)
        findMatchCount = matches.size
        currentFindIndex = if (matches.isNotEmpty()) 0 else 0
    }

    fun findNext() {
        if (findMatchCount == 0 || findQuery.isBlank()) return
        val text = textFieldValue.text
        val matches = findAllMatches(text, findQuery)
        if (matches.isEmpty()) return
        currentFindIndex = (currentFindIndex + 1) % matches.size
        val match = matches[currentFindIndex]
        textFieldValue = textFieldValue.copy(selection = TextRange(match.first, match.last))
    }

    fun findPrevious() {
        if (findMatchCount == 0 || findQuery.isBlank()) return
        val text = textFieldValue.text
        val matches = findAllMatches(text, findQuery)
        if (matches.isEmpty()) return
        currentFindIndex = if (currentFindIndex <= 0) matches.size - 1 else currentFindIndex - 1
        val match = matches[currentFindIndex]
        textFieldValue = textFieldValue.copy(selection = TextRange(match.first, match.last))
    }

    fun replaceCurrent(replacement: String) {
        if (findMatchCount == 0 || findQuery.isBlank()) return
        val text = textFieldValue.text
        val matches = findAllMatches(text, findQuery)
        if (matches.isEmpty()) return
        val match = matches[currentFindIndex.coerceAtMost(matches.size - 1)]
        val newText = text.substring(0, match.first) + replacement + text.substring(match.last)
        val newCursor = match.first + replacement.length
        pushUndo(text)
        textFieldValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
        updateAllStats(newText)
        performFind(findQuery)
    }

    fun replaceAll(find: String, replacement: String) {
        if (find.isBlank()) return
        val text = textFieldValue.text
        val newText = text.replace(find, replacement)
        pushUndo(text)
        textFieldValue = TextFieldValue(text = newText, selection = TextRange(0))
        updateAllStats(newText)
        performFind(find)
    }

    private fun findAllMatches(text: String, query: String): List<IntRange> {
        val matches = mutableListOf<IntRange>()
        var startIndex = 0
        while (true) {
            val index = text.indexOf(query, startIndex, ignoreCase = true)
            if (index < 0) break
            matches.add(index until (index + query.length))
            startIndex = index + 1
        }
        return matches
    }

    fun setTags(tags: List<String>) {
        viewModelScope.launch {
            if (documentId > 0) {
                documentRepository.setTags(documentId, tags)
            }
        }
    }

    fun togglePin() {
        viewModelScope.launch {
            if (documentId > 0) {
                documentRepository.setPinned(documentId, !isPinned)
                isPinned = !isPinned
            }
        }
    }

    var isPinned by mutableStateOf(false)
        private set

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
        if (undoStack.size > 100) undoStack.removeAt(0)
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

        if (action == FormatAction.Undo) { undo(); return }
        if (action == FormatAction.Redo) { redo(); return }

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
                if (selectedText.contains("\n")) wrapBlock(text, selection, "```\n", "\n```")
                else wrapInline(text, selection, "`", selectedText)
            }
            FormatAction.Link -> {
                if (selectedText.isNotBlank()) {
                    val start = text.substring(0, selection.start)
                    val end = text.substring(selection.end)
                    Pair("$start[$selectedText](url)$end", TextRange(selection.start + selectedText.length + 8))
                } else {
                    val start = text.substring(0, selection.start)
                    val end = text.substring(selection.end)
                    Pair("$start[link text](url)$end", TextRange(selection.start + 1))
                }
            }
            FormatAction.HorizontalRule -> {
                val start = text.substring(0, selection.start)
                val end = text.substring(selection.end)
                Pair("$start\n\n---\n\n$end", TextRange(selection.start + 6))
            }
            FormatAction.Undo, FormatAction.Redo -> Pair(text, selection)
        }

        val finalText = if (action == FormatAction.NumberedList) {
            applyNumberedList(newText, selection) ?: newText
        } else newText

        textFieldValue = TextFieldValue(text = finalText, selection = newSelection)
        updateAllStats(finalText)
    }

    private fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.push(textFieldValue)
            isUndoRedoAction = true
            textFieldValue = undoStack.pop()
            updateAllStats(textFieldValue.text)
            isUndoRedoAction = false
        }
    }

    private fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.push(textFieldValue)
            isUndoRedoAction = true
            textFieldValue = redoStack.pop()
            updateAllStats(textFieldValue.text)
            isUndoRedoAction = false
        }
    }

    private fun wrapInline(text: String, selection: TextRange, wrapper: String, selected: String): Pair<String, TextRange> {
        val start = text.substring(0, selection.start)
        val end = text.substring(selection.end)
        val wrapped = if (selected.isEmpty()) "$wrapper$wrapper" else "$wrapper$selected$wrapper"
        return Pair("$start$wrapped$end", TextRange(selection.start + wrapper.length))
    }

    private fun wrapBlock(text: String, selection: TextRange, open: String, close: String): Pair<String, TextRange> {
        val start = text.substring(0, selection.start)
        val end = text.substring(selection.end)
        return Pair("$start$open$close$end", TextRange(selection.start + open.length))
    }

    private fun prependToLine(text: String, selection: TextRange, prefix: String): Pair<String, TextRange> {
        val lineStart = text.lastIndexOf('\n', selection.start - 1) + 1
        val start = text.substring(0, lineStart)
        val end = text.substring(lineStart)
        return Pair("$start$prefix$end", TextRange(selection.start + prefix.length))
    }

    private fun applyNumberedList(text: String, selection: TextRange): String? = null

    private fun updateAllStats(text: String) {
        val words = text.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val wordCount = words.size
        val charCount = text.length
        val charCountNoSpaces = text.count { !it.isWhitespace() }
        val paragraphs = text.split("\n\n").filter { it.isNotBlank() }
        val paragraphCount = paragraphs.size
        val sentenceCount = text.split(Regex("[.!?]+")).filter { it.isNotBlank() }.size
        val readingTimeMinutes = (wordCount / 200).coerceAtLeast(1)

        stats = DocumentStats(
            wordCount = wordCount,
            charCount = charCount,
            charCountNoSpaces = charCountNoSpaces,
            paragraphCount = paragraphCount,
            sentenceCount = sentenceCount,
            readingTimeMinutes = readingTimeMinutes
        )
    }

    var suggestionPreview by mutableStateOf<String?>(null)
        private set

    var ghostTextPreview by mutableStateOf<String?>(null)
        private set

    var originalTextBeforeSuggestion by mutableStateOf<String?>(null)
        private set

    var selectedText by mutableStateOf<String?>(null)

    fun applySuggestionPreview(modified: String, original: String) {
        originalTextBeforeSuggestion = textFieldValue.text
        val current = textFieldValue.text
        if (current.contains(original)) {
            textFieldValue = TextFieldValue(
                text = current.replaceFirst(original, modified),
                selection = TextRange(0)
            )
        }
        suggestionPreview = modified
    }

    fun acceptSuggestion() {
        suggestionPreview = null
        originalTextBeforeSuggestion = null
        updateAllStats(textFieldValue.text)
        autoSave()
    }

    fun revertSuggestion() {
        val orig = originalTextBeforeSuggestion
        if (orig != null) {
            textFieldValue = TextFieldValue(orig, TextRange(0))
        }
        suggestionPreview = null
        originalTextBeforeSuggestion = null
    }

    fun applyCopilotSuggestion(newContent: String) {
        textFieldValue = TextFieldValue(newContent, TextRange(0))
        suggestionPreview = null
        originalTextBeforeSuggestion = null
        updateAllStats(newContent)
        autoSave()
    }

    fun applyGhostText(text: String) {
        val tv = textFieldValue
        val cursor = tv.selection.start
        val newText = tv.text.substring(0, cursor) + text + tv.text.substring(cursor)
        textFieldValue = tv.copy(
            text = newText,
            selection = TextRange(cursor + text.length)
        )
        ghostTextPreview = null
        updateAllStats(newText)
        autoSave()
    }

    fun dismissGhostText() {
        ghostTextPreview = null
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
                            content = currentContent,
                            wordCount = stats.wordCount,
                            charCount = stats.charCount,
                            paragraphCount = stats.paragraphCount,
                            sentenceCount = stats.sentenceCount
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
