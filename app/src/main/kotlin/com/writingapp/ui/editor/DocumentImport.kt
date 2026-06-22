package com.writingapp.ui.editor

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DocumentImport {

    suspend fun importFromUri(context: Context, uri: Uri): Result<ImportedDocument> = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
            val fileName = uri.lastPathSegment ?: "imported.md"
            val title = fileName.substringBeforeLast(".")
            val extension = fileName.substringAfterLast(".", "").lowercase()

            val markdown = when (extension) {
                "txt" -> content
                "md" -> content
                "html", "htm" -> htmlToMarkdown(content)
                else -> content
            }

            Result.success(ImportedDocument(title = title, content = markdown))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class ImportedDocument(val title: String, val content: String)

    private fun htmlToMarkdown(html: String): String {
        var md = html
        md = md.replace(Regex("<h1[^>]*>(.*?)</h1>", RegexOption.DOT_MATCHES_ALL)) { "# ${it.groupValues[1]}\n\n" }
        md = md.replace(Regex("<h2[^>]*>(.*?)</h2>", RegexOption.DOT_MATCHES_ALL)) { "## ${it.groupValues[1]}\n\n" }
        md = md.replace(Regex("<h3[^>]*>(.*?)</h3>", RegexOption.DOT_MATCHES_ALL)) { "### ${it.groupValues[1]}\n\n" }
        md = md.replace(Regex("<strong>(.*?)</strong>", RegexOption.DOT_MATCHES_ALL)) { "**${it.groupValues[1]}**" }
        md = md.replace(Regex("<em>(.*?)</em>", RegexOption.DOT_MATCHES_ALL)) { "*${it.groupValues[1]}*" }
        md = md.replace(Regex("<a\\s+href=\"(.*?)\"[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)) { "[${it.groupValues[2]}](${it.groupValues[1]})" }
        md = md.replace(Regex("<p[^>]*>(.*?)</p>", RegexOption.DOT_MATCHES_ALL)) { "${it.groupValues[1]}\n\n" }
        md = md.replace(Regex("<br\\s*/?>", RegexOption.DOT_MATCHES_ALL)) { "\n" }
        md = md.replace(Regex("<li[^>]*>(.*?)</li>", RegexOption.DOT_MATCHES_ALL)) { "- ${it.groupValues[1]}\n" }
        md = md.replace(Regex("<[^>]+>"), "")
        md = md.replace(Regex("\\n{3,}"), "\n\n")
        return md.trim()
    }
}
