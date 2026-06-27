package com.tidev.titanium.alloy

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute

/**
 * Go-to-definition from an Alloy view:
 *  - an `onXxx="handler"` value jumps to `function handler` in the related controller;
 *  - an `L('key')` / `textid="key"` value jumps to the `<string name="key">` entry in strings.xml.
 *
 * Uses text offsets (no JS/TSS PSI), so it works on every IDE.
 */
class AlloyGotoDeclarationHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(
        element: PsiElement?,
        offset: Int,
        editor: Editor?,
    ): Array<PsiElement>? {
        element ?: return null
        val vfile = element.containingFile?.virtualFile ?: return null
        if (AlloyKind.of(vfile) != AlloyKind.VIEW) return null
        val attr = PsiTreeUtil.getParentOfType(element, XmlAttribute::class.java, false) ?: return null
        val value = attr.value?.trim().orEmpty()
        if (value.isEmpty()) return null

        // Event handler -> controller function.
        if (attr.name.startsWith("on") && attr.name.length > 2 && value.all { it.isLetterOrDigit() || it == '_' }) {
            controllerTarget(element, vfile, value)?.let { return arrayOf(it) }
        }

        // i18n key -> strings.xml entry.
        val key = if (attr.name == "textid") value
        else Regex("""L\(\s*['"]([^'"]+)['"]""").find(value)?.groupValues?.get(1)
        if (key != null) {
            i18nTarget(element, vfile, key)?.let { return arrayOf(it) }
        }
        return null
    }

    private fun controllerTarget(element: PsiElement, vfile: com.intellij.openapi.vfs.VirtualFile, handler: String): PsiElement? {
        val controller = AlloyRelated.related(vfile, AlloyKind.CONTROLLER) ?: return null
        val psi = PsiManager.getInstance(element.project).findFile(controller) ?: return null
        val text = psi.text
        val match = Regex("""\b(?:function\s+|${Regex.escape(handler)}\s*[:=]\s*function)""").find(text)
        val idx = when {
            text.contains("function $handler") -> text.indexOf("function $handler") + "function ".length
            match != null -> text.indexOf(handler)
            else -> return null
        }
        return psi.findElementAt(idx)
    }

    private fun i18nTarget(element: PsiElement, vfile: com.intellij.openapi.vfs.VirtualFile, key: String): PsiElement? {
        val app = AlloyRelated.appRoot(vfile) ?: return null
        val i18n = app.findChild("i18n") ?: return null
        for (langDir in i18n.children) {
            val strings = langDir.findChild("strings.xml") ?: continue
            val psi = PsiManager.getInstance(element.project).findFile(strings) ?: continue
            val idx = psi.text.indexOf("name=\"$key\"")
            if (idx >= 0) return psi.findElementAt(idx)
        }
        return null
    }
}
