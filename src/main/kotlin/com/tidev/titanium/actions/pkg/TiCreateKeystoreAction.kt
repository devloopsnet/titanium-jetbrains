package com.tidev.titanium.actions.pkg

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.tidev.titanium.settings.TiSettings
import com.tidev.titanium.util.TiNotifications
import com.tidev.titanium.util.TiVfs
import java.io.File

/** Generate an Android signing keystore with the JDK's `keytool`. */
class TiCreateKeystoreAction : AnAction(), DumbAware {

    override fun getActionUpdateThread() = ActionUpdateThread.EDT
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val defaultDir = project.guessProjectDir()?.path ?: System.getProperty("user.home")
        val dialog = TiCreateKeystoreDialog(project, defaultDir)
        if (!dialog.showAndGet()) return

        val cmd = GeneralCommandLine(keytoolPath())
            .withParameters(
                "-genkeypair", "-v",
                "-keystore", dialog.keystorePath,
                "-alias", dialog.alias,
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", dialog.validityDays,
                "-storepass", dialog.storePassword,
                "-keypass", dialog.keyPassword,
                "-dname", dialog.dname,
            )

        object : Task.Backgroundable(project, "Creating keystore", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                val result = try {
                    CapturingProcessHandler(cmd).runProcess(60_000)
                } catch (t: Throwable) {
                    TiNotifications.error(project, "Could not run keytool: ${t.message}")
                    return
                }
                if (result.exitCode == 0) {
                    rememberKeystore(dialog.keystorePath, dialog.alias)
                    TiVfs.refresh(File(dialog.keystorePath).parent ?: defaultDir)
                    TiNotifications.info(project, "Keystore created: ${dialog.keystorePath}")
                } else {
                    TiNotifications.warn(project, "keytool failed (exit ${result.exitCode}). ${result.stderr.take(300)}")
                }
            }
        }.queue()
    }

    private fun rememberKeystore(path: String, alias: String) {
        TiSettings.getInstance().state.apply {
            androidKeystorePath = path
            androidKeystoreAlias = alias
        }
    }

    /** Prefer the keytool bundled with the running JDK; fall back to PATH. */
    private fun keytoolPath(): String {
        val home = System.getProperty("java.home") ?: return "keytool"
        val exe = if (System.getProperty("os.name").startsWith("Windows")) "keytool.exe" else "keytool"
        val candidate = File(File(home, "bin"), exe)
        return if (candidate.exists()) candidate.path else "keytool"
    }
}
