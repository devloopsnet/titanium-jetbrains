package com.tidev.titanium.tss

import com.intellij.psi.tree.IElementType
import com.tidev.titanium.tss.lexer.TssLexer
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure unit test for the hand-written TSS lexer (no IDE fixture). */
class TssLexerTest {

    @Test
    fun lexesCommentsStringsNumbersBracesAndIdentifiers() {
        val text = """
            '#title': {
                // a line comment
                color: '#ffffff',
                width: 100,
                visible: true
            }
        """.trimIndent()

        val types = tokenize(text)

        assertTrue("line comment", types.contains(TssTokenTypes.LINE_COMMENT))
        assertTrue("string", types.contains(TssTokenTypes.STRING))
        assertTrue("number", types.contains(TssTokenTypes.NUMBER))
        assertTrue("keyword true", types.contains(TssTokenTypes.KEYWORD))
        assertTrue("identifier", types.contains(TssTokenTypes.IDENTIFIER))
        assertTrue("open brace", types.contains(TssTokenTypes.LBRACE))
        assertTrue("close brace", types.contains(TssTokenTypes.RBRACE))
    }

    @Test
    fun lexesBlockComment() {
        val types = tokenize("/* block */ width: 5")
        assertTrue(types.contains(TssTokenTypes.BLOCK_COMMENT))
        assertTrue(types.contains(TssTokenTypes.NUMBER))
    }

    private fun tokenize(text: String): List<IElementType> {
        val lexer = TssLexer()
        lexer.start(text, 0, text.length, 0)
        val out = mutableListOf<IElementType>()
        while (lexer.tokenType != null) {
            out += lexer.tokenType!!
            lexer.advance()
        }
        return out
    }
}
