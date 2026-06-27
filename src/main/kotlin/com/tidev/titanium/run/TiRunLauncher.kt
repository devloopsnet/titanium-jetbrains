package com.tidev.titanium.run

import com.intellij.execution.Executor
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.project.Project
import com.tidev.titanium.cli.model.TiDevice
import com.tidev.titanium.cli.model.TiPlatform
import com.tidev.titanium.cli.model.TiTarget
import com.tidev.titanium.environment.TiEnvironmentService

/** Builds and runs Titanium configurations programmatically (tool window + toolbar buttons). */
object TiRunLauncher {

    private fun runExecutor(): Executor = DefaultRunExecutor.getRunExecutorInstance()

    /** Create (or reuse) a run config targeting [device] and execute it. */
    fun launchDevice(project: Project, device: TiDevice, executor: Executor = runExecutor()) {
        launch(project, "Titanium · ${device.display}", executor) { options ->
            options.platform = device.platform.cliName
            options.target = device.target.cliName
            options.deviceId = device.id
        }
    }

    /** Launch a plain platform/target build with no specific device. */
    fun launchTarget(
        project: Project,
        platform: TiPlatform,
        target: TiTarget,
        executor: Executor = runExecutor(),
    ) {
        launch(project, "Titanium · ${platform.label} ${target.label}", executor) { options ->
            options.platform = platform.cliName
            options.target = target.cliName
            options.deviceId = ""
        }
    }

    /**
     * Build/run using the toolbar's platform + device selection (resolved from `ti info`).
     * The device's target (simulator/emulator/device) is taken from the detected environment;
     * with no device chosen, a sensible default target for the platform is used.
     */
    fun launchSelection(project: Project, executor: Executor = runExecutor()) {
        val selection = TiRunSelection.getInstance(project)
        val env = TiEnvironmentService.getInstance(project).environment
        val device = selection.deviceId?.let { id -> env.devices.firstOrNull { it.id == id } }
        val target = device?.target ?: defaultTarget(selection.platform)
        val label = device?.display ?: "${selection.platform.label} ${target.label}"
        launch(project, "Titanium · $label", executor) { options ->
            options.platform = selection.platform.cliName
            options.target = target.cliName
            options.deviceId = device?.id ?: ""
        }
    }

    private fun defaultTarget(platform: TiPlatform): TiTarget =
        if (platform == TiPlatform.IOS) TiTarget.IOS_SIMULATOR else TiTarget.ANDROID_EMULATOR

    private fun launch(
        project: Project,
        name: String,
        executor: Executor,
        configure: (TiRunConfigurationOptions) -> Unit,
    ) {
        val type = ConfigurationTypeUtil.findConfigurationType(TiRunConfigurationType::class.java)
        val factory = type.configurationFactories.first()
        val runManager = RunManager.getInstance(project)

        val settings = runManager.createConfiguration(name, factory)
        val config = settings.configuration as TiRunConfiguration
        configure(config.tiOptions)

        runManager.addConfiguration(settings)
        runManager.selectedConfiguration = settings
        ProgramRunnerUtil.executeConfiguration(settings, executor)
    }
}
