package com.tidev.titanium.actions

import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.tidev.titanium.environment.TiEnvironmentService
import com.tidev.titanium.project.TiProjectService
import com.tidev.titanium.run.TiRunLauncher
import com.tidev.titanium.util.TiNotifications

/** Shared enable rule: there must be a Titanium project in the workspace. */
private fun hasTiProject(e: AnActionEvent): Boolean {
    val project = e.project ?: return false
    return TiProjectService.getInstance(project).primary() != null
}

/** Toolbar/menu: build & run the selected (or a default) Titanium configuration. */
class TiBuildAction : AnAction(), DumbAware {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = hasTiProject(e)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        if (!hasTiProject(e)) {
            TiNotifications.warn(project, "No Titanium project (tiapp.xml) found in this workspace.")
            return
        }
        TiRunLauncher.launchSelection(project, DefaultRunExecutor.getRunExecutorInstance())
    }
}

/** Toolbar/menu: debug the selected (or a default) Titanium configuration. */
class TiDebugAction : AnAction(), DumbAware {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = hasTiProject(e)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        if (!hasTiProject(e)) {
            TiNotifications.warn(project, "No Titanium project (tiapp.xml) found in this workspace.")
            return
        }
        TiRunLauncher.launchSelection(project, DefaultDebugExecutor.getDebugExecutorInstance())
    }
}

/** Toolbar/menu: re-run `ti info` to refresh detected SDKs, simulators and devices. */
class TiRefreshEnvironmentAction : AnAction(), DumbAware {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled = project != null && !TiEnvironmentService.getInstance(project).isLoading
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { TiEnvironmentService.getInstance(it).refresh(notify = true) }
    }
}
