package com.tidev.titanium.actions.pkg

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent

/** Collects parameters to generate an Android keystore via `keytool -genkeypair`. */
class TiCreateKeystoreDialog(project: Project, defaultDir: String) : DialogWrapper(project) {

    private val pathField = JBTextField("$defaultDir/app.jks")
    private val aliasField = JBTextField("app")
    private val storePasswordField = JBPasswordField()
    private val keyPasswordField = JBPasswordField()
    private val validityField = JBTextField("10000")
    private val nameField = JBTextField()
    private val orgField = JBTextField()
    private val countryField = JBTextField("US")

    val keystorePath: String get() = pathField.text.trim()
    val alias: String get() = aliasField.text.trim()
    val storePassword: String get() = String(storePasswordField.password)
    val keyPassword: String get() = String(keyPasswordField.password).ifBlank { storePassword }
    val validityDays: String get() = validityField.text.trim().ifBlank { "10000" }
    val dname: String
        get() = buildList {
            nameField.text.trim().takeIf { it.isNotBlank() }?.let { add("CN=$it") }
            orgField.text.trim().takeIf { it.isNotBlank() }?.let { add("O=$it") }
            countryField.text.trim().takeIf { it.isNotBlank() }?.let { add("C=$it") }
        }.joinToString(", ").ifBlank { "CN=Unknown" }

    init {
        title = "Create Android Keystore"
        init()
    }

    override fun createCenterPanel(): JComponent =
        FormBuilder.createFormBuilder()
            .addLabeledComponent("Keystore path:", pathField)
            .addLabeledComponent("Key alias:", aliasField)
            .addLabeledComponent("Keystore password:", storePasswordField)
            .addLabeledComponent("Key password:", keyPasswordField)
            .addLabeledComponent("Validity (days):", validityField)
            .addSeparator()
            .addLabeledComponent("Name (CN):", nameField)
            .addLabeledComponent("Organization (O):", orgField)
            .addLabeledComponent("Country (C):", countryField)
            .panel

    override fun doValidate(): ValidationInfo? = when {
        keystorePath.isBlank() -> ValidationInfo("Keystore path is required", pathField)
        alias.isBlank() -> ValidationInfo("Key alias is required", aliasField)
        storePassword.length < 6 -> ValidationInfo("Keystore password must be at least 6 characters", storePasswordField)
        else -> null
    }

    override fun getPreferredFocusedComponent(): JComponent = pathField
}
