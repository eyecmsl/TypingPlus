package com.writingapp.ui.editor

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import java.io.File

object MarkdownExport {

    fun toHtml(markdown: String): String {
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
        html.append("table{border-collapse:collapse;width:100%}")
        html.append("th,td{border:1px solid #e0e0e0;padding:8px;text-align:left}")
        html.append("</style></head><body>")

        var inCodeBlock = false
        var inList = false
        var isOrderedList = false

        for (i in lines.indices) {
            val line = lines[i]

            if (line.startsWith("```")) {
                if (inCodeBlock) {
                    html.append("</code></pre>\n")
                    inCodeBlock = false
                } else {
                    html.append("<pre><code>")
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                html.append(line).append("\n")
                continue
            }

            if (line.matches(Regex("^#{1,6}\\s.*"))) {
                val level = line.takeWhile { it == '#' }.length
                val content = line.drop(level).trim()
                html.append("<h$level>$content</h$level>\n")
                continue
            }

            if (line.matches(Regex("^>\\s.*"))) {
                val content = line.drop(1).trim()
                html.append("<blockquote><p>$content</p></blockquote>\n")
                continue
            }

            if (line.matches(Regex("^[-*+]\\s.*"))) {
                if (!inList || isOrderedList) {
                    if (isOrderedList) html.append("</ol>\n")
                    html.append("<ul>\n")
                    inList = true
                    isOrderedList = false
                }
                val content = line.drop(2).trim()
                html.append("<li>$content</li>\n")
                continue
            }

            if (line.matches(Regex("^\\d+\\.\\s.*"))) {
                if (!inList || !isOrderedList) {
                    if (inList && !isOrderedList) html.append("</ul>\n")
                    html.append("<ol>\n")
                    inList = true
                    isOrderedList = true
                }
                val content = line.substringAfter(". ").trim()
                html.append("<li>$content</li>\n")
                continue
            }

            if (inList && line.isBlank()) {
                if (isOrderedList) html.append("</ol>\n") else html.append("</ul>\n")
                inList = false
                html.append("<br>\n")
                continue
            }

            if (line.matches(Regex("^---+\$"))) {
                html.append("<hr>\n")
                continue
            }

            if (line.isBlank()) {
                html.append("<br>\n")
                continue
            }

            html.append("<p>").append(inlineHtml(line)).append("</p>\n")
        }

        if (inCodeBlock) html.append("</code></pre>\n")
        if (inList) {
            if (isOrderedList) html.append("</ol>\n") else html.append("</ul>\n")
        }

        html.append("</body></html>")
        return html.toString()
    }

    private fun inlineHtml(text: String): String {
        var result = text
        result = result.replace(Regex("""!\[([^\]]*)\]\(([^)]*)\)""")) { match ->
            """<img src="${match.groupValues[2]}" alt="${match.groupValues[1]}">"""
        }
        result = result.replace(Regex("""\[([^\]]*)\]\(([^)]*)\)""")) { match ->
            """<a href="${match.groupValues[2]}">${match.groupValues[1]}</a>"""
        }
        result = result.replace(Regex("""\*\*\*(.+?)\*\*\*""")) { "<strong><em>${it.groupValues[1]}</em></strong>" }
        result = result.replace(Regex("""\*\*(.+?)\*\*""")) { "<strong>${it.groupValues[1]}</strong>" }
        result = result.replace(Regex("""\*(.+?)\*""")) { "<em>${it.groupValues[1]}</em>" }
        result = result.replace(Regex("""~~(.+?)~~""")) { "<del>${it.groupValues[1]}</del>" }
        result = result.replace(Regex("""`([^`]+)`""")) { "<code>${it.groupValues[1]}</code>" }
        return result
    }

    fun shareAsHtml(context: Context, title: String, markdown: String) {
        val html = toHtml(markdown)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, html)
            putExtra(Intent.EXTRA_HTML_TEXT, html)
        }
        context.startActivity(Intent.createChooser(intent, "Export as HTML"))
    }

    fun exportAsPdf(context: Context, title: String, markdown: String) {
        val html = toHtml(markdown)
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val attributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.NA_LETTER)
                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                    .build()
                printManager.print(title, view.createPrintDocumentAdapter(title), attributes)
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}
