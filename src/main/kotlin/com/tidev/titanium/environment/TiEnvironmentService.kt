package com.tidev.titanium.environment

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import com.tidev.titanium.TitaniumBundle
import com.tidev.titanium.cli.TiCli
import com.tidev.titanium.cli.TiCliException
import com.tidev.titanium.cli.model.TiEnvironment
import com.tidev.titanium.cli.model.TiInfoParser
import com.tidev.titanium.util.TiNotifications
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Caches the detected Titanium environment (SDKs, simulators, emulators, devices, certs,
 * profiles) by shelling `ti info --output json`. Every picker in the plugin reads from here;
 * a manual refresh re-runs detection on a background thread.
 */
@Service(Service.Level.PROJECT)
class TiEnvironmentService(private val project: Project) {

    private val LOG = logger<TiEnvironmentService>()
    private val loading = AtomicBoolean(false)

    @Volatile
    var environment: TiEnvironment = TiEnvironment.EMPTY
        private set

    @Volatile
    var cliAvailable: Boolean = false
        private set

    @Volatile
    var lastError: String? = null
        private set

    val isLoading: Boolean get() = loading.get()

    /** Refresh the environment on a background thread. Coalesces concurrent requests. */
    fun refresh(notify: Boolean = false) {
        if (!loading.compareAndSet(false, true)) return

        object : Task.Backgroundable(project, TitaniumBundle.message("toolwindow.loading"), true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                try {
                    val version = TiCli.version()
                    cliAvailable = version != null
                    if (version == null) {
                        environment = TiEnvironment.EMPTY
                        lastError = TitaniumBundle.message("toolwindow.no.cli")
                        return
                    }
                    indicator.checkCanceled()
                    // v8 CLI: plain `ti info --output json` (no --types/--no-* flags).
                    val json = TiCli.runJson(listOf("info"), timeoutMs = 120_000)
                    environment = TiInfoParser.parse(json, version)
                    lastError = null
                    LOG.info("Titanium env: ${environment.sdks.size} SDK(s), ${environment.devices.size} device(s)")
                } catch (e: TiCliException) {
                    cliAvailable = false
                    environment = TiEnvironment.EMPTY
                    lastError = e.message
                    LOG.warn("Environment refresh failed", e)
                }
            }

            override fun onFinished() {
                loading.set(false)
                // Load SDK API metadata for completion (best-effort, background).
                TiApiMetadata.getInstance(project).refreshFor(environment.selectedSdk?.path)
                project.messageBus.syncPublisher(CHANGED).environmentChanged(environment)
                if (notify && lastError == null) {
                    if (environment.issues.isEmpty()) {
                        TiNotifications.info(project, TitaniumBundle.message("notification.env.refreshed"))
                    } else {
                        TiNotifications.warn(
                            project,
                            "Titanium environment has ${environment.issues.size} issue(s). See the Titanium tool window.",
                        )
                    }
                }
            }
        }.queue()
    }

    fun interface Listener {
        fun environmentChanged(environment: TiEnvironment)
    }

    companion object {
        @JvmField
        @Topic.ProjectLevel
        val CHANGED: Topic<Listener> = Topic.create("Titanium environment changed", Listener::class.java)

        fun getInstance(project: Project): TiEnvironmentService = project.service()
    }
}
