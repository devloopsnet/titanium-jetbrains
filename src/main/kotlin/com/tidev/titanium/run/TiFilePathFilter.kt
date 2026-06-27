package com.tidev.titanium.run

import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem

/** Linkifies absolute `…/file.(js|xml|tss|json):line` paths in build/package output. */
class TiFilePathFilter(private val project: Project) : Filter {

    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        val items = mutableListOf<Filter.ResultItem>()
        val lineStart = entireLength - line.length
        for (match in PATTERN.findAll(line)) {
            val path = match.groupValues[1]
            val lineNo = match.groupValues[2].toIntOrNull() ?: continue
            val vf = LocalFileSystem.getInstance().findFileByPath(path) ?: continue
            val start = lineStart + match.range.first
            val end = lineStart + match.range.last + 1
            items += Filter.ResultItem(start, end, OpenFileHyperlinkInfo(project, vf, lineNo - 1))
        }
        return if (items.isEmpty()) null else Filter.Result(items)
    }

    private companion object {
        val PATTERN = Regex("""(/[^\s:]+\.(?:js|xml|tss|json)):(\d+)""")
    }
}
