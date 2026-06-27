package com.tidev.titanium.alloy

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.tidev.titanium.environment.TiApiMetadata

/**
 * Quick-doc (Ctrl+Q) / hover for Alloy view tags, attributes and TSS properties, sourced from the
 * installed SDK's `api.jsca` descriptions. Returns null for unknown names so other providers run.
 */
class AlloyDocumentationProvider : AbstractDocumentationProvider() {

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int,
    ): PsiElement? = contextElement

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val target = originalElement ?: element ?: return null
        val name = target.text?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val meta = TiApiMetadata.getInstance(target.project)
        val description = meta.descriptions[name]
            ?: meta.descriptions[name.removePrefix("on").replaceFirstChar { it.lowercase() }]
            ?: return null
        return "<b>$name</b><br/><br/>$description"
    }
}
