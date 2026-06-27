package com.tidev.titanium.alloy

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.xml.XmlTokenType
import com.intellij.util.ProcessingContext
import com.tidev.titanium.TiIcons

/** Completion for `tiapp.xml` element names. Universal (XML). */
class AlloyTiappCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(XmlTokenType.XML_NAME),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet,
                ) {
                    if (parameters.originalFile.name != "tiapp.xml") return
                    TIAPP_TAGS.forEach {
                        result.addElement(
                            LookupElementBuilder.create(it).withIcon(TiIcons.Titanium).withTypeText("tiapp"),
                        )
                    }
                }
            },
        )
    }

    private companion object {
        val TIAPP_TAGS = listOf(
            "id", "name", "version", "publisher", "url", "description", "copyright",
            "icon", "guid", "sdk-version", "persistent-wifi", "prerendered-icon",
            "statusbar-style", "statusbar-hidden", "fullscreen", "navbar-hidden",
            "analytics", "deployment-targets", "target", "property",
            "ios", "android", "modules", "module", "plugins", "plugin",
            "min-ios-ver", "manifest", "application", "activity", "service", "uses-sdk",
            "tool", "transpile", "sourcemap",
        )
    }
}
