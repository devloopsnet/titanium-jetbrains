package com.tidev.titanium.run

import com.intellij.execution.configurations.GeneralCommandLine
import com.tidev.titanium.cli.TiCli
import com.tidev.titanium.settings.TiSettings

/** Turns a run configuration's options into a `ti build` [GeneralCommandLine]. */
object TiBuildCommandBuilder {

    fun build(
        options: TiRunConfigurationOptions,
        projectDir: String,
        debugPort: Int? = null,
    ): GeneralCommandLine {
        val args = mutableListOf("build")

        options.platform?.takeIf { it.isNotBlank() }?.let { args += listOf("-p", it) }
        options.target?.takeIf { it.isNotBlank() }?.let { args += listOf("-T", it) }
        options.deviceId?.takeIf { it.isNotBlank() }?.let { args += listOf("-C", it) }
        options.sdkVersion?.takeIf { it.isNotBlank() }?.let { args += listOf("-s", it) }
        options.deployType?.takeIf { it.isNotBlank() }?.let { args += listOf("--deploy-type", it) }
        if (options.buildOnly) args += "--build-only"

        // LiveView: explicit flag from config, falling back to the global setting.
        if (options.liveView && TiSettings.getInstance().state.liveViewEnabled) args += "--liveview"

        // Debug: expose the runtime's Chrome DevTools endpoint for the debugger to attach to.
        debugPort?.let { args += listOf("--debug-host", "127.0.0.1:$it") }

        val logLevel = TiSettings.getInstance().state.logLevel
        if (logLevel.isNotBlank()) args += listOf("--log-level", logLevel)

        options.extraArgs?.takeIf { it.isNotBlank() }?.let { extra ->
            args += extra.trim().split(Regex("\\s+"))
        }

        // `commandLine` appends the non-interactive flags + --project-dir + working dir.
        return TiCli.commandLine(args, projectDir = projectDir)
    }

    /** A human-readable preview of the command (for the console header / tooltips). */
    fun preview(options: TiRunConfigurationOptions, projectDir: String): String =
        build(options, projectDir).commandLineString
}
