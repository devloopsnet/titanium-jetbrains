package com.tidev.titanium.core

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.tidev.titanium.environment.TiEnvironmentService
import com.tidev.titanium.project.TiProjectService

/**
 * Runs once after a project opens. If the workspace contains a Titanium project, kick off an
 * initial environment detection so pickers and the tool window are populated.
 */
class TiStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val hasProject = TiProjectService.getInstance(project).primary() != null
        if (hasProject) {
            TiEnvironmentService.getInstance(project).refresh()
        }
    }
}
