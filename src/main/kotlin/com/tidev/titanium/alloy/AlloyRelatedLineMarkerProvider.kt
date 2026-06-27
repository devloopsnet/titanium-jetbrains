package com.tidev.titanium.alloy

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.tidev.titanium.TiIcons

/**
 * Adds a gutter icon to Alloy controllers/views/styles that navigates to the related files.
 * Registered for XML (views) in the core descriptor and for JavaScript (controllers) in the
 * optional descriptor. The provider itself touches no JS PSI, so it is safe in both.
 */
class AlloyRelatedLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun getName(): String = "Alloy related files"

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    ) {
        val file = element.containingFile ?: return
        val vfile = file.virtualFile ?: return
        if (AlloyKind.of(vfile) == null) return
        // Mark once, on the first leaf, to avoid duplicate gutter icons.
        if (element != PsiTreeUtil.getDeepestFirst(file)) return

        val psiManager = PsiManager.getInstance(element.project)
        val targets = AlloyRelated.allRelated(vfile).mapNotNull { psiManager.findFile(it) }
        if (targets.isEmpty()) return

        val info = NavigationGutterIconBuilder.create(TiIcons.Titanium)
            .setTargets(targets)
            .setTooltipText("Go to related Alloy file")
            .createLineMarkerInfo(element)
        result.add(info)
    }
}
