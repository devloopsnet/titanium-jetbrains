package com.tidev.titanium.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.tidev.titanium.cli.TiCli
import com.tidev.titanium.cli.TiCliException
import com.tidev.titanium.project.TiProjectService
import com.tidev.titanium.util.TiNotifications

/** Runs `ti clean` for the current Titanium project. */
class TiCleanAction : AnAction(), DumbAware {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled =
            project != null && TiProjectService.getInstance(project).primary() != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val tiProject = TiProjectService.getInstance(project).primary() ?: return

        object : Task.Backgroundable(project, "Cleaning Titanium project", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                try {
                    val result = TiCli.run(listOf("clean"), projectDir = tiProject.path, timeoutMs = 120_000)
                    if (result.success) {
                        TiNotifications.info(project, "Titanium project cleaned: ${tiProject.display}")
                    } else {
                        TiNotifications.warn(project, "Clean failed (exit ${result.exitCode}). See logs.")
                    }
                } catch (ex: TiCliException) {
                    TiNotifications.error(project, ex.message ?: "Titanium CLI not found")
                }
            }
        }.queue()
    }
}
