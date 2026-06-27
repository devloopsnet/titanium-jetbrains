package com.tidev.titanium.alloy.js

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.tidev.titanium.TiIcons

/**
 * Completion inside Alloy JS controllers: `$`, `Alloy`, `Ti`, `Titanium`, etc.
 *
 * OPTIONAL: this contributor is registered against the `JavaScript` language in
 * META-INF/titanium-javascript.xml, so it is loaded ONLY when the bundled JavaScript plugin
 * is present (WebStorm, IDEA Ultimate, …). On Community IDEs it never loads, and the rest of
 * the plugin is unaffected.
 *
 * Note: this class deliberately does NOT import com.intellij.lang.javascript.* — it only needs
 * the JS language to exist, which the optional descriptor guarantees. Anything that touches JS
 * PSI directly must also live under this `alloy.js` package and be wired only from the optional
 * descriptor.
 */
class AlloyControllerCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet,
                ) {
                    val path = parameters.originalFile.virtualFile?.path ?: return
                    if (!path.contains("/app/controllers/") &&
                        !path.contains("/app/lib/") &&
                        !path.contains("/app/widgets/") &&
                        !path.endsWith("/app/alloy.js")
                    ) return

                    GLOBALS.forEach { (name, type) ->
                        result.addElement(
                            LookupElementBuilder.create(name).withIcon(TiIcons.Titanium).withTypeText(type),
                        )
                    }
                }
            },
        )
    }

    private companion object {
        val GLOBALS = listOf(
            "$" to "Alloy controller scope",
            "Alloy" to "Alloy framework",
            "Ti" to "Titanium namespace",
            "Titanium" to "Titanium namespace",
            "arguments" to "controller args",
            "exports" to "module exports",
        )
    }
}
