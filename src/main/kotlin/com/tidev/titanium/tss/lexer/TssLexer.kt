package com.tidev.titanium.tss.lexer

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.tidev.titanium.tss.TssTokenTypes

/**
 * Hand-written lexer for Alloy TSS (a JSON/CSS-like style dialect). Sufficient for syntax
 * highlighting without a full grammar: it recognizes comments, strings, numbers, keywords,
 * identifiers, braces/brackets and punctuation.
 */
class TssLexer : LexerBase() {

    private var buffer: CharSequence = ""
    private var endOffset = 0
    private var tokenStart = 0
    private var tokenEnd = 0
    private var tokenType: IElementType? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.endOffset = endOffset
        this.tokenStart = startOffset
        this.tokenEnd = startOffset
        advance()
    }

    override fun getState(): Int = 0
    override fun getTokenType(): IElementType? = tokenType
    override fun getTokenStart(): Int = tokenStart
    override fun getTokenEnd(): Int = tokenEnd
    override fun getBufferSequence(): CharSequence = buffer
    override fun getBufferEnd(): Int = endOffset

    override fun advance() {
        tokenStart = tokenEnd
        if (tokenStart >= endOffset) {
            tokenType = null
            return
        }
        val c = buffer[tokenStart]
        when {
            c.isWhitespace() -> consumeWhile(TokenType.WHITE_SPACE) { it.isWhitespace() }
            c == '/' && peek(1) == '/' -> consumeLineComment()
            c == '/' && peek(1) == '*' -> consumeBlockComment()
            c == '"' || c == '\'' -> consumeString(c)
            c.isDigit() || (c == '-' && peek(1)?.isDigit() == true) -> consumeNumber()
            isIdentStart(c) -> consumeIdentifier()
            c == '{' || c == '}' -> single(TssTokenTypes.BRACE)
            c == '[' || c == ']' -> single(TssTokenTypes.BRACKET)
            c == ':' || c == ',' || c == ';' || c == '.' || c == '#' -> single(TssTokenTypes.OPERATOR)
            else -> single(TssTokenTypes.BAD_CHARACTER)
        }
    }

    private fun peek(ahead: Int): Char? =
        (tokenStart + ahead).takeIf { it < endOffset }?.let { buffer[it] }

    private fun single(type: IElementType) {
        tokenType = type
        tokenEnd = tokenStart + 1
    }

    private inline fun consumeWhile(type: IElementType, predicate: (Char) -> Boolean) {
        var i = tokenStart
        while (i < endOffset && predicate(buffer[i])) i++
        tokenType = type
        tokenEnd = i
    }

    private fun consumeLineComment() {
        var i = tokenStart + 2
        while (i < endOffset && buffer[i] != '\n') i++
        tokenType = TssTokenTypes.LINE_COMMENT
        tokenEnd = i
    }

    private fun consumeBlockComment() {
        var i = tokenStart + 2
        while (i < endOffset) {
            if (buffer[i] == '*' && i + 1 < endOffset && buffer[i + 1] == '/') {
                i += 2
                break
            }
            i++
        }
        tokenType = TssTokenTypes.BLOCK_COMMENT
        tokenEnd = i
    }

    private fun consumeString(quote: Char) {
        var i = tokenStart + 1
        while (i < endOffset) {
            val ch = buffer[i]
            if (ch == '\\') { i += 2; continue }
            if (ch == quote || ch == '\n') { if (ch == quote) i++; break }
            i++
        }
        tokenType = TssTokenTypes.STRING
        tokenEnd = i
    }

    private fun consumeNumber() {
        var i = tokenStart
        if (buffer[i] == '-') i++
        while (i < endOffset && (buffer[i].isDigit() || buffer[i] == '.' || buffer[i] == 'x' ||
                buffer[i] in 'a'..'f' || buffer[i] in 'A'..'F' || buffer[i] == '%')
        ) i++
        tokenType = TssTokenTypes.NUMBER
        tokenEnd = i
    }

    private fun consumeIdentifier() {
        var i = tokenStart
        while (i < endOffset && isIdentPart(buffer[i])) i++
        val text = buffer.subSequence(tokenStart, i).toString()
        tokenType = if (text in KEYWORDS) TssTokenTypes.KEYWORD else TssTokenTypes.IDENTIFIER
        tokenEnd = i
    }

    private fun isIdentStart(c: Char) = c.isLetter() || c == '_' || c == '$'
    private fun isIdentPart(c: Char) = c.isLetterOrDigit() || c == '_' || c == '$' || c == '.'

    private companion object {
        val KEYWORDS = setOf("true", "false", "null")
    }
}
