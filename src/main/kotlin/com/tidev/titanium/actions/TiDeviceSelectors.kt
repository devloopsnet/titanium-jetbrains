package com.tidev.titanium.actions

import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.tidev.titanium.TiIcons
import com.tidev.titanium.cli.model.TiPlatform
import com.tidev.titanium.environment.TiEnvironmentService
import com.tidev.titanium.run.TiRunSelection
import javax.swing.JComponent

private fun projectFrom(button: JComponent): Project? =
    CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(button))

/** Toolbar dropdown to pick the run platform (iOS / Android). */
class TiPlatformComboAction : ComboBoxAction(), DumbAware {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null
        if (project != null) {
            e.presentation.text = TiRunSelection.getInstance(project).platform.label
            e.presentation.icon = TiIcons.Titanium
        }
    }

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override fun createPopupActionGroup(button: JComponent): DefaultActionGroup {
        val project = projectFrom(button) ?: return DefaultActionGroup()
        val group = DefaultActionGroup()
        TiPlatform.entries.forEach { platform ->
            group.add(object : AnAction(platform.label, null, null), DumbAware {
                override fun actionPerformed(e: AnActionEvent) {
                    val selection = TiRunSelection.getInstance(project)
                    selection.platform = platform
                    selection.deviceId = null // reset device when platform changes
                }
            })
        }
        return group
    }
}

/** Toolbar dropdown to pick the device/simulator, resolved from `ti info` for the chosen platform. */
class TiDeviceComboAction : ComboBoxAction(), DumbAware {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null
        if (project != null) {
            val selection = TiRunSelection.getInstance(project)
            val env = TiEnvironmentService.getInstance(project).environment
            val device = env.devices.firstOrNull { it.id == selection.deviceId }
            e.presentation.text = device?.display ?: "Any device"
            e.presentation.icon = AllIcons.General.Mouse
        }
    }

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override fun createPopupActionGroup(button: JComponent): DefaultActionGroup {
        val project = projectFrom(button) ?: return DefaultActionGroup()
        val selection = TiRunSelection.getInstance(project)
        val env = TiEnvironmentService.getInstance(project).environment
        val group = DefaultActionGroup()

        group.add(object : AnAction("Any device / default", null, null), DumbAware {
            override fun actionPerformed(e: AnActionEvent) {
                selection.deviceId = null
            }
        })
        group.addSeparator()

        val devices = env.devicesFor(selection.platform)
        if (devices.isEmpty()) {
            group.add(object : AnAction("No devices detected — refresh environment", null, null), DumbAware {
                override fun actionPerformed(e: AnActionEvent) {
                    TiEnvironmentService.getInstance(project).refresh(notify = true)
                }
            })
        } else {
            devices.forEach { device ->
                group.add(object : AnAction(device.display, null, null), DumbAware {
                    override fun actionPerformed(e: AnActionEvent) {
                        selection.deviceId = device.id
                    }
                })
            }
        }
        return group
    }
}
