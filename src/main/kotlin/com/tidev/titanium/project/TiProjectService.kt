package com.tidev.titanium.project

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile

/** Locates Titanium projects (tiapp.xml / timodule.xml) within the open project. */
@Service(Service.Level.PROJECT)
class TiProjectService(private val project: Project) {

    private val LOG = logger<TiProjectService>()

    @Volatile
    private var cached: List<TiProject>? = null

    fun projects(): List<TiProject> = cached ?: discover().also { cached = it }

    /** The primary Titanium project (first discovered), or null. */
    fun primary(): TiProject? = projects().firstOrNull()

    fun invalidate() {
        cached = null
    }

    private fun discover(): List<TiProject> {
        val base = project.guessProjectDir() ?: return emptyList()
        val out = mutableListOf<TiProject>()
        scan(base, depth = 0, out)
        LOG.info("Discovered ${out.size} Titanium project(s)")
        return out
    }

    private fun scan(dir: VirtualFile, depth: Int, out: MutableList<TiProject>) {
        if (depth > MAX_DEPTH || !dir.isDirectory) return
        if (dir.name in SKIP_DIRS) return

        dir.findChild("tiapp.xml")?.let { out += build(dir, it, TiProjectType.APP) }
        dir.findChild("timodule.xml")?.let { out += build(dir, it, TiProjectType.MODULE) }

        for (child in dir.children) {
            if (child.isDirectory) scan(child, depth + 1, out)
        }
    }

    private fun build(root: VirtualFile, descriptor: VirtualFile, type: TiProjectType): TiProject {
        val xml = runCatching { String(descriptor.contentsToByteArray()) }.getOrDefault("")
        return TiProject(
            rootDir = root,
            type = type,
            name = xml.tag("name"),
            appId = xml.tag("id"),
        )
    }

    /** Tiny, dependency-free tag extractor — good enough for name/id; full parsing comes later. */
    private fun String.tag(tag: String): String? {
        val m = Regex("<$tag>(.*?)</$tag>", RegexOption.DOT_MATCHES_ALL).find(this) ?: return null
        return m.groupValues[1].trim().ifBlank { null }
    }

    companion object {
        private const val MAX_DEPTH = 4
        private val SKIP_DIRS = setOf("node_modules", "build", ".git", "Resources", "dist", "platform")
        fun getInstance(project: Project): TiProjectService = project.service()
    }
}
