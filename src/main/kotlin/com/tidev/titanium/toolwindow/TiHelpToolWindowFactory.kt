package com.tidev.titanium.toolwindow

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.HyperlinkLabel
import com.intellij.util.ui.JBUI
import com.tidev.titanium.sdk.TiSdkManager
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

/** A small "Help & Feedback" tool window: documentation links + an update check. */
class TiHelpToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(10)
        }

        LINKS.forEach { (text, url) ->
            panel.add(HyperlinkLabel(text).apply { addHyperlinkListener { BrowserUtil.browse(url) } })
            panel.add(javax.swing.Box.createVerticalStrut(6))
        }

        panel.add(javax.swing.Box.createVerticalStrut(10))
        panel.add(JButton("Check for Updates").apply {
            addActionListener { TiSdkManager.checkForUpdates(project) }
        })

        val content = toolWindow.contentManager.factory.createContent(panel, null, false)
        content.isCloseable = false
        toolWindow.contentManager.addContent(content)
    }

    private companion object {
        val LINKS = listOf(
            "Titanium SDK documentation" to "https://titaniumsdk.com",
            "Alloy framework guide" to "https://titaniumsdk.com/guide/Alloy_Framework/",
            "API reference" to "https://titaniumsdk.com/api/",
            "TiDev on GitHub" to "https://github.com/tidev",
            "Community Slack" to "https://tidev.slack.com",
        )
    }
}
