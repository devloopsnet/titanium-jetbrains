package com.tidev.titanium.actions.pkg

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.tidev.titanium.environment.TiEnvironmentService
import com.tidev.titanium.project.TiProjectService
import com.tidev.titanium.run.TiPackageCommandBuilder
import com.tidev.titanium.run.TiPackager
import com.tidev.titanium.run.TiRunSelection
import com.tidev.titanium.util.TiNotifications

/** Distribution build (App Store / Ad Hoc / Play Store) via a guided dialog. */
class TiPackageAction : AnAction(), DumbAware {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled = project != null && TiProjectService.getInstance(project).primary() != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val tiProject = TiProjectService.getInstance(project).primary() ?: run {
            TiNotifications.warn(project, "No Titanium project (tiapp.xml) found in this workspace.")
            return
        }
        val env = TiEnvironmentService.getInstance(project).environment
        val initialPlatform = TiRunSelection.getInstance(project).platform

        val dialog = TiPackageDialog(project, env, tiProject.path, initialPlatform)
        if (!dialog.showAndGet()) return

        val command = TiPackageCommandBuilder.build(dialog.result(), tiProject.path)
        TiPackager.run(project, command)
    }
}
