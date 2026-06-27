package com.tidev.titanium.cli.model

/** Platforms a Titanium app can target. */
enum class TiPlatform(val cliName: String, val label: String) {
    IOS("ios", "iOS"),
    ANDROID("android", "Android");

    companion object {
        fun fromCli(value: String?): TiPlatform? =
            entries.firstOrNull { it.cliName.equals(value, ignoreCase = true) }
    }
}

/** Build targets passed to `ti build -T`. */
enum class TiTarget(val cliName: String, val label: String, val platform: TiPlatform) {
    IOS_SIMULATOR("simulator", "Simulator", TiPlatform.IOS),
    IOS_DEVICE("device", "Device", TiPlatform.IOS),
    IOS_DIST_APPSTORE("dist-appstore", "App Store", TiPlatform.IOS),
    IOS_DIST_ADHOC("dist-adhoc", "Ad Hoc", TiPlatform.IOS),
    ANDROID_EMULATOR("emulator", "Emulator", TiPlatform.ANDROID),
    ANDROID_DEVICE("device", "Device", TiPlatform.ANDROID),
    ANDROID_DIST_PLAYSTORE("dist-playstore", "Play Store", TiPlatform.ANDROID);

    companion object {
        fun forPlatform(platform: TiPlatform): List<TiTarget> = entries.filter { it.platform == platform }
    }
}

/** An installed Titanium SDK. */
data class TiSdk(
    val version: String,
    val path: String? = null,
    val selected: Boolean = false,
)

/** A bootable simulator/emulator or a connected physical device. */
data class TiDevice(
    val id: String,
    val name: String,
    val platform: TiPlatform,
    val target: TiTarget,
    val osVersion: String? = null,
) {
    val display: String get() = buildString {
        append(name)
        osVersion?.let { append(" (").append(it).append(')') }
    }
}

/** iOS signing certificate. */
data class TiCertificate(val name: String, val fullName: String? = null, val expired: Boolean = false)

/** iOS provisioning profile. */
data class TiProvisioningProfile(
    val uuid: String,
    val name: String,
    val appId: String? = null,
    val expired: Boolean = false,
)

/**
 * Snapshot of the detected Titanium environment, parsed from `ti info --output json`.
 * Everything that needs a dropdown (SDK / device / cert / profile pickers) reads from here.
 */
data class TiEnvironment(
    val cliVersion: String? = null,
    val sdks: List<TiSdk> = emptyList(),
    val devices: List<TiDevice> = emptyList(),
    val iosCertificates: List<TiCertificate> = emptyList(),
    val iosProfiles: List<TiProvisioningProfile> = emptyList(),
    val issues: List<String> = emptyList(),
) {
    val selectedSdk: TiSdk? get() = sdks.firstOrNull { it.selected } ?: sdks.firstOrNull()

    fun devicesFor(platform: TiPlatform, target: TiTarget? = null): List<TiDevice> =
        devices.filter { it.platform == platform && (target == null || it.target == target) }

    companion object {
        val EMPTY = TiEnvironment()
    }
}
