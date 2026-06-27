package com.tidev.titanium.cli

import com.intellij.execution.configurations.GeneralCommandLine
import com.tidev.titanium.settings.TiSettings
import java.io.File

/**
 * Wrapper around the separate `alloy` binary used for Alloy code generation
 * (`alloy generate controller|view|style|model|migration|widget …`).
 */
object AlloyCli {

    /** Alloy component generators surfaced as IDE actions. */
    enum class Component(val cliName: String, val label: String) {
        CONTROLLER("controller", "Controller"),
        VIEW("view", "View"),
        STYLE("style", "Style"),
        MODEL("model", "Model"),
        MIGRATION("migration", "Migration"),
        WIDGET("widget", "Widget"),
    }

    private fun settings() = TiSettings.getInstance().state

    /**
     * Build an `alloy generate <component> <name>` command. [appDir] should point at the
     * project's `app/` directory (Alloy's `-o`/--outputPath).
     */
    fun generate(
        component: Component,
        name: String,
        appDir: String,
        platform: String? = null,
        extraArgs: List<String> = emptyList(),
    ): GeneralCommandLine {
        val exe = settings().alloyPath.ifBlank { "alloy" }
        val cmd = GeneralCommandLine(exe, "generate", component.cliName, name)
        cmd.addParameters("-o", appDir, "--no-colors")
        platform?.let { cmd.addParameters("--platform", it) }
        cmd.addParameters(extraArgs)
        cmd.withWorkDirectory(File(appDir).parentFile ?: File(appDir))
        cmd.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
        return cmd
    }
}
