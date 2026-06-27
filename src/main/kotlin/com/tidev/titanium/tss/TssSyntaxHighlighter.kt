package com.tidev.titanium.tss

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import com.tidev.titanium.tss.lexer.TssLexer

/** Maps TSS token types to editor colors (with sensible fallbacks to language defaults). */
class TssSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer = TssLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> =
        when (tokenType) {
            TssTokenTypes.LINE_COMMENT, TssTokenTypes.BLOCK_COMMENT -> COMMENT
            TssTokenTypes.STRING -> STRING
            TssTokenTypes.NUMBER -> NUMBER
            TssTokenTypes.KEYWORD -> KEYWORD
            TssTokenTypes.IDENTIFIER -> IDENTIFIER
            TssTokenTypes.BRACE -> BRACES
            TssTokenTypes.BRACKET -> BRACKETS
            TssTokenTypes.OPERATOR -> OPERATOR
            TssTokenTypes.BAD_CHARACTER -> BAD_CHAR
            else -> EMPTY
        }

    private companion object {
        fun key(name: String, fallback: TextAttributesKey): Array<TextAttributesKey> =
            arrayOf(TextAttributesKey.createTextAttributesKey("TSS_$name", fallback))

        val COMMENT = key("COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        val STRING = key("STRING", DefaultLanguageHighlighterColors.STRING)
        val NUMBER = key("NUMBER", DefaultLanguageHighlighterColors.NUMBER)
        val KEYWORD = key("KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
        val IDENTIFIER = key("IDENTIFIER", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
        val BRACES = key("BRACES", DefaultLanguageHighlighterColors.BRACES)
        val BRACKETS = key("BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
        val OPERATOR = key("OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        val BAD_CHAR = arrayOf(com.intellij.openapi.editor.HighlighterColors.BAD_CHARACTER)
        val EMPTY = emptyArray<TextAttributesKey>()
    }
}

/** Registers [TssSyntaxHighlighter] for the TSS language / `.tss` files. */
class TssSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        TssSyntaxHighlighter()
}
