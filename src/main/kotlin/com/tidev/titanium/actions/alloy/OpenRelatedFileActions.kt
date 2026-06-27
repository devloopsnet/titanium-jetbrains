package com.tidev.titanium.actions.alloy

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vfs.VirtualFile
import com.tidev.titanium.alloy.AlloyKind
import com.tidev.titanium.alloy.AlloyRelated

/** Jump from the current Alloy file to its related controller/view/style. */
abstract class OpenRelatedFileAction(private val target: AlloyKind) : AnAction(), DumbAware {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = currentFile(e)
        e.presentation.isEnabledAndVisible =
            file != null && AlloyKind.of(file) != null && AlloyKind.of(file) != target
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = currentFile(e) ?: return
        val related = AlloyRelated.related(file, target) ?: return
        FileEditorManager.getInstance(project).openTextEditor(OpenFileDescriptor(project, related), true)
    }

    private fun currentFile(e: AnActionEvent): VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)
}

class OpenRelatedControllerAction : OpenRelatedFileAction(AlloyKind.CONTROLLER)
class OpenRelatedViewAction : OpenRelatedFileAction(AlloyKind.VIEW)
class OpenRelatedStyleAction : OpenRelatedFileAction(AlloyKind.STYLE)

/** Open every related Alloy file (controller + view + style) for the current file. */
class OpenAllRelatedFilesAction : AnAction(), DumbAware {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file != null && AlloyKind.of(file) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        AlloyRelated.allRelated(file).forEach {
            FileEditorManager.getInstance(project).openTextEditor(OpenFileDescriptor(project, it), false)
        }
    }
}
