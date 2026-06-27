package com.tidev.titanium.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil

/** Small VFS conveniences for actions that create files on disk via the CLI. */
object TiVfs {

    /** Synchronously refresh [path] (and its subtree) so newly created files appear in the IDE. */
    fun refresh(path: String) {
        val file = LocalFileSystem.getInstance().refreshAndFindFileByPath(path) ?: return
        VfsUtil.markDirtyAndRefresh(false, true, true, file)
    }

    /** Refresh then open [filePath] in an editor (no-op if it doesn't exist). Safe to call off-EDT. */
    fun refreshAndOpen(project: Project, filePath: String) {
        ApplicationManager.getApplication().invokeLater {
            val file = LocalFileSystem.getInstance().refreshAndFindFileByPath(filePath) ?: return@invokeLater
            FileEditorManager.getInstance(project).openTextEditor(
                OpenFileDescriptor(project, file), true,
            )
        }
    }
}
