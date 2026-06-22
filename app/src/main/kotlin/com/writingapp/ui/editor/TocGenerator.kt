package com.writingapp.ui.editor

data class TocEntry(
    val level: Int,
    val title: String,
    val anchor: String
)

object TocGenerator {
    fun generate(markdown: String): List<TocEntry> {
        val entries = mutableListOf<TocEntry>()
        val pattern = Regex("^(#{1,6})\\s+(.+)$", RegexOption.MULTILINE)
        for (match in pattern.findAll(markdown)) {
            val level = match.groupValues[1].length
            val title = match.groupValues[2].trim()
            val anchor = title.lowercase().replace(Regex("[^a-z0-9\\s-]"), "").replace("\\s+".toRegex(), "-")
            entries.add(TocEntry(level = level, title = title, anchor = anchor))
        }
        return entries
    }
}
