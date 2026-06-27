package com.tidev.titanium.run

import com.intellij.execution.configurations.RunConfigurationOptions

/**
 * Persisted state for a Titanium run configuration. Uses [RunConfigurationOptions]'
 * StoredProperty delegates so serialization is automatic (no readExternal/writeExternal).
 *
 * The `by string(...)` / `by property(...)` form is the supported delegation; the property name
 * is captured automatically, so no manual `provideDelegate` is needed.
 */
class TiRunConfigurationOptions : RunConfigurationOptions() {
    var platform: String? by string("ios")
    var target: String? by string("simulator")
    var deviceId: String? by string("")
    var sdkVersion: String? by string("")
    var deployType: String? by string("")
    var projectDir: String? by string("")
    var extraArgs: String? by string("")
    var buildOnly: Boolean by property(false)
    var liveView: Boolean by property(true)
}
