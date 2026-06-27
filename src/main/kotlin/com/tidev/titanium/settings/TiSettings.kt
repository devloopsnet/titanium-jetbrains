package com.tidev.titanium.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Application-level, machine-specific settings (CLI paths etc.). Stored PER_OS so a roaming
 * profile doesn't carry a macOS path onto Windows.
 */
@Service(Service.Level.APP)
@State(
    name = "TitaniumSettings",
    storages = [Storage(value = "titanium.xml", roamingType = RoamingType.PER_OS)],
)
class TiSettings : PersistentStateComponent<TiSettings.State> {

    class State {
        @JvmField var cliPath: String = "ti"
        @JvmField var alloyPath: String = "alloy"
        @JvmField var nodePath: String = ""
        @JvmField var logLevel: String = "info"
        @JvmField var defaultI18nLanguage: String = "en"
        @JvmField var distOutputDir: String = "dist"
        @JvmField var liveViewEnabled: Boolean = true

        // Android packaging defaults (non-sensitive only — passwords are never stored).
        @JvmField var androidKeystorePath: String = ""
        @JvmField var androidKeystoreAlias: String = ""
    }

    private var state = State()

    override fun getState(): State = state
    override fun loadState(s: State) {
        XmlSerializerUtil.copyBean(s, state)
    }

    companion object {
        fun getInstance(): TiSettings = service()
    }
}
