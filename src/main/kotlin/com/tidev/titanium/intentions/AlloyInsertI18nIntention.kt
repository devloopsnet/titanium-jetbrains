package com.tidev.titanium.intentions

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.tidev.titanium.alloy.AlloyKind
import com.tidev.titanium.alloy.AlloyRelated
import com.tidev.titanium.settings.TiSettings

/**
 * On an Alloy view, when the caret is on an i18n reference (`L('key')` or `textid="key"`) whose key
 * isn't in `app/i18n/<lang>/strings.xml`, offer to add it. XML-PSI + VFS only — works on any IDE.
 */
class AlloyInsertI18nIntention : PsiElementBaseIntentionAction() {

    override fun getFamilyName(): String = "Titanium"
    override fun getText(): String = "Add i18n string to strings.xml"
    override fun startInWriteAction(): Boolean = false

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val vfile = element.containingFile?.virtualFile ?: return false
        if (AlloyKind.of(vfile) != AlloyKind.VIEW) return false
        val key = i18nKey(element) ?: return false
        return !stringExists(vfile, key)
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val vfile = element.containingFile?.virtualFile ?: return
        val key = i18nKey(element) ?: return
        val app = AlloyRelated.appRoot(vfile) ?: return
        val lang = TiSettings.getInstance().state.defaultI18nLanguage.ifBlank { "en" }

        WriteCommandAction.runWriteCommandAction(project, getText(), "Titanium", {
            val dir = VfsUtil.createDirectoryIfMissing(app, "i18n/$lang")
            val existing = dir.findChild("strings.xml")
            if (existing == null) {
                val file = dir.createChildData(REQUESTOR, "strings.xml")
                VfsUtil.saveText(
                    file,
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<resources>\n    <string name=\"$key\">$key</string>\n</resources>\n",
                )
            } else {
                addEntry(existing, key)
            }
        })
    }

    private fun addEntry(file: VirtualFile, key: String) {
        val entry = "    <string name=\"$key\">$key</string>\n"
        val document = FileDocumentManager.getInstance().getDocument(file)
        if (document != null) {
            val idx = document.text.lastIndexOf("</resources>")
            if (idx >= 0) document.insertString(idx, entry) else document.insertString(document.textLength, entry)
            FileDocumentManager.getInstance().saveDocument(document)
        } else {
            val text = VfsUtil.loadText(file)
            val updated = if (text.contains("</resources>")) text.replaceFirst("</resources>", "$entry</resources>")
            else text + entry
            VfsUtil.saveText(file, updated)
        }
    }

    private fun i18nKey(element: PsiElement): String? {
        val attr = PsiTreeUtil.getParentOfType(element, XmlAttribute::class.java, false) ?: return null
        val value = attr.value?.trim().orEmpty()
        if (attr.name == "textid" && value.isNotBlank()) return value
        return Regex("""L\(\s*['"]([^'"]+)['"]""").find(value)?.groupValues?.get(1)
    }

    private fun stringExists(viewFile: VirtualFile, key: String): Boolean {
        val app = AlloyRelated.appRoot(viewFile) ?: return false
        val i18n = app.findChild("i18n") ?: return false
        for (langDir in i18n.children) {
            val strings = langDir.findChild("strings.xml") ?: continue
            val text = runCatching { VfsUtil.loadText(strings) }.getOrDefault("")
            if (text.contains("name=\"$key\"")) return true
        }
        return false
    }

    private companion object {
        val REQUESTOR = Any()
    }
}
