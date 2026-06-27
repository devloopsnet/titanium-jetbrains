package com.tidev.titanium.sdk

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.tidev.titanium.cli.TiCli
import com.tidev.titanium.cli.TiCliException
import com.tidev.titanium.environment.TiEnvironmentService
import com.tidev.titanium.util.TiNotifications

/** Titanium SDK lifecycle operations (`ti sdk …`) plus a simple update check. */
object TiSdkManager {

    /** List available GA releases via `ti sdk list --releases -o json`. Best-effort. */
    fun listReleases(): List<String> {
        val json = try {
            TiCli.runJson(listOf("sdk", "list", "--releases"), timeoutMs = 60_000)
        } catch (e: TiCliException) {
            null
        } ?: return emptyList()

        val versions = linkedSetOf<String>()
        fun collect(node: com.google.gson.JsonElement?) {
            when (node) {
                is JsonObject -> {
                    // `releases` may be a map of version -> url, or nested under "releases".
                    node.entrySet().forEach { (k, v) ->
                        if (k.matches(VERSION_RE)) versions += k else collect(v)
                    }
                }
                is JsonArray -> node.forEach { el ->
                    if (el.isJsonPrimitive) el.asString.takeIf { it.matches(VERSION_RE) }?.let { versions += it }
                    else collect(el)
                }
                else -> {}
            }
        }
        collect(json)
        return versions.sortedDescending()
    }

    fun install(project: Project, version: String) = backgroundSdkOp(
        project,
        "Installing Titanium SDK $version",
        listOf("sdk", "install", version, "--default"),
        timeoutMs = 600_000,
        successMessage = "Installed Titanium SDK $version",
    )

    fun select(project: Project, version: String) = backgroundSdkOp(
        project,
        "Selecting Titanium SDK $version",
        listOf("sdk", "select", version),
        successMessage = "Selected Titanium SDK $version",
    )

    fun uninstall(project: Project, version: String) = backgroundSdkOp(
        project,
        "Uninstalling Titanium SDK $version",
        listOf("sdk", "uninstall", version, "--force"),
        timeoutMs = 120_000,
        successMessage = "Uninstalled Titanium SDK $version",
    )

    /** Compare the selected SDK against the latest release and notify. */
    fun checkForUpdates(project: Project) {
        object : Task.Backgroundable(project, "Checking for Titanium updates", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                val env = TiEnvironmentService.getInstance(project).environment
                val latest = listReleases().firstOrNull()
                val current = env.selectedSdk?.version
                val msg = when {
                    latest == null -> "Couldn't determine the latest Titanium SDK release."
                    current == null -> "Latest Titanium SDK is $latest. No SDK is currently selected."
                    versionLt(current, latest) -> "Update available: $current → $latest. Use Tools | Titanium | Install SDK."
                    else -> "You're up to date (SDK $current)."
                }
                TiNotifications.info(project, msg)
            }
        }.queue()
    }

    private fun backgroundSdkOp(
        project: Project,
        title: String,
        args: List<String>,
        timeoutMs: Int = 60_000,
        successMessage: String,
    ) {
        object : Task.Backgroundable(project, title, true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                try {
                    val result = TiCli.run(args, timeoutMs = timeoutMs)
                    if (result.success) {
                        TiNotifications.info(project, successMessage)
                        TiEnvironmentService.getInstance(project).refresh()
                    } else {
                        TiNotifications.warn(project, "$title failed (exit ${result.exitCode}). ${result.stderr.take(200)}")
                    }
                } catch (e: TiCliException) {
                    TiNotifications.error(project, e.message ?: "Titanium CLI not found")
                }
            }
        }.queue()
    }

    /** Very small semantic-version "<" good enough for X.Y.Z[.GA] comparisons. */
    private fun versionLt(a: String, b: String): Boolean {
        val pa = a.split('.').mapNotNull { it.toIntOrNull() }
        val pb = b.split('.').mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x < y
        }
        return false
    }

    private val VERSION_RE = Regex("""^\d+\.\d+\.\d+.*""")
}
