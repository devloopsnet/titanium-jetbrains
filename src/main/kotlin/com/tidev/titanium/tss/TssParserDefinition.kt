package com.tidev.titanium.tss

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.tidev.titanium.tss.lexer.TssLexer

/** PSI file node for `.tss` documents. */
class TssFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, TssLanguage) {
    override fun getFileType(): FileType = TssFileType.INSTANCE
    override fun toString(): String = "TSS File"
}

/**
 * Minimal flat parser: every lexer token becomes a leaf under the file node. This is enough to
 * give TSS a real PSI tree so completion contributors and line markers fire — a structured
 * grammar (selectors/blocks/properties) is a later enhancement.
 */
class TssParserDefinition : ParserDefinition {
    override fun createLexer(project: com.intellij.openapi.project.Project?): Lexer = TssLexer()

    override fun createParser(project: com.intellij.openapi.project.Project?): PsiParser = PsiParser { root, builder ->
        val mark = builder.mark()
        while (!builder.eof()) builder.advanceLexer()
        mark.done(root)
        builder.treeBuilt
    }

    override fun getFileNodeType(): IFileElementType = FILE
    override fun getCommentTokens(): TokenSet = COMMENTS
    override fun getStringLiteralElements(): TokenSet = STRINGS
    override fun getWhitespaceTokens(): TokenSet = WHITESPACE

    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = TssFile(viewProvider)

    private companion object {
        val FILE = IFileElementType(TssLanguage)
        val WHITESPACE: TokenSet = TokenSet.create(TokenType.WHITE_SPACE)
        val COMMENTS: TokenSet = TokenSet.create(TssTokenTypes.LINE_COMMENT, TssTokenTypes.BLOCK_COMMENT)
        val STRINGS: TokenSet = TokenSet.create(TssTokenTypes.STRING)
    }
}
