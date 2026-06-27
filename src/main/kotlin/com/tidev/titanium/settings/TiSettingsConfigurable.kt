package com.tidev.titanium.settings

import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toNullableProperty

/** Settings page at Settings | Tools | Titanium. */
class TiSettingsConfigurable : BoundSearchableConfigurable(
    "Titanium",
    "com.tidev.titanium.settings",
    "com.tidev.titanium.settings",
) {
    private val state get() = TiSettings.getInstance().state

    override fun createPanel(): DialogPanel = panel {
        group("Command-Line Tools") {
            row("Titanium CLI (ti):") {
                textField().bindText(state::cliPath).columns(34)
                    .comment("Path to the `ti` binary, or just `ti` if it's on your PATH.")
            }
            row("Alloy CLI:") {
                textField().bindText(state::alloyPath).columns(34)
            }
            row("Extra PATH entry:") {
                textField().bindText(state::nodePath).columns(34)
                    .comment("Optional directory prepended to PATH (e.g. your Node bin) so `ti`/`alloy` resolve.")
            }
        }
        group("Build") {
            row("Log level:") {
                comboBox(listOf("trace", "debug", "info", "warn", "error"))
                    .bindItem(state::logLevel.toNullableProperty())
            }
            row {
                checkBox("Enable LiveView by default").bindSelected(state::liveViewEnabled)
            }
            row("Distribution output directory:") {
                textField().bindText(state::distOutputDir).columns(20)
            }
        }
        group("Android Packaging") {
            row("Default keystore path:") {
                textField().bindText(state::androidKeystorePath).columns(34)
                    .comment("Used to pre-fill the Package dialog. Passwords are never stored.")
            }
            row("Default key alias:") {
                textField().bindText(state::androidKeystoreAlias).columns(20)
            }
        }
        group("Alloy") {
            row("Default i18n language:") {
                textField().bindText(state::defaultI18nLanguage).columns(8)
            }
        }
    }
}
