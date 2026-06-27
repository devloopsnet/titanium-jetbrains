package com.tidev.titanium.project

import com.intellij.openapi.vfs.VirtualFile

/** Whether a project is a Titanium app or a native module. */
enum class TiProjectType { APP, MODULE }

/**
 * A discovered Titanium project: a directory containing a `tiapp.xml` (app) or `timodule.xml`
 * (module). [appDir] is the Alloy `app/` folder when present.
 */
data class TiProject(
    val rootDir: VirtualFile,
    val type: TiProjectType,
    val name: String?,
    val appId: String?,
) {
    val path: String get() = rootDir.path
    val appDir: String get() = "$path/app"
    val isAlloy: Boolean get() = rootDir.findChild("app")?.isDirectory == true

    val display: String get() = name ?: rootDir.name
}
