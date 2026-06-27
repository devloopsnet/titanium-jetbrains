package com.tidev.titanium.cli.model

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure unit test for the `ti info --output json` parser (no IDE fixture required). */
class TiInfoParserTest {

    @Test
    fun parsesSdksDevicesCertsAndProfiles() {
        val env = TiInfoParser.parse(JsonParser.parseString(SAMPLE), "5.4.0")

        assertEquals("5.4.0", env.cliVersion)

        // SDKs
        assertEquals(1, env.sdks.size)
        assertEquals("12.4.0.GA", env.selectedSdk?.version)
        assertTrue(env.selectedSdk?.selected == true)

        // Devices: 1 iOS sim + 1 iOS device + 1 Android AVD + 1 Android device
        assertEquals(4, env.devices.size)
        assertTrue(env.devices.any { it.name == "iPhone 15" && it.platform == TiPlatform.IOS && it.target == TiTarget.IOS_SIMULATOR })
        assertTrue(env.devices.any { it.name == "My iPhone" && it.target == TiTarget.IOS_DEVICE })
        assertTrue(env.devices.any { it.name == "Pixel_7" && it.platform == TiPlatform.ANDROID && it.target == TiTarget.ANDROID_EMULATOR })
        assertTrue(env.devices.any { it.platform == TiPlatform.ANDROID && it.target == TiTarget.ANDROID_DEVICE })

        // Signing
        assertEquals(1, env.iosCertificates.size)
        assertEquals(1, env.iosProfiles.size)
        assertEquals("UUID1", env.iosProfiles.first().uuid)

        // Issues
        assertEquals(listOf("Something needs attention"), env.issues)
    }

    @Test
    fun emptyInputYieldsEmptyEnvironment() {
        val env = TiInfoParser.parse(null, null)
        assertTrue(env.sdks.isEmpty())
        assertTrue(env.devices.isEmpty())
    }

    private companion object {
        val SAMPLE = """
        {
          "titaniumCLI": { "version": "5.4.0" },
          "titanium": {
            "12.4.0.GA": { "version": "12.4.0.GA", "path": "/sdks/12.4.0.GA", "selected": true }
          },
          "ios": {
            "simulators": { "ios": { "17.0": [ { "name": "iPhone 15", "udid": "ABC", "version": "17.0" } ] } },
            "devices": [ { "name": "My iPhone", "udid": "DEV1" } ],
            "certs": { "distribution": [ { "name": "iPhone Distribution: Acme", "invalid": false } ] },
            "provisioning": { "distribution": [ { "uuid": "UUID1", "name": "Acme Dist", "appId": "com.acme" } ] }
          },
          "android": {
            "avds": [ { "name": "Pixel_7", "target": "android-34" } ],
            "devices": [ { "id": "emulator-5554", "model": "Pixel", "release": "14" } ]
          },
          "issues": [ "Something needs attention" ]
        }
        """.trimIndent()
    }
}
