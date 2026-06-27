package com.tidev.titanium.actions.alloy

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent

/** Collects parameters for `alloy generate model <name> <adapter> [field:type …]`. */
class AlloyModelDialog(project: Project) : DialogWrapper(project) {

    private val nameField = JBTextField()
    private val adapterCombo = com.intellij.openapi.ui.ComboBox(arrayOf("sql", "properties"))
    private val schemaField = JBTextField().apply {
        toolTipText = "Space-separated field:type pairs, e.g. name:string age:number dob:date"
    }

    val modelName: String get() = nameField.text.trim()
    val adapter: String get() = adapterCombo.item
    val schemaTokens: List<String>
        get() = schemaField.text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

    init {
        title = "New Alloy Model"
        init()
    }

    override fun createCenterPanel(): JComponent =
        FormBuilder.createFormBuilder()
            .addLabeledComponent("Model name:", nameField)
            .addLabeledComponent("Adapter:", adapterCombo)
            .addLabeledComponent("Schema:", schemaField)
            .addComponentToRightColumn(
                com.intellij.ui.components.JBLabel("e.g.  name:string  age:number  dob:date").apply {
                    foreground = com.intellij.util.ui.UIUtil.getContextHelpForeground()
                },
            )
            .panel

    override fun doValidate(): ValidationInfo? =
        if (modelName.isBlank()) ValidationInfo("Model name is required", nameField) else null

    override fun getPreferredFocusedComponent(): JComponent = nameField
}
