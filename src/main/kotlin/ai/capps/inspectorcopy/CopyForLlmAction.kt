package ai.capps.inspectorcopy

import com.intellij.analysis.problemsView.toolWindow.HighlightingProblem
import com.intellij.analysis.problemsView.toolWindow.Node
import com.intellij.analysis.problemsView.toolWindow.ProblemNode
import com.intellij.codeInspection.ui.InspectionTreeNode
import com.intellij.codeInspection.ui.ProblemDescriptionNode
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import java.awt.datatransfer.StringSelection
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

class CopyForLlmAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val entries = mutableListOf<ProblemEntry>()

        // --- Path 1: Problems View (current-file / project-wide highlight panel) ---
        collectFromProblemsView(e, entries)

        // --- Path 2: Inspections Run result tree ---
        if (entries.isEmpty()) collectFromInspectionTree(e, entries)

        val markdown = ProblemFormatter.format(entries)
        CopyPasteManager.getInstance().setContents(StringSelection(markdown))

        com.intellij.openapi.ui.Messages.showInfoMessage(
            project,
            "Copied ${entries.size} problem(s) to clipboard.",
            "Inspector Copy for LLM",
        )
    }

    // ── Problems View (highlighting-based panel) ──────────────────────────────

    private fun collectFromProblemsView(e: AnActionEvent, out: MutableList<ProblemEntry>) {
        val tree = e.getData(LangDataKeys.PSI_ELEMENT_ARRAY) // fallback data key
        // The Problems panel exposes selected nodes via the tree component in focus.
        val component = e.inputEvent?.source as? JTree ?: getFocusedTree() ?: return
        val paths: Array<TreePath> = component.selectionPaths ?: return

        for (path in paths) {
            val node = path.lastPathComponent
            if (node is DefaultMutableTreeNode) {
                val userObj = node.userObject
                if (userObj is ProblemNode) {
                    val problem = userObj.problem
                    if (problem is HighlightingProblem) {
                        val info = problem.highlightInfo
                        val file: VirtualFile = problem.file
                        val doc = com.intellij.openapi.fileEditor.FileDocumentManager
                            .getInstance().getDocument(file) ?: continue
                        val line = doc.getLineNumber(info.startOffset) + 1
                        val severity = severityLabel(info.severity)
                        out += ProblemEntry(
                            filePath = file.path,
                            line = line,
                            severity = severity,
                            message = info.description ?: info.toolTip ?: "unknown",
                            codeSnippet = ProblemFormatter.extractSnippet(file, line),
                        )
                    }
                }
            }
        }
    }

    // ── Inspection Results tree (Run Inspections / Analyze menu) ─────────────

    private fun collectFromInspectionTree(e: AnActionEvent, out: MutableList<ProblemEntry>) {
        val tree = getFocusedTree() ?: return
        val paths: Array<TreePath> = tree.selectionPaths ?: return

        for (path in paths) {
            val treeNode = path.lastPathComponent as? InspectionTreeNode ?: continue
            if (treeNode is ProblemDescriptionNode) {
                val descriptor = treeNode.descriptor ?: continue
                val psiElement = descriptor.psiElement ?: continue
                val file: VirtualFile = psiElement.containingFile?.virtualFile ?: continue
                val doc = PsiDocumentManager.getInstance(psiElement.project)
                    .getDocument(psiElement.containingFile) ?: continue
                val line = doc.getLineNumber(psiElement.textOffset) + 1

                out += ProblemEntry(
                    filePath = file.path,
                    line = line,
                    severity = "Error",
                    message = descriptor.descriptionTemplate,
                    codeSnippet = ProblemFormatter.extractSnippet(file, line),
                )
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun getFocusedTree(): JTree? =
        java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .focusOwner as? JTree

    private fun severityLabel(s: HighlightSeverity): String = when {
        s >= HighlightSeverity.ERROR -> "Error"
        s >= HighlightSeverity.WARNING -> "Warning"
        s >= HighlightSeverity.WEAK_WARNING -> "Weak warning"
        else -> s.name
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
