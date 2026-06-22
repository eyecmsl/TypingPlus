package com.writingapp.ui.editor

object SmartPunctuation {

    fun apply(text: String, start: Int, end: Int): Pair<String, Int> {
        val before = if (start > 0) text[start - 1] else ' '
        val after = if (end < text.length) text[end] else ' '

        var result = text
        var cursorShift = 0

        if (before == '"' && after.isLetterOrDigit()) {
            result = result.substring(0, start - 1) + '\u201c' + result.substring(start)
            cursorShift = 0
        }
        if (start > 0 && text.substring(start - 2, start) == "\" " && after.isWhitespace()) {
            result = result.substring(0, start - 2) + '\u201d' + result.substring(start - 1)
            cursorShift = 0
        }

        if (start >= 2 && text.substring(start - 2, start) == "--") {
            result = result.substring(0, start - 2) + '\u2014' + result.substring(start)
            cursorShift = -1
        }

        return Pair(result, start + cursorShift)
    }

    fun processOnInput(text: String, cursorPos: Int, newChar: Char): Pair<String, Int>? {
        if (newChar == '"' || newChar == '\'') {
            val preceding = if (cursorPos > 0) text.substring(0, cursorPos) else ""
            val isOpening = preceding.isEmpty() || preceding.last().isWhitespace() || preceding.last() == '(' || preceding.last() == '[' || preceding.last() == '{'
            val smartQuote = if (isOpening) {
                if (newChar == '"') '\u201c' else '\u2018'
            } else {
                if (newChar == '"') '\u201d' else '\u2019'
            }
            val newText = text.substring(0, cursorPos) + smartQuote + text.substring(cursorPos)
            return Pair(newText, cursorPos + 1)
        }

        if (newChar == '-' && cursorPos >= 2 && text.substring(cursorPos - 2, cursorPos) == "--") {
            val newText = text.substring(0, cursorPos - 2) + '\u2014' + text.substring(cursorPos)
            return Pair(newText, cursorPos - 1)
        }

        return null
    }
}
