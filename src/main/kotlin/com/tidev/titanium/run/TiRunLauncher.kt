package com.tidev.titanium.run

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.project.Project
import com.tidev.titanium.cli.model.TiDevice
import com.tidev.titanium.cli.model.TiPlatform
import com.tidev.titanium.cli.model.TiTarget

/** Builds and runs Titanium configurations programmatically (used by the tool window). */
object TiRunLauncher {

    /** Create (or reuse) a run config targeting [device] and execute it with the Run executor. */
    fun launchDevice(project: Project, device: TiDevice) {
        launch(project, "Titanium · ${device.display}") { options ->
            options.platform = device.platform.cliName
            options.target = device.target.cliName
            options.deviceId = device.id
        }
    }

    /** Launch a plain platform/target build with no specific device. */
    fun launchTarget(project: Project, platform: TiPlatform, target: TiTarget) {
        launch(project, "Titanium · ${platform.label} ${target.label}") { options ->
            options.platform = platform.cliName
            options.target = target.cliName
            options.deviceId = ""
        }
    }

    private fun launch(project: Project, name: String, configure: (TiRunConfigurationOptions) -> Unit) {
        val type = ConfigurationTypeUtil.findConfigurationType(TiRunConfigurationType::class.java)
        val factory = type.configurationFactories.first()
        val runManager = RunManager.getInstance(project)

        val settings = runManager.createConfiguration(name, factory)
        val config = settings.configuration as TiRunConfiguration
        configure(config.tiOptions)

        runManager.addConfiguration(settings)
        runManager.selectedConfiguration = settings
        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance())
    }
}
