package com.tidev.titanium.tss

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.tidev.titanium.TiIcons
import javax.swing.Icon

/** The Alloy TSS (Titanium Style Sheets) language. CSS-like; full parser is a later phase. */
object TssLanguage : Language("TSS") {
    private fun readResolve(): Any = TssLanguage
    override fun getDisplayName(): String = "Alloy Style (TSS)"
}

/** `.tss` file type. Universal — needs no JavaScript plugin. Registered via the fileType EP. */
class TssFileType private constructor() : LanguageFileType(TssLanguage) {
    override fun getName(): String = "Alloy Style (TSS)"
    override fun getDescription(): String = "Alloy TSS stylesheet"
    override fun getDefaultExtension(): String = "tss"
    override fun getIcon(): Icon = TiIcons.Titanium

    companion object {
        @JvmField
        val INSTANCE = TssFileType()
    }
}
