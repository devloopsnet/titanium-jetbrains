package com.tidev.titanium.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.filters.UrlFilter
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.configurations.CommandLineState
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.tidev.titanium.project.TiProjectService

/** A Titanium build/run configuration. State lives in [TiRunConfigurationOptions]. */
class TiRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    RunConfigurationBase<TiRunConfigurationOptions>(project, factory, name) {

    public override fun getOptions(): TiRunConfigurationOptions =
        super.getOptions() as TiRunConfigurationOptions

    val tiOptions: TiRunConfigurationOptions get() = options

    /** Resolve the project dir: explicit option, else the discovered primary project. */
    fun resolveProjectDir(): String? =
        tiOptions.projectDir?.takeIf { it.isNotBlank() }
            ?: TiProjectService.getInstance(project).primary()?.path

    override fun getConfigurationEditor(): SettingsEditor<out RunConfigurationBase<*>> =
        TiRunConfigurationEditor(project)

    @Throws(RuntimeConfigurationException::class)
    override fun checkConfiguration() {
        if (tiOptions.platform.isNullOrBlank()) {
            throw RuntimeConfigurationError("Select a platform (iOS or Android).")
        }
        if (resolveProjectDir() == null) {
            throw RuntimeConfigurationError("No Titanium project (tiapp.xml) found. Set a project directory.")
        }
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): CommandLineState =
        createState(environment, debugPort = null)

    /** Build state that also opens the runtime debugger endpoint on [debugPort]. */
    fun createDebugState(environment: ExecutionEnvironment, debugPort: Int): CommandLineState =
        createState(environment, debugPort)

    private fun createState(environment: ExecutionEnvironment, debugPort: Int?): CommandLineState {
        val projectDir = resolveProjectDir()
            ?: throw RuntimeConfigurationError("No Titanium project directory.")
        return object : CommandLineState(environment) {
            init {
                addConsoleFilters(UrlFilter(), TiFilePathFilter(project))
            }

            override fun startProcess(): ProcessHandler {
                val cmd: GeneralCommandLine = TiBuildCommandBuilder.build(tiOptions, projectDir, debugPort)
                val handler = KillableColoredProcessHandler(cmd)
                ProcessTerminatedListener.attach(handler)
                return handler
            }
        }
    }
}
