package com.tidev.titanium.actions.sdk

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.tidev.titanium.TiIcons
import com.tidev.titanium.environment.TiEnvironmentService
import com.tidev.titanium.sdk.TiSdkManager
import com.tidev.titanium.util.TiNotifications

/** Install a Titanium SDK (pick from available releases, or type a version / "latest"). */
class TiInstallSdkAction : AnAction(), DumbAware {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT
    override fun update(e: AnActionEvent) { e.presentation.isEnabledAndVisible = e.project != null }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        object : Task.Backgroundable(project, "Fetching Titanium SDK releases", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                val releases = TiSdkManager.listReleases()
                ApplicationManager.getApplication().invokeLater {
                    val values = (listOf("latest") + releases).toTypedArray()
                    val choice = Messages.showEditableChooseDialog(
                        "Version to install:",
                        "Install Titanium SDK",
                        TiIcons.Titanium,
                        values,
                        values.firstOrNull() ?: "latest",
                        object : InputValidator {
                            override fun checkInput(input: String?) = !input.isNullOrBlank()
                            override fun canClose(input: String?) = checkInput(input)
                        },
                    )
                    if (!choice.isNullOrBlank()) TiSdkManager.install(project, choice.trim())
                }
            }
        }.queue()
    }
}

/** Select the active Titanium SDK from those installed. */
class TiSelectSdkAction : AnAction(), DumbAware {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT
    override fun update(e: AnActionEvent) { e.presentation.isEnabledAndVisible = e.project != null }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val installed = TiEnvironmentService.getInstance(project).environment.sdks.map { it.version }
        if (installed.isEmpty()) {
            TiNotifications.warn(project, "No Titanium SDKs are installed. Use Install SDK first.")
            return
        }
        val choice = Messages.showEditableChooseDialog(
            "Active SDK:",
            "Select Titanium SDK",
            TiIcons.Titanium,
            installed.toTypedArray(),
            installed.first(),
            null,
        )
        if (!choice.isNullOrBlank()) TiSdkManager.select(project, choice.trim())
    }
}

/** Check whether a newer Titanium SDK release is available. */
class TiCheckUpdatesAction : AnAction(), DumbAware {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT
    override fun update(e: AnActionEvent) { e.presentation.isEnabledAndVisible = e.project != null }
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { TiSdkManager.checkForUpdates(it) }
    }
}
