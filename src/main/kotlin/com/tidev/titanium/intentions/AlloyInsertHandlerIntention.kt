package com.tidev.titanium.intentions

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.tidev.titanium.alloy.AlloyKind
import com.tidev.titanium.alloy.AlloyRelated

/**
 * On an Alloy view, when the caret is on an event attribute (e.g. `onClick="doStuff"`) whose
 * handler doesn't yet exist in the related controller, offer to generate the function stub.
 *
 * Pure XML-PSI + document text edits — no JavaScript PSI, so it works on every IDE.
 */
class AlloyInsertHandlerIntention : PsiElementBaseIntentionAction() {

    override fun getFamilyName(): String = "Titanium"
    override fun getText(): String = "Create Alloy event handler in controller"
    override fun startInWriteAction(): Boolean = false

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val vfile = element.containingFile?.virtualFile ?: return false
        if (AlloyKind.of(vfile) != AlloyKind.VIEW) return false
        val handler = handlerAt(element) ?: return false
        val controller = AlloyRelated.related(vfile, AlloyKind.CONTROLLER) ?: return false
        return !controllerHasFunction(controller, handler)
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val vfile = element.containingFile?.virtualFile ?: return
        val handler = handlerAt(element) ?: return
        val controller = AlloyRelated.related(vfile, AlloyKind.CONTROLLER) ?: return
        val document = FileDocumentManager.getInstance().getDocument(controller) ?: return

        WriteCommandAction.runWriteCommandAction(project, getText(), "Titanium", {
            val template = com.tidev.titanium.settings.TiSettings.getInstance().state.jsFunctionTemplate
            val body = template.replace("{name}", handler).ifBlank { "function $handler(e) {\n}" }
            document.insertString(document.textLength, "\n$body\n")
            FileDocumentManager.getInstance().saveDocument(document)
        })
        FileEditorManager.getInstance(project).openFile(controller, true)
    }

    /** The handler name if [element] sits in an `on…="name"` attribute with an identifier value. */
    private fun handlerAt(element: PsiElement): String? {
        val attr = PsiTreeUtil.getParentOfType(element, XmlAttribute::class.java, false) ?: return null
        val name = attr.name
        if (!name.startsWith("on") || name.length <= 2) return null
        val value = attr.value?.trim().orEmpty()
        return value.takeIf { it.isNotEmpty() && it.all { c -> c.isLetterOrDigit() || c == '_' } }
    }

    private fun controllerHasFunction(controller: VirtualFile, name: String): Boolean {
        val text = FileDocumentManager.getInstance().getDocument(controller)?.charsSequence?.toString()
            ?: runCatching { String(controller.contentsToByteArray()) }.getOrDefault("")
        return text.contains("function $name(") || Regex("\\b${Regex.escape(name)}\\s*[:=]").containsMatchIn(text)
    }
}
