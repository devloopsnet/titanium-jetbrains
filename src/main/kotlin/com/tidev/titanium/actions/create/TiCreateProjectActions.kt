package com.tidev.titanium.actions.create

import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.Messages
import com.tidev.titanium.cli.TiCli
import com.tidev.titanium.project.TiProjectType
import com.tidev.titanium.util.TiNotifications
import com.tidev.titanium.util.TiVfs

/** Base for `ti create`: collects parameters via a dialog and runs the CLI in the background. */
abstract class TiCreateProjectAction(private val type: TiProjectType) : AnAction(), DumbAware {

    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val defaultLocation = project.guessProjectDir()?.path ?: System.getProperty("user.home")
        val dialog = TiCreateProjectDialog(project, type, defaultLocation)
        if (!dialog.showAndGet()) return

        val typeArg = if (type == TiProjectType.APP) "app" else "module"
        val args = listOf(
            "create", "-t", typeArg,
            "-n", dialog.projectName,
            "--id", dialog.appId,
            "-p", dialog.platforms,
            "-d", dialog.location,
            "--force",
        )

        object : Task.Backgroundable(project, "Creating Titanium ${typeArg}", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                val result = try {
                    CapturingProcessHandler(TiCli.commandLine(args)).runProcess(180_000)
                } catch (t: Throwable) {
                    TiNotifications.error(project, "Could not launch `ti`. Is the Titanium CLI installed?")
                    return
                }
                if (result.exitCode == 0) {
                    TiVfs.refresh(dialog.location)
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showInfoMessage(
                            project,
                            "Created '${dialog.projectName}' at:\n${dialog.resultingProjectDir}\n\n" +
                                "Open it via File | Open to start building.",
                            "Titanium Project Created",
                        )
                    }
                } else {
                    TiNotifications.warn(project, "ti create failed (exit ${result.exitCode}). ${result.stderr.take(200)}")
                }
            }
        }.queue()
    }
}

class TiCreateAppAction : TiCreateProjectAction(TiProjectType.APP)
class TiCreateModuleAction : TiCreateProjectAction(TiProjectType.MODULE)
