package com.tidev.titanium.debug

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XLineBreakpointType

/**
 * Line breakpoints for Titanium/Alloy JavaScript controllers. We register our own type (rather
 * than depend on the JS plugin's) so breakpoints work on every IDE; where the JS plugin is
 * present, its breakpoints coexist.
 */
class TiDebugBreakpointType :
    XLineBreakpointType<XBreakpointProperties<*>>(ID, "Titanium (Alloy) Breakpoints") {

    override fun createBreakpointProperties(file: VirtualFile, line: Int): XBreakpointProperties<*>? = null

    override fun canPutAt(file: VirtualFile, line: Int, project: Project): Boolean {
        if (file.extension != "js") return false
        val path = file.path
        return path.contains("/app/controllers/") || path.contains("/app/lib/") ||
            path.contains("/app/widgets/") || path.endsWith("/app/alloy.js")
    }

    companion object {
        const val ID = "titanium-line"
    }
}
