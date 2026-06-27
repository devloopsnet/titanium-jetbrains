package com.tidev.titanium.actions.sdk

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.tidev.titanium.run.TiPackager
import com.tidev.titanium.settings.TiSettings
import java.io.File

/** Base for `npm install -g <pkg>` tooling updates, shown in a console. */
abstract class TiNpmUpdateAction(private val pkg: String, private val title: String) : AnAction(), DumbAware {

    override fun getActionUpdateThread() = ActionUpdateThread.EDT
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val cmd = GeneralCommandLine("npm", "install", "-g", pkg)
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
        val nodePath = TiSettings.getInstance().state.nodePath
        if (nodePath.isNotBlank()) {
            cmd.withEnvironment("PATH", nodePath + File.pathSeparator + System.getenv("PATH").orEmpty())
        }
        TiPackager.run(project, cmd, title)
    }
}

class TiUpdateCliAction : TiNpmUpdateAction("titanium", "Update Titanium CLI")
class TiUpdateAlloyAction : TiNpmUpdateAction("alloy", "Update Alloy CLI")
