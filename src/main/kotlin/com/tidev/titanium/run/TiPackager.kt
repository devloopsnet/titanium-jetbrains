package com.tidev.titanium.run

import com.intellij.execution.RunContentExecutor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.filters.UrlFilter
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.openapi.project.Project

/** Runs a packaging build and shows its streamed output in the Run tool window. */
object TiPackager {

    fun run(project: Project, command: GeneralCommandLine, title: String = "Titanium Package") {
        val handler = KillableColoredProcessHandler(command)
        ProcessTerminatedListener.attach(handler)
        RunContentExecutor(project, handler)
            .withTitle(title)
            .withActivateToolWindow(true)
            .withFilter(UrlFilter())
            .withFilter(TiFilePathFilter(project))
            .withStop({ handler.destroyProcess() }, { !handler.isProcessTerminated })
            .run()
    }
}
