package com.tidev.titanium.run

import com.intellij.execution.configurations.RunConfigurationOptions

/**
 * Persisted state for a Titanium run configuration. Uses [RunConfigurationOptions]'
 * StoredProperty delegates so serialization is automatic (no readExternal/writeExternal).
 */
class TiRunConfigurationOptions : RunConfigurationOptions() {
    private val platformProp = string("ios").provideDelegate(this, "platform")
    var platform: String? by platformProp

    private val targetProp = string("simulator").provideDelegate(this, "target")
    var target: String? by targetProp

    private val deviceIdProp = string("").provideDelegate(this, "deviceId")
    var deviceId: String? by deviceIdProp

    private val sdkProp = string("").provideDelegate(this, "sdkVersion")
    var sdkVersion: String? by sdkProp

    private val deployTypeProp = string("").provideDelegate(this, "deployType")
    var deployType: String? by deployTypeProp

    private val projectDirProp = string("").provideDelegate(this, "projectDir")
    var projectDir: String? by projectDirProp

    private val extraArgsProp = string("").provideDelegate(this, "extraArgs")
    var extraArgs: String? by extraArgsProp

    private val buildOnlyProp = property(false).provideDelegate(this, "buildOnly")
    var buildOnly: Boolean by buildOnlyProp

    private val liveViewProp = property(true).provideDelegate(this, "liveView")
    var liveView: Boolean by liveViewProp
}
