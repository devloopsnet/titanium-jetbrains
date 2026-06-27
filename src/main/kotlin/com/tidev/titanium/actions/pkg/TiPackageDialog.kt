package com.tidev.titanium.actions.pkg

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.tidev.titanium.cli.model.TiEnvironment
import com.tidev.titanium.cli.model.TiPlatform
import com.tidev.titanium.cli.model.TiProvisioningProfile
import com.tidev.titanium.cli.model.TiTarget
import com.tidev.titanium.run.TiPackageOptions
import com.tidev.titanium.settings.TiSettings
import java.io.File
import javax.swing.JComponent

/** Collects parameters for a distribution build, with cert/profile lists resolved from `ti info`. */
class TiPackageDialog(
    private val project: Project,
    private val env: TiEnvironment,
    private val projectDir: String,
    initialPlatform: TiPlatform,
) : DialogWrapper(project) {

    private val settings = TiSettings.getInstance().state

    private val platformCombo = ComboBox(TiPlatform.entries.toTypedArray()).apply {
        renderer = SimpleListCellRenderer.create("") { it.label }
        item = initialPlatform
    }
    private val targetCombo = ComboBox<TiTarget>().apply {
        renderer = SimpleListCellRenderer.create("") { it.label }
    }

    // iOS
    private val certCombo = ComboBox<String>().apply {
        renderer = SimpleListCellRenderer.create("(none)") { it }
    }
    private val profileCombo = ComboBox<TiProvisioningProfile>().apply {
        renderer = SimpleListCellRenderer.create("(none)") { "${it.name} — ${it.uuid}" }
    }

    // Android
    private val keystoreField = TextFieldWithBrowseButton().apply { text = settings.androidKeystorePath }
    private val aliasField = JBTextField(settings.androidKeystoreAlias)
    private val storePasswordField = JBPasswordField()
    private val keyPasswordField = JBPasswordField()

    private val outputField = TextFieldWithBrowseButton().apply {
        text = File(projectDir, settings.distOutputDir.ifBlank { "dist" }).path
    }

    init {
        title = "Package Titanium App"
        keystoreField.addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFileDescriptor().withTitle("Select Keystore"),
        )
        outputField.addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor().withTitle("Output Directory"),
        )
        certCombo.run { env.iosCertificates.forEach { addItem(it.name) } }
        env.iosProfiles.forEach { profileCombo.addItem(it) }

        platformCombo.addActionListener { onPlatformChanged() }
        targetCombo.addActionListener { }
        init()
        onPlatformChanged()
    }

    private fun onPlatformChanged() {
        val platform = platformCombo.item ?: return
        targetCombo.removeAllItems()
        TiTarget.forPlatform(platform).filter { it.cliName.startsWith("dist") }.forEach { targetCombo.addItem(it) }

        val isIos = platform == TiPlatform.IOS
        certCombo.isEnabled = isIos
        profileCombo.isEnabled = isIos
        keystoreField.isEnabled = !isIos
        aliasField.isEnabled = !isIos
        storePasswordField.isEnabled = !isIos
        keyPasswordField.isEnabled = !isIos
    }

    override fun createCenterPanel(): JComponent =
        FormBuilder.createFormBuilder()
            .addLabeledComponent("Platform:", platformCombo)
            .addLabeledComponent("Target:", targetCombo)
            .addSeparator()
            .addLabeledComponent("iOS distribution cert:", certCombo)
            .addLabeledComponent("iOS provisioning profile:", profileCombo)
            .addSeparator()
            .addLabeledComponent("Android keystore:", keystoreField)
            .addLabeledComponent("Key alias:", aliasField)
            .addLabeledComponent("Keystore password:", storePasswordField)
            .addLabeledComponent("Key password:", keyPasswordField)
            .addSeparator()
            .addLabeledComponent("Output directory:", outputField)
            .panel

    override fun doValidate(): ValidationInfo? {
        if (targetCombo.item == null) return ValidationInfo("Select a distribution target", targetCombo)
        if (platformCombo.item == TiPlatform.ANDROID && keystoreField.text.isBlank()) {
            return ValidationInfo("A keystore is required for Android packaging", keystoreField)
        }
        if (outputField.text.isBlank()) return ValidationInfo("Choose an output directory", outputField)
        return null
    }

    /** Persist non-sensitive Android defaults and return the assembled options. */
    fun result(): TiPackageOptions {
        val platform = platformCombo.item ?: TiPlatform.IOS
        val target = targetCombo.item
            ?: TiTarget.forPlatform(platform).first { it.cliName.startsWith("dist") }
        if (platform == TiPlatform.ANDROID) {
            settings.androidKeystorePath = keystoreField.text.trim()
            settings.androidKeystoreAlias = aliasField.text.trim()
        }
        return TiPackageOptions(
            platform = platform,
            target = target,
            outputDir = outputField.text.trim(),
            iosCertificate = certCombo.item,
            iosProfileUuid = (profileCombo.item as? TiProvisioningProfile)?.uuid,
            keystorePath = keystoreField.text.trim(),
            keystoreAlias = aliasField.text.trim(),
            storePassword = String(storePasswordField.password),
            keyPassword = String(keyPasswordField.password),
        )
    }

    override fun getPreferredFocusedComponent(): JComponent = platformCombo
}
