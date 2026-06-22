package com.writingapp.ui.graph

data class GraphNode(
    val id: Long,
    val title: String,
    val backlinks: List<Long> = emptyList(),
    val links: List<Long> = emptyList()
)

data class GraphData(
    val nodes: List<GraphNode> = emptyList()
)

object GraphBuilder {
    fun build(documents: List<com.writingapp.domain.model.Document>): GraphData {
        val nodes = documents.map { doc ->
            val links = mutableListOf<Long>()
            val backlinks = mutableListOf<Long>()

            val wikilinkPattern = Regex("""\[\[([^\]|]+)(?:\|[^\]]+)?\]\]""")
            for (match in wikilinkPattern.findAll(doc.content)) {
                val targetName = match.groupValues[1].trim()
                val targetDoc = documents.find { it.title.equals(targetName, ignoreCase = true) }
                if (targetDoc != null) {
                    links.add(targetDoc.id)
                }
            }

            GraphNode(
                id = doc.id,
                title = doc.title,
                links = links
            )
        }

        val nodeMap = nodes.associateBy { it.id }

        val nodesWithBacklinks = nodes.map { node ->
            val backlinks = nodes.filter { it.id in node.links }.map { it.id }
            node.copy(backlinks = backlinks)
        }

        return GraphData(nodes = nodesWithBacklinks)
    }
}
