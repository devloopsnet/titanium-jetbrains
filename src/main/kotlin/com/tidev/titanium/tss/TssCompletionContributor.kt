package com.tidev.titanium.tss

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.tidev.titanium.TiIcons
import com.tidev.titanium.environment.TiApiMetadata

/** Basic property/value completion inside `.tss` files. Universal (no JS plugin needed). */
class TssCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().inFile(PlatformPatterns.psiFile(TssFile::class.java)),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet,
                ) {
                    val meta = TiApiMetadata.getInstance(parameters.position.project)
                    val props = if (meta.properties.isNotEmpty()) meta.properties + PROPERTIES else PROPERTIES
                    props.forEach {
                        result.addElement(LookupElementBuilder.create(it).withIcon(TiIcons.Titanium).withTypeText("property"))
                    }
                    CONSTANTS.forEach {
                        result.addElement(LookupElementBuilder.create(it).withTypeText("Ti.UI"))
                    }
                }
            },
        )
    }

    private companion object {
        val PROPERTIES = listOf(
            "top", "left", "right", "bottom", "width", "height", "color", "backgroundColor",
            "text", "title", "font", "textAlign", "borderRadius", "borderColor", "borderWidth",
            "layout", "opacity", "visible", "zIndex", "image", "tintColor",
        )
        val CONSTANTS = listOf(
            "Ti.UI.FILL", "Ti.UI.SIZE", "Ti.UI.TEXT_ALIGNMENT_CENTER", "Ti.UI.TEXT_ALIGNMENT_LEFT",
            "Ti.UI.TEXT_ALIGNMENT_RIGHT",
        )
    }
}
