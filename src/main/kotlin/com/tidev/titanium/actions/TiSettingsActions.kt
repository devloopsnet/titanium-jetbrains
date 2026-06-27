package com.tidev.titanium.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.tidev.titanium.settings.TiSettings

/** Toggle the default LiveView flag (passed as `--liveview` to dev builds). */
class TiLiveViewToggleAction : ToggleAction(), DumbAware {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun isSelected(e: AnActionEvent): Boolean = TiSettings.getInstance().state.liveViewEnabled
    override fun setSelected(e: AnActionEvent, state: Boolean) {
        TiSettings.getInstance().state.liveViewEnabled = state
    }
}

/** Quick-pick the CLI log level (`--log-level`). */
class TiSetLogLevelAction : AnAction(), DumbAware {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        val levels = arrayOf("trace", "debug", "info", "warn", "error")
        val settings = TiSettings.getInstance().state
        val choice = Messages.showEditableChooseDialog(
            "Log level:",
            "Titanium Log Level",
            null,
            levels,
            settings.logLevel,
            null,
        )
        if (!choice.isNullOrBlank()) settings.logLevel = choice.trim()
    }
}
