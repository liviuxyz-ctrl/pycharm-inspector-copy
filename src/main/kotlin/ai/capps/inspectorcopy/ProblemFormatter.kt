package ai.capps.inspectorcopy

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile

private const val CONTEXT_LINES = 5

object ProblemFormatter {

    fun format(entries: List<ProblemEntry>): String {
        if (entries.isEmpty()) return "_No problems selected._"
        val sb = StringBuilder()
        sb.appendLine("## Inspection Problems\n")
        entries.forEach { entry ->
            sb.appendLine("### `${entry.filePath}:${entry.line}` — ${entry.severity}")
            sb.appendLine(entry.message)
            sb.appendLine()
            entry.codeSnippet?.let { snippet ->
                sb.appendLine("```python")
                sb.append(snippet)
                sb.appendLine("```")
                sb.appendLine()
            }
        }
        return sb.toString().trimEnd()
    }

    fun extractSnippet(file: VirtualFile, oneBased: Int): String? {
        val doc: Document = FileDocumentManager.getInstance().getDocument(file) ?: return null
        val lineIndex = oneBased - 1
        val start = maxOf(0, lineIndex - CONTEXT_LINES)
        val end = minOf(doc.lineCount - 1, lineIndex + CONTEXT_LINES)
        val sb = StringBuilder()
        for (i in start..end) {
            val lineStart = doc.getLineStartOffset(i)
            val lineEnd = doc.getLineEndOffset(i)
            val lineText = doc.getText(com.intellij.openapi.util.TextRange(lineStart, lineEnd))
            val prefix = if (i == lineIndex) ">" else " "
            sb.appendLine("$prefix ${i + 1}: $lineText")
        }
        return sb.toString()
    }
}

data class ProblemEntry(
    val filePath: String,
    val line: Int,
    val severity: String,
    val message: String,
    val codeSnippet: String?,
)
