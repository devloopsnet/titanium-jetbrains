package com.tidev.titanium.actions.create

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.tidev.titanium.project.TiProjectType
import java.io.File
import javax.swing.JComponent

/** Collects parameters for `ti create -t app|module …`. */
class TiCreateProjectDialog(
    private val project: Project,
    private val type: TiProjectType,
    defaultLocation: String,
) : DialogWrapper(project) {

    private val nameField = JBTextField()
    private val idField = JBTextField()
    private val iosCheck = JBCheckBox("iOS", true)
    private val androidCheck = JBCheckBox("Android", true)
    private val locationField = TextFieldWithBrowseButton().apply { text = defaultLocation }

    val projectName: String get() = nameField.text.trim()
    val appId: String get() = idField.text.trim().ifBlank { "com.example.${projectName.lowercase().filter { it.isLetterOrDigit() }}" }
    val platforms: String get() = buildList {
        if (iosCheck.isSelected) add("ios")
        if (androidCheck.isSelected) add("android")
    }.joinToString(",")
    val location: String get() = locationField.text.trim()

    /** The directory the new project will live in once created. */
    val resultingProjectDir: String get() = File(location, projectName).path

    init {
        title = if (type == TiProjectType.APP) "New Titanium App" else "New Titanium Module"
        idField.emptyText.text = "com.example.app"
        locationField.addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
                .withTitle("Select Location")
                .withDescription("Choose where the project will be created"),
        )
        init()
    }

    override fun createCenterPanel(): JComponent =
        FormBuilder.createFormBuilder()
            .addLabeledComponent("Name:", nameField)
            .addLabeledComponent("App ID:", idField)
            .addLabeledComponent("Platforms:", platformRow())
            .addLabeledComponent("Location:", locationField)
            .panel

    private fun platformRow(): JComponent {
        val panel = javax.swing.JPanel()
        panel.layout = javax.swing.BoxLayout(panel, javax.swing.BoxLayout.X_AXIS)
        panel.add(iosCheck)
        panel.add(androidCheck)
        return panel
    }

    override fun doValidate(): ValidationInfo? = when {
        projectName.isBlank() -> ValidationInfo("Name is required", nameField)
        platforms.isBlank() -> ValidationInfo("Select at least one platform", iosCheck)
        location.isBlank() || !File(location).isDirectory -> ValidationInfo("Choose a valid location", locationField)
        else -> null
    }

    override fun getPreferredFocusedComponent(): JComponent = nameField
}
