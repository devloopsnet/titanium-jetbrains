package com.tidev.titanium.tss

import com.intellij.lang.Commenter

/** Enables line (`//`) and block (`/* */`) commenting in TSS (Ctrl+/ and Ctrl+Shift+/). */
class TssCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "//"
    override fun getBlockCommentPrefix(): String = "/*"
    override fun getBlockCommentSuffix(): String = "*/"
    override fun getCommentedBlockCommentPrefix(): String? = null
    override fun getCommentedBlockCommentSuffix(): String? = null
}
