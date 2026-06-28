package com.tidev.titanium.cli

import com.google.gson.JsonParser
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.diagnostic.logger
import com.tidev.titanium.settings.TiSettings
import java.io.File

/**
 * Thin wrapper around the Titanium CLI (`ti`). Stateless: every call reads the current
 * [TiSettings].
 *
 * The modern (v8) CLI rejects unknown global flags, so we keep invocations minimal: bare
 * `ti -v`, `ti info --output json`, and `ti build <options>` run in the project directory
 * (no `--project-dir`/`--no-*` flags, which v8 doesn't accept).
 */
object TiCli {
    private val LOG = logger<TiCli>()

    private fun settings() = TiSettings.getInstance().state

    /**
     * Build a [GeneralCommandLine] for an arbitrary `ti` invocation. [projectDir] sets the working
     * directory so the CLI operates on the right project (we use cwd rather than a `--project-dir`
     * flag for cross-version compatibility).
     */
    fun commandLine(args: List<String>, projectDir: String? = null): GeneralCommandLine {
        val s = settings()
        val exe = s.cliPath.ifBlank { "ti" }
        val cmd = GeneralCommandLine(exe)
        cmd.addParameters(args)
        projectDir?.let { cmd.withWorkDirectory(File(it)) }
        // Inherit the user's shell PATH so a node-installed `ti` resolves even when the IDE was
        // launched from the Dock with a stripped PATH; honor an explicit node dir too.
        cmd.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
        if (s.nodePath.isNotBlank()) {
            val existing = System.getenv("PATH").orEmpty()
            cmd.withEnvironment("PATH", s.nodePath + File.pathSeparator + existing)
        }
        return cmd
    }

    /** Run a command synchronously and capture its output. Never throws on non-zero exit. */
    fun run(args: List<String>, projectDir: String? = null, timeoutMs: Int = 60_000): TiCliResult {
        val cmd = commandLine(args, projectDir)
        return try {
            val handler = CapturingProcessHandler(cmd)
            val output = handler.runProcess(timeoutMs)
            TiCliResult(output.exitCode, output.stdout, output.stderr, output.isTimeout)
        } catch (t: Throwable) {
            LOG.warn("ti ${args.joinToString(" ")} failed to launch", t)
            throw TiCliException("Could not launch '${cmd.exePath}'. Is the Titanium CLI installed and on PATH?", t)
        }
    }

    /** Run a command that supports `--output json` and return the parsed JSON (or null). */
    fun runJson(args: List<String>, projectDir: String? = null, timeoutMs: Int = 60_000): com.google.gson.JsonElement? {
        val result = run(args + listOf("--output", "json"), projectDir, timeoutMs)
        if (!result.success) {
            LOG.info("ti ${args.joinToString(" ")} exited ${result.exitCode}: ${result.stderr.take(500)}")
        }
        val text = result.stdout.trim()
        if (text.isEmpty()) return null
        return try {
            JsonParser.parseString(extractJson(text))
        } catch (e: Exception) {
            LOG.warn("Failed to parse JSON from ti ${args.joinToString(" ")}", e)
            null
        }
    }

    /** Return the installed CLI version (`ti -v`), or null if it cannot be determined. */
    fun version(): String? = try {
        run(listOf("-v"), timeoutMs = 15_000).takeIf { it.success }?.stdout?.trim()?.ifBlank { null }
    } catch (e: TiCliException) {
        null
    }

    fun isAvailable(): Boolean = version() != null

    /** Trim anything before the first '{' or '[' in case the CLI prefixes a stray line. */
    private fun extractJson(text: String): String {
        val brace = text.indexOf('{')
        val bracket = text.indexOf('[')
        val start = when {
            brace == -1 -> bracket
            bracket == -1 -> brace
            else -> minOf(brace, bracket)
        }
        return if (start > 0) text.substring(start) else text
    }
}
