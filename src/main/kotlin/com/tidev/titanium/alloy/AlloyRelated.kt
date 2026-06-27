package com.tidev.titanium.alloy

import com.intellij.openapi.vfs.VirtualFile

/** The three faces of an Alloy component. */
enum class AlloyKind(val dir: String, val ext: String) {
    CONTROLLER("controllers", "js"),
    VIEW("views", "xml"),
    STYLE("styles", "tss");

    companion object {
        fun of(file: VirtualFile): AlloyKind? {
            val path = file.path
            return when {
                path.contains("/app/controllers/") && file.extension == "js" -> CONTROLLER
                path.contains("/app/views/") && file.extension == "xml" -> VIEW
                path.contains("/app/styles/") && file.extension == "tss" -> STYLE
                else -> null
            }
        }
    }
}

/**
 * Resolves the related Alloy files for a given controller/view/style. Components share a base
 * name under `app/controllers`, `app/views`, `app/styles` (and the widget equivalents).
 */
object AlloyRelated {

    /** The `app/` root for [file], or null if it isn't inside an Alloy project. */
    private fun appRoot(file: VirtualFile): VirtualFile? {
        var dir = file.parent
        while (dir != null) {
            if (dir.name == "app" && dir.parent != null) return dir
            dir = dir.parent
        }
        return null
    }

    /** Base component name (the file name without extension), accounting for widget paths. */
    fun baseName(file: VirtualFile): String = file.nameWithoutExtension

    /** Find the related file of [targetKind] for [file], or null if it doesn't exist. */
    fun related(file: VirtualFile, targetKind: AlloyKind): VirtualFile? {
        val app = appRoot(file) ?: return null
        val name = baseName(file)
        return app.findFileByRelativePath("${targetKind.dir}/$name.${targetKind.ext}")
    }

    /** All related files (excluding [file] itself) that currently exist on disk. */
    fun allRelated(file: VirtualFile): List<VirtualFile> {
        val current = AlloyKind.of(file) ?: return emptyList()
        return AlloyKind.entries.filter { it != current }.mapNotNull { related(file, it) }
    }
}
