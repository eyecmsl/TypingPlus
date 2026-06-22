package com.writingapp.ui.editor

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object MarkdownExport {

    fun toHtml(markdown: String, title: String = ""): String {
        val escaped = markdown
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

        val lines = escaped.split("\n")
        val html = StringBuilder()
        html.append("""<!DOCTYPE html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">""")
        html.append("<style>")
        html.append("body{font-family:-apple-system,BlinkMacSystemFont,sans-serif;max-width:800px;margin:0 auto;padding:16px;line-height:1.6;color:#1c1b1f}")
        html.append("h1,h2,h3,h4{line-height:1.25;margin-top:24px;margin-bottom:8px}")
        html.append("h1{border-bottom:1px solid #e0e0e0;padding-bottom:8px}")
        html.append("code{background:#f5f5f5;padding:2px 6px;border-radius:4px;font-size:0.9em}")
        html.append("pre{background:#f5f5f5;padding:16px;border-radius:8px;overflow-x:auto}")
        html.append("pre code{padding:0;background:transparent}")
        html.append("blockquote{border-left:4px solid #6750a4;margin:0;padding:0 16px;color:#49454f}")
        html.append("img{max-width:100%;height:auto;border-radius:8px}")
        html.append("hr{border:none;border-top:1px solid #e0e0e0;margin:24px 0}")
        html.append("</style></head><body>")
        if (title.isNotBlank()) {
            html.append("<h1>").append(escapedTitle(title)).append("</h1>")
        }

        var inCodeBlock = false
        var inList = false
        var isOrderedList = false

        for (line in lines) {
            when {
                line.startsWith("```") -> {
                    if (inCodeBlock) { html.append("</code></pre>\n"); inCodeBlock = false }
                    else { html.append("<pre><code>"); inCodeBlock = true }
                }
                inCodeBlock -> html.append(line).append("\n")
                line.matches(Regex("^#{1,6}\\s.*")) -> {
                    val level = line.takeWhile { it == '#' }.length
                    html.append("<h$level>").append(line.drop(level).trim()).append("</h$level>\n")
                }
                line.matches(Regex("^>\\s.*")) -> html.append("<blockquote><p>").append(line.drop(1).trim()).append("</p></blockquote>\n")
                line.matches(Regex("^[-*+]\\s.*")) -> {
                    if (!inList || isOrderedList) { if (isOrderedList) html.append("</ol>\n"); html.append("<ul>\n"); inList = true; isOrderedList = false }
                    html.append("<li>").append(line.drop(2).trim()).append("</li>\n")
                }
                line.matches(Regex("^\\d+\\.\\s.*")) -> {
                    if (!inList || !isOrderedList) { if (inList && !isOrderedList) html.append("</ul>\n"); html.append("<ol>\n"); inList = true; isOrderedList = true }
                    html.append("<li>").append(line.substringAfter(". ").trim()).append("</li>\n")
                }
                inList && line.isBlank() -> {
                    if (isOrderedList) html.append("</ol>\n") else html.append("</ul>\n")
                    inList = false; html.append("<br>\n")
                }
                line.matches(Regex("^---+\$")) -> html.append("<hr>\n")
                line.isBlank() -> html.append("<br>\n")
                else -> html.append("<p>").append(inlineHtml(line)).append("</p>\n")
            }
        }

        if (inCodeBlock) html.append("</code></pre>\n")
        if (inList) { if (isOrderedList) html.append("</ol>\n") else html.append("</ul>\n") }

        html.append("</body></html>")
        return html.toString()
    }

    private fun escapedTitle(title: String): String {
        return title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    }

    private fun inlineHtml(text: String): String {
        var result = text
        result = result.replace(Regex("""!\[([^\]]*)\]\(([^)]*)\)""")) { """<img src="${it.groupValues[2]}" alt="${it.groupValues[1]}">""" }
        result = result.replace(Regex("""\[([^\]]*)\]\(([^)]*)\)""")) { """<a href="${it.groupValues[2]}">${it.groupValues[1]}</a>""" }
        result = result.replace(Regex("""\*\*\*(.+?)\*\*\*""")) { "<strong><em>${it.groupValues[1]}</em></strong>" }
        result = result.replace(Regex("""\*\*(.+?)\*\*""")) { "<strong>${it.groupValues[1]}</strong>" }
        result = result.replace(Regex("""\*(.+?)\*""")) { "<em>${it.groupValues[1]}</em>" }
        result = result.replace(Regex("""~~(.+?)~~""")) { "<del>${it.groupValues[1]}</del>" }
        result = result.replace(Regex("""`([^`]+)`""")) { "<code>${it.groupValues[1]}</code>" }
        return result
    }

    fun shareAsHtml(context: Context, title: String, markdown: String) {
        val html = toHtml(markdown, title)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, html)
            putExtra(Intent.EXTRA_HTML_TEXT, html)
        }
        val chooser = Intent.createChooser(intent, "Export as HTML")
        context.startActivity(chooser)
    }

    fun exportAsPdf(context: Context, title: String, markdown: String): Result<Uri> {
        return try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(612, 792, 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val titlePaint = Paint().apply {
                typeface = Typeface.DEFAULT_BOLD
                textSize = 24f
                color = android.graphics.Color.parseColor("#1C1B1F")
            }
            val bodyPaint = Paint().apply {
                textSize = 14f
                color = android.graphics.Color.parseColor("#1C1B1F")
                isAntiAlias = true
            }

            var y = 50f
            val margin = 50f
            val maxWidth = 512f

            if (title.isNotBlank()) {
                canvas.drawText(title, margin, y, titlePaint)
                y += 40f
            }

            val lines = markdown.split("\n")
            for (line in lines) {
                if (y > 750f) break
                if (line.isBlank()) {
                    y += 20f
                    continue
                }
                val text = line.replace(Regex("[#*_`~\\[\\]()>|\\-]"), "")
                if (text.isNotBlank()) {
                    val wrappedLines = wrapText(text, bodyPaint, maxWidth)
                    for (wl in wrappedLines) {
                        if (y > 750f) break
                        canvas.drawText(wl, margin, y, bodyPaint)
                        y += 22f
                    }
                } else {
                    y += 10f
                }
            }

            document.finishPage(page)

            val file = File(context.cacheDir, "${title.take(30).replace(Regex("[^a-zA-Z0-9]"), "_")}.pdf")
            FileOutputStream(file).use { out -> document.writeTo(out) }
            document.close()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        val currentLine = StringBuilder()
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                if (currentLine.isNotEmpty()) currentLine.append(" ")
                currentLine.append(word)
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
                currentLine.clear()
                currentLine.append(word)
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return lines.ifEmpty { listOf(text) }
    }

    fun exportToFile(context: Context, title: String, markdown: String): Result<Uri> {
        return try {
            val md = "# $title\n\n$markdown"
            val file = File(context.getExternalFilesDir(null), "${title.take(30).replace(Regex("[^a-zA-Z0-9]"), "_")}.md")
            file.writeText(md)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
