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

/**
 * Completion inside Alloy XML views: Titanium UI element tags and common attributes.
 *
 * UNIVERSAL: registered against the XML language, which is available in every IntelliJ IDE,
 * so this needs no JavaScript plugin and works on Community editions.
 *
 * Scoped to Alloy view files (under app/views or app/widgets) to avoid polluting plain XML.
 */
class AlloyViewCompletionContributor : CompletionContributor() {

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
                    val path = parameters.originalFile.virtualFile?.path ?: return
                    if (!path.contains("/app/views/") && !path.contains("/app/widgets/")) return

                    UI_ELEMENTS.forEach {
                        result.addElement(
                            LookupElementBuilder.create(it).withIcon(TiIcons.Titanium).withTypeText("Ti.UI"),
                        )
                    }
                    COMMON_ATTRS.forEach {
                        result.addElement(LookupElementBuilder.create(it).withTypeText("attribute"))
                    }
                }
            },
        )
    }

    private companion object {
        val UI_ELEMENTS = listOf(
            "Alloy", "Window", "View", "ScrollView", "ScrollableView", "Label", "Button",
            "TextField", "TextArea", "ImageView", "Switch", "Slider", "ActivityIndicator",
            "TableView", "TableViewRow", "TableViewSection", "ListView", "ListSection", "ListItem",
            "Tab", "TabGroup", "NavigationWindow", "Toolbar", "WebView", "Picker", "ProgressBar",
            "Require", "Widget", "Menu", "MenuItem",
        )
        val COMMON_ATTRS = listOf(
            "id", "class", "module", "platform", "top", "left", "right", "bottom",
            "width", "height", "text", "title", "color", "backgroundColor", "layout",
            "onClick", "onLoad", "visible", "opacity", "image", "font",
        )
    }
}
