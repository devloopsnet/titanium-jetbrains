package com.tidev.titanium.run

import com.intellij.execution.configurations.GeneralCommandLine
import com.tidev.titanium.cli.TiCli
import com.tidev.titanium.cli.model.TiPlatform
import com.tidev.titanium.cli.model.TiTarget
import com.tidev.titanium.settings.TiSettings

/** Parameters for a distribution build (`ti build -T dist-*`). */
data class TiPackageOptions(
    val platform: TiPlatform,
    val target: TiTarget,
    val outputDir: String,
    // iOS distribution
    val iosCertificate: String? = null,
    val iosProfileUuid: String? = null,
    // Android distribution
    val keystorePath: String? = null,
    val keystoreAlias: String? = null,
    val storePassword: String? = null,
    val keyPassword: String? = null,
)

/**
 * Builds the `ti build` command for a distribution/packaging build. Mirrors the flags the
 * vscode-titanium extension passes:
 *   iOS:     -T dist-appstore|dist-adhoc  -R <dist cert>  -P <pp uuid>  [-O <dir>]
 *   Android: -T dist-playstore  -K <keystore>  -P <store pwd>  --key-password <pwd>  -L <alias>  -O <dir>
 */
object TiPackageCommandBuilder {

    fun build(options: TiPackageOptions, projectDir: String): GeneralCommandLine {
        val args = mutableListOf("build", "-p", options.platform.cliName, "-T", options.target.cliName)

        when (options.platform) {
            TiPlatform.IOS -> {
                options.iosCertificate?.takeIf { it.isNotBlank() }?.let { args += listOf("-R", it) }
                options.iosProfileUuid?.takeIf { it.isNotBlank() }?.let { args += listOf("-P", it) }
            }
            TiPlatform.ANDROID -> {
                options.keystorePath?.takeIf { it.isNotBlank() }?.let { args += listOf("-K", it) }
                options.storePassword?.takeIf { it.isNotBlank() }?.let { args += listOf("-P", it) }
                options.keyPassword?.takeIf { it.isNotBlank() }?.let { args += listOf("--key-password", it) }
                options.keystoreAlias?.takeIf { it.isNotBlank() }?.let { args += listOf("-L", it) }
            }
        }

        options.outputDir.takeIf { it.isNotBlank() }?.let { args += listOf("-O", it) }

        val logLevel = TiSettings.getInstance().state.logLevel
        if (logLevel.isNotBlank()) args += listOf("--log-level", logLevel)

        return TiCli.commandLine(args, projectDir = projectDir)
    }
}
