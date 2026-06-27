package com.tidev.titanium.run

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.tidev.titanium.cli.model.TiPlatform

/**
 * Holds the platform + device the toolbar pickers have selected, persisted per-workspace so the
 * choice survives IDE restarts. The Build/Debug toolbar buttons read from here.
 */
@Service(Service.Level.PROJECT)
@State(name = "TitaniumRunSelection", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class TiRunSelection : PersistentStateComponent<TiRunSelection.State> {

    class State {
        @JvmField var platform: String = TiPlatform.IOS.cliName
        @JvmField var deviceId: String? = null
    }

    private var state = State()

    var platform: TiPlatform
        get() = TiPlatform.fromCli(state.platform) ?: TiPlatform.IOS
        set(value) {
            state.platform = value.cliName
        }

    var deviceId: String?
        get() = state.deviceId
        set(value) {
            state.deviceId = value
        }

    override fun getState(): State = state
    override fun loadState(s: State) {
        state = s
    }

    companion object {
        fun getInstance(project: Project): TiRunSelection = project.service()
    }
}
