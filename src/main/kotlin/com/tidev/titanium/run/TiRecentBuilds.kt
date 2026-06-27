package com.tidev.titanium.run

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/** A previously launched build target (serializable bean). */
class RecentBuild {
    @JvmField var label: String = ""
    @JvmField var platform: String = "ios"
    @JvmField var target: String = "simulator"
    @JvmField var deviceId: String = ""
}

/** Remembers recently launched build targets (per-workspace), newest first, capped at 5. */
@Service(Service.Level.PROJECT)
@State(name = "TitaniumRecentBuilds", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class TiRecentBuilds : PersistentStateComponent<TiRecentBuilds.State> {

    class State {
        @JvmField var entries: MutableList<RecentBuild> = mutableListOf()
    }

    private var state = State()

    val recents: List<RecentBuild> get() = state.entries

    fun add(label: String, platform: String?, target: String?, deviceId: String?) {
        val entry = RecentBuild().apply {
            this.label = label
            this.platform = platform.orEmpty().ifBlank { "ios" }
            this.target = target.orEmpty().ifBlank { "simulator" }
            this.deviceId = deviceId.orEmpty()
        }
        state.entries.removeAll { it.label == label }
        state.entries.add(0, entry)
        while (state.entries.size > MAX) state.entries.removeAt(state.entries.size - 1)
    }

    fun clear() {
        state.entries.clear()
    }

    override fun getState(): State = state
    override fun loadState(s: State) {
        state = s
    }

    companion object {
        private const val MAX = 5
        fun getInstance(project: Project): TiRecentBuilds = project.service()
    }
}
