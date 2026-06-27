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
     * Build an `alloy generate <component> <positional…> -o <appDir>` command.
     *
     * [positional] is the full positional tail after the component, e.g. `["index"]` for a
     * controller, or `["user", "sql", "name:string", "age:number"]` for a model. Positional args
     * are kept contiguous (before the `-o` option) so the CLI parses them correctly.
     */
    fun generate(
        component: Component,
        positional: List<String>,
        appDir: String,
        platform: String? = null,
    ): GeneralCommandLine {
        val exe = settings().alloyPath.ifBlank { "alloy" }
        val cmd = GeneralCommandLine(exe, "generate", component.cliName)
        cmd.addParameters(positional)
        cmd.addParameters("-o", appDir, "--no-colors")
        platform?.let { cmd.addParameters("--platform", it) }
        cmd.withWorkDirectory(File(appDir).parentFile ?: File(appDir))
        cmd.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
        if (settings().nodePath.isNotBlank()) {
            val existing = System.getenv("PATH").orEmpty()
            cmd.withEnvironment("PATH", settings().nodePath + File.pathSeparator + existing)
        }
        return cmd
    }
}
