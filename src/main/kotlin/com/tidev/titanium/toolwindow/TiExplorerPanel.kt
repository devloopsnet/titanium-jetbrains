package com.tidev.titanium.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.treeStructure.Tree
import com.tidev.titanium.TitaniumBundle
import com.tidev.titanium.cli.model.TiEnvironment
import com.tidev.titanium.cli.model.TiPlatform
import com.tidev.titanium.cli.model.TiTarget
import com.tidev.titanium.environment.TiEnvironmentService
import com.tidev.titanium.project.TiProjectService
import com.tidev.titanium.run.TiRunLauncher
import java.awt.event.MouseEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

/**
 * The "Titanium" tool window: a build explorer tree of project → platforms → targets → devices
 * plus detected SDKs. Double-clicking a device builds & runs it. Refresh re-detects the env.
 */
class TiExplorerPanel(private val project: Project, parent: Disposable) : SimpleToolWindowPanel(true, true) {

    private val root = DefaultMutableTreeNode()
    private val treeModel = DefaultTreeModel(root)
    private val tree = Tree(treeModel).apply {
        isRootVisible = false
        showsRootHandles = true
        selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        cellRenderer = TiTreeCellRenderer()
    }

    init {
        TreeSpeedSearch.installOn(tree)
        toolbar = buildToolbar()
        setContent(ScrollPaneFactory.createScrollPane(tree))

        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean = runSelected()
        }.installOn(tree)

        // Rebuild whenever detection finishes.
        project.messageBus.connect(parent).subscribe(
            TiEnvironmentService.CHANGED,
            TiEnvironmentService.Listener { env -> ApplicationManager.getApplication().invokeLater { rebuild(env) } },
        )

        rebuild(TiEnvironmentService.getInstance(project).environment)
    }

    private fun buildToolbar(): javax.swing.JComponent {
        val group = DefaultActionGroup().apply {
            add(object : AnAction("Build Selected", "Build & run the selected target", AllIcons.Actions.Execute) {
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = selectedPayload() is DeviceNode || selectedPayload() is TargetNode
                }
                override fun actionPerformed(e: AnActionEvent) { runSelected() }
            })
            add(object : AnAction("Refresh", "Re-run `ti info`", AllIcons.Actions.Refresh) {
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = !TiEnvironmentService.getInstance(project).isLoading
                }
                override fun actionPerformed(e: AnActionEvent) {
                    TiEnvironmentService.getInstance(project).refresh(notify = true)
                }
            })
        }
        val toolbar = ActionManager.getInstance().createActionToolbar("TitaniumExplorer", group, true)
        toolbar.targetComponent = this
        return toolbar.component
    }

    private fun selectedPayload(): TiNode? =
        (tree.lastSelectedPathComponent as? DefaultMutableTreeNode)?.userObject as? TiNode

    private fun runSelected(): Boolean {
        when (val payload = selectedPayload()) {
            is DeviceNode -> { TiRunLauncher.launchDevice(project, payload.device); return true }
            is TargetNode -> { TiRunLauncher.launchTarget(project, payload.platform, payload.target); return true }
            else -> return false
        }
    }

    private fun rebuild(env: TiEnvironment) {
        root.removeAllChildren()

        val primary = TiProjectService.getInstance(project).primary()
        if (primary == null) {
            root.add(tiNode(MessageNode(TitaniumBundle.message("toolwindow.no.project"), warning = true)))
            treeModel.reload()
            return
        }
        root.add(tiNode(ProjectNode(primary.display, primary.type.name.lowercase())))

        val svc = TiEnvironmentService.getInstance(project)
        if (!svc.cliAvailable) {
            root.add(tiNode(MessageNode(svc.lastError ?: TitaniumBundle.message("toolwindow.no.cli"), warning = true)))
            treeModel.reload()
            return
        }

        for (platform in TiPlatform.entries) {
            val platformNode = tiNode(PlatformNode(platform))
            for (target in TiTarget.forPlatform(platform)) {
                val devices = env.devicesFor(platform, target)
                if (devices.isEmpty() && target.cliName.startsWith("dist")) continue
                val targetNode = tiNode(TargetNode(platform, target))
                devices.forEach { targetNode.add(tiNode(DeviceNode(it))) }
                if (devices.isEmpty()) targetNode.add(tiNode(MessageNode("No devices detected")))
                platformNode.add(targetNode)
            }
            root.add(platformNode)
        }

        if (env.sdks.isNotEmpty()) {
            val sdkGroup = tiNode(SdksGroupNode(env.sdks.size))
            env.sdks.forEach { sdkGroup.add(tiNode(SdkNode(it))) }
            root.add(sdkGroup)
        }

        treeModel.reload()
        // Expand the project + platform rows for convenience.
        for (i in 0 until tree.rowCount) tree.expandRow(i)
    }
}
