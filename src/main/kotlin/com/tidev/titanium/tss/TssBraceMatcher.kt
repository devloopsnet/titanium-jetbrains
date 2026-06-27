package com.tidev.titanium.tss

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

/** Brace matching for `{}` and `[]` in TSS. */
class TssBraceMatcher : PairedBraceMatcher {
    override fun getPairs(): Array<BracePair> = PAIRS

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset

    private companion object {
        val PAIRS = arrayOf(
            BracePair(TssTokenTypes.LBRACE, TssTokenTypes.RBRACE, true),
            BracePair(TssTokenTypes.LBRACKET, TssTokenTypes.RBRACKET, false),
        )
    }
}
