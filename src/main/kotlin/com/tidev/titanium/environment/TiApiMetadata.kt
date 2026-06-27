package com.tidev.titanium.environment

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import java.io.File

/**
 * Loads Titanium API metadata from the installed SDK's `api.jsca` so completion reflects the
 * actual SDK rather than a hand-maintained list. Best-effort: if the file is missing or the
 * format is unexpected, callers fall back to their curated defaults.
 */
@Service(Service.Level.PROJECT)
class TiApiMetadata(private val project: Project) {

    private val log = logger<TiApiMetadata>()

    @Volatile
    private var loadedPath: String? = null

    @Volatile
    var uiElements: Set<String> = emptySet()
        private set

    @Volatile
    var properties: Set<String> = emptySet()
        private set

    val isLoaded: Boolean get() = loadedPath != null

    /** Load metadata for [sdkPath] (the SDK install dir) on a background thread, once per path. */
    fun refreshFor(sdkPath: String?) {
        val path = sdkPath ?: return
        if (path == loadedPath) return
        val file = File(path, "api.jsca")
        if (!file.isFile) return
        ApplicationManager.getApplication().executeOnPooledThread { load(file, path) }
    }

    private fun load(file: File, path: String) {
        try {
            val root = file.bufferedReader().use { JsonParser.parseReader(it) } as? JsonObject ?: return
            val types = root.getAsJsonArray("types") ?: return
            val ui = sortedSetOf<String>()
            val props = sortedSetOf<String>()
            types.forEach { element ->
                val type = element as? JsonObject ?: return@forEach
                val name = type.get("name")?.asString ?: return@forEach
                if (name.startsWith("Titanium.UI.") || name.startsWith("Ti.UI.")) {
                    val short = name.substringAfterLast('.')
                    if (short.isNotBlank() && short[0].isUpperCase()) ui += short
                }
                type.getAsJsonArray("properties")?.forEach { p ->
                    (p as? JsonObject)?.get("name")?.asString?.let { props += it }
                }
            }
            uiElements = ui
            properties = props
            loadedPath = path
            log.info("Loaded Titanium API metadata: ${ui.size} UI types, ${props.size} properties")
        } catch (t: Throwable) {
            log.warn("Failed to parse api.jsca at $path", t)
        }
    }

    companion object {
        fun getInstance(project: Project): TiApiMetadata = project.service()
    }
}
