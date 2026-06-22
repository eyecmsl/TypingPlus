package com.writingapp.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class MarkdownVisualTransformation(
    private val syntaxColor: Color = Color.Gray.copy(alpha = 0.5f),
    private val headingColor: Color = Color(0xFFE65100),
    private val linkColor: Color = Color(0xFF1565C0),
    private val codeColor: Color = Color(0xFF2E7D32),
    private val quoteColor: Color = Color(0xFF6A1B9A)
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val builder = AnnotatedString.Builder(raw)

        applyFormattingSyntax(raw, builder, syntaxColor)
        applyHeadings(raw, builder, headingColor)
        applyLinks(raw, builder, linkColor)
        applyCode(raw, builder, codeColor)
        applyQuotes(raw, builder, quoteColor)

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    private fun applyFormattingSyntax(raw: String, builder: AnnotatedString.Builder, color: Color) {
        val patterns = listOf(
            Regex("""\*\*"""),
            Regex("""__"""),
            Regex("""(?<!\*)\*(?!\*)"""),
            Regex("""(?<!_) _(?!_)"""),
            Regex("""~~"""),
            Regex("""`(?!``)"""),
            Regex(""""```"""),
        )
        for (pattern in patterns) {
            for (match in pattern.findAll(raw)) {
                builder.addStyle(SpanStyle(color = color), match.range.first, match.range.last + 1)
            }
        }
    }

    private fun applyHeadings(raw: String, builder: AnnotatedString.Builder, color: Color) {
        val headingPattern = Regex("""^(#{1,6})\s""", RegexOption.MULTILINE)
        for (match in headingPattern.findAll(raw)) {
            val range = match.range
            builder.addStyle(
                SpanStyle(color = color, fontWeight = FontWeight.Bold),
                range.first, range.last + 1
            )
            val lineEnd = raw.indexOf('\n', range.last)
            val end = if (lineEnd >= 0) lineEnd else raw.length
            builder.addStyle(
                SpanStyle(color = color.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold),
                range.first, end
            )
        }
    }

    private fun applyLinks(raw: String, builder: AnnotatedString.Builder, color: Color) {
        val linkPattern = Regex("""\[([^\]]*)\]\(([^)]*)\)""")
        for (match in linkPattern.findAll(raw)) {
            builder.addStyle(SpanStyle(color = color), match.range.first, match.range.last + 1)
        }

        val imagePattern = Regex("""!\[([^\]]*)\]\(([^)]*)\)""")
        for (match in imagePattern.findAll(raw)) {
            builder.addStyle(SpanStyle(color = color.copy(alpha = 0.6f)), match.range.first, match.range.last + 1)
        }
    }

    private fun applyCode(raw: String, builder: AnnotatedString.Builder, color: Color) {
        val inlineCode = Regex("""(`[^`]+`)""")
        for (match in inlineCode.findAll(raw)) {
            builder.addStyle(
                SpanStyle(color = color, fontStyle = FontStyle.Normal),
                match.range.first, match.range.last + 1
            )
        }

        val fenceCode = Regex("""```[\s\S]*?```""")
        for (match in fenceCode.findAll(raw)) {
            builder.addStyle(
                SpanStyle(color = color.copy(alpha = 0.8f)),
                match.range.first, match.range.last + 1
            )
        }
    }

    private fun applyQuotes(raw: String, builder: AnnotatedString.Builder, color: Color) {
        val quotePattern = Regex("""^>\s""", RegexOption.MULTILINE)
        for (match in quotePattern.findAll(raw)) {
            builder.addStyle(SpanStyle(color = color), match.range.first, match.range.last + 1)
        }
    }
}

data class Wikilink(val name: String, val displayText: String)

fun parseWikilinks(text: String): List<Wikilink> {
    val pattern = Regex("""\[\[([^\]|]+)(?:\|([^\]]+))?\]\]""")
    return pattern.findAll(text).map { match ->
        val name = match.groupValues[1].trim()
        val displayText = match.groupValues[2].ifBlank { name }
        Wikilink(name, displayText)
    }.toList()
}

fun renderWikilinks(text: String): String {
    return text.replace(Regex("""\[\[([^\]|]+)(?:\|([^\]]+))?\]\]""")) { match ->
        val name = match.groupValues[1].trim()
        val displayText = match.groupValues[2].ifBlank { name }
        "[$displayText](wikilink://$name)"
    }
}
