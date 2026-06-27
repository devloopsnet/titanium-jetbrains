package com.tidev.titanium.intentions

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import com.tidev.titanium.alloy.AlloyKind
import com.tidev.titanium.alloy.AlloyRelated

/**
 * On an Alloy view tag with inline style attributes, extract them into a TSS rule in the related
 * style file (keyed by #id, .class, or tag name) and remove them from the markup.
 */
class AlloyExtractStyleIntention : PsiElementBaseIntentionAction() {

    override fun getFamilyName(): String = "Titanium"
    override fun getText(): String = "Extract style to TSS"
    override fun startInWriteAction(): Boolean = false

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val vfile = element.containingFile?.virtualFile ?: return false
        if (AlloyKind.of(vfile) != AlloyKind.VIEW) return false
        val tag = PsiTreeUtil.getParentOfType(element, XmlTag::class.java, false) ?: return false
        return tag.attributes.any { it.name in STYLE_ATTRS }
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val vfile = element.containingFile?.virtualFile ?: return
        val tag = PsiTreeUtil.getParentOfType(element, XmlTag::class.java, false) ?: return
        val styleAttrs = tag.attributes.filter { it.name in STYLE_ATTRS }
        if (styleAttrs.isEmpty()) return

        val selector = selectorFor(tag)
        val block = buildString {
            append("\n'").append(selector).append("': {\n")
            styleAttrs.forEach { append("    ").append(it.name).append(": ").append(tssValue(it.value.orEmpty())).append(",\n") }
            append("}\n")
        }

        WriteCommandAction.runWriteCommandAction(project, getText(), "Titanium", {
            val styleFile = ensureStyleFile(vfile)
            appendToStyle(styleFile, block)
            styleAttrs.forEach { it.delete() }
            FileEditorManager.getInstance(project).openFile(styleFile, false)
        })
    }

    private fun selectorFor(tag: XmlTag): String {
        tag.getAttributeValue("id")?.takeIf { it.isNotBlank() }?.let { return "#$it" }
        tag.getAttributeValue("class")?.trim()?.split(Regex("\\s+"))?.firstOrNull()?.takeIf { it.isNotBlank() }
            ?.let { return ".$it" }
        return tag.name
    }

    private fun tssValue(raw: String): String = when {
        raw.matches(Regex("-?\\d+(\\.\\d+)?")) -> raw
        raw == "true" || raw == "false" -> raw
        raw.startsWith("Ti.") || raw.startsWith("Titanium.") -> raw
        else -> "'" + raw.replace("'", "\\'") + "'"
    }

    private fun ensureStyleFile(viewFile: VirtualFile): VirtualFile {
        AlloyRelated.related(viewFile, AlloyKind.STYLE)?.let { return it }
        val app = AlloyRelated.appRoot(viewFile)!!
        val dir = VfsUtil.createDirectoryIfMissing(app, "styles")
        val name = viewFile.nameWithoutExtension + ".tss"
        return dir.findChild(name) ?: dir.createChildData(REQUESTOR, name)
    }

    private fun appendToStyle(file: VirtualFile, block: String) {
        val document = FileDocumentManager.getInstance().getDocument(file)
        if (document != null) {
            document.insertString(document.textLength, block)
            FileDocumentManager.getInstance().saveDocument(document)
        } else {
            VfsUtil.saveText(file, VfsUtil.loadText(file) + block)
        }
    }

    private companion object {
        val REQUESTOR = Any()
        val STYLE_ATTRS = setOf(
            "top", "left", "right", "bottom", "width", "height", "color", "backgroundColor",
            "borderRadius", "borderColor", "borderWidth", "font", "opacity", "textAlign", "layout",
            "tintColor", "zIndex", "visible", "horizontalWrap", "touchEnabled",
        )
    }
}
