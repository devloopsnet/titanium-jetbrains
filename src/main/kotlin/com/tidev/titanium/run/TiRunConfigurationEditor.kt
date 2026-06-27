package com.tidev.titanium.run

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.tidev.titanium.cli.model.TiDevice
import com.tidev.titanium.cli.model.TiPlatform
import com.tidev.titanium.cli.model.TiTarget
import com.tidev.titanium.environment.TiEnvironmentService
import javax.swing.JComponent
import javax.swing.JPanel

/** UI for editing a [TiRunConfiguration]: platform / target / device / SDK / deploy options. */
class TiRunConfigurationEditor(private val project: Project) : SettingsEditor<TiRunConfiguration>() {

    private val env get() = TiEnvironmentService.getInstance(project).environment

    private val platformCombo = ComboBox(TiPlatform.entries.toTypedArray()).apply {
        renderer = SimpleListCellRenderer.create("") { it.label }
    }
    private val targetCombo = ComboBox<TiTarget>().apply {
        renderer = SimpleListCellRenderer.create("") { it.label }
    }
    private val deviceCombo = ComboBox<TiDevice>().apply {
        renderer = SimpleListCellRenderer.create("Any / default") { it.display }
    }
    private val sdkCombo = ComboBox<String>().apply {
        renderer = SimpleListCellRenderer.create("Project default") { it }
    }
    private val deployTypeCombo = ComboBox(arrayOf("", "development", "test", "production")).apply {
        renderer = SimpleListCellRenderer.create("Default") { if (it.isBlank()) "Default" else it }
    }
    private val buildOnly = JBCheckBox("Build only (don't install/launch)")
    private val liveView = JBCheckBox("Enable LiveView")
    private val extraArgs = JBTextField()

    init {
        platformCombo.addActionListener { reloadTargets(); reloadDevices() }
        targetCombo.addActionListener { reloadDevices() }
    }

    private fun reloadTargets() {
        val platform = platformCombo.item ?: return
        val prev = targetCombo.item
        targetCombo.removeAllItems()
        TiTarget.forPlatform(platform).forEach { targetCombo.addItem(it) }
        if (prev != null && prev.platform == platform) targetCombo.item = prev
    }

    private fun reloadDevices() {
        val platform = platformCombo.item ?: return
        val target = targetCombo.item
        val prev = deviceCombo.item
        deviceCombo.removeAllItems()
        deviceCombo.addItem(null) // "Any / default"
        env.devicesFor(platform, target).forEach { deviceCombo.addItem(it) }
        if (prev != null) deviceCombo.item = env.devices.firstOrNull { it.id == prev.id }
    }

    private fun reloadSdks(selected: String?) {
        sdkCombo.removeAllItems()
        sdkCombo.addItem("") // project default
        env.sdks.forEach { sdkCombo.addItem(it.version) }
        sdkCombo.item = selected ?: ""
    }

    override fun resetEditorFrom(config: TiRunConfiguration) {
        val o = config.tiOptions
        val platform = TiPlatform.fromCli(o.platform) ?: TiPlatform.IOS
        platformCombo.item = platform
        reloadTargets()
        targetCombo.item = TiTarget.forPlatform(platform).firstOrNull { it.cliName == o.target }
            ?: targetCombo.item
        reloadDevices()
        deviceCombo.item = env.devices.firstOrNull { it.id == o.deviceId }
        reloadSdks(o.sdkVersion)
        deployTypeCombo.item = o.deployType ?: ""
        buildOnly.isSelected = o.buildOnly
        liveView.isSelected = o.liveView
        extraArgs.text = o.extraArgs ?: ""
    }

    override fun applyEditorTo(config: TiRunConfiguration) {
        val o = config.tiOptions
        o.platform = platformCombo.item?.cliName
        o.target = targetCombo.item?.cliName
        o.deviceId = deviceCombo.item?.id ?: ""
        o.sdkVersion = sdkCombo.item ?: ""
        o.deployType = deployTypeCombo.item ?: ""
        o.buildOnly = buildOnly.isSelected
        o.liveView = liveView.isSelected
        o.extraArgs = extraArgs.text
    }

    override fun createEditor(): JComponent {
        val panel: JPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Platform:", platformCombo)
            .addLabeledComponent("Target:", targetCombo)
            .addLabeledComponent("Device / simulator:", deviceCombo)
            .addLabeledComponent("Titanium SDK:", sdkCombo)
            .addLabeledComponent("Deploy type:", deployTypeCombo)
            .addComponent(buildOnly)
            .addComponent(liveView)
            .addLabeledComponent("Extra CLI args:", extraArgs)
            .panel
        // Populate from the latest detected environment up front.
        reloadTargets()
        reloadDevices()
        reloadSdks(null)
        return panel
    }
}
