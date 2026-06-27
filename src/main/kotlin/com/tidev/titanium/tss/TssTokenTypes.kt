package com.tidev.titanium.tss

import com.intellij.psi.tree.IElementType

/** Token type for the TSS language. */
class TssTokenType(debugName: String) : IElementType(debugName, TssLanguage)

/** All TSS token types produced by [com.tidev.titanium.tss.lexer.TssLexer]. */
object TssTokenTypes {
    @JvmField val LINE_COMMENT = TssTokenType("TSS_LINE_COMMENT")
    @JvmField val BLOCK_COMMENT = TssTokenType("TSS_BLOCK_COMMENT")
    @JvmField val STRING = TssTokenType("TSS_STRING")
    @JvmField val NUMBER = TssTokenType("TSS_NUMBER")
    @JvmField val KEYWORD = TssTokenType("TSS_KEYWORD")
    @JvmField val IDENTIFIER = TssTokenType("TSS_IDENTIFIER")
    @JvmField val BRACE = TssTokenType("TSS_BRACE")
    @JvmField val BRACKET = TssTokenType("TSS_BRACKET")
    @JvmField val OPERATOR = TssTokenType("TSS_OPERATOR")
    @JvmField val BAD_CHARACTER = TssTokenType("TSS_BAD_CHARACTER")
}
