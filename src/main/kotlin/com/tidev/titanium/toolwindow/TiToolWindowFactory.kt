package com.tidev.titanium.toolwindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

/** Registers the Titanium build-explorer tool window. */
class TiToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = TiExplorerPanel(project, toolWindow.disposable)
        val content = toolWindow.contentManager.factory.createContent(panel, null, false)
        content.isCloseable = false
        toolWindow.contentManager.addContent(content)
    }
}
