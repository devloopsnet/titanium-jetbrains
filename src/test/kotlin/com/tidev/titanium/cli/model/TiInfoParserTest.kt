package com.tidev.titanium.cli.model

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure unit test for the `ti info --output json` parser, against the real CLI v8 shape. */
class TiInfoParserTest {

    @Test
    fun parsesSdksDevicesCertsAndProfiles() {
        val env = TiInfoParser.parse(JsonParser.parseString(SAMPLE))

        assertEquals("8.1.5", env.cliVersion)

        // SDKs — map keyed by full id; selection comes from ios.tisdk / android.tisdk.
        assertEquals(2, env.sdks.size)
        assertEquals("13.2.0.GA", env.selectedSdk?.version)
        assertTrue(env.selectedSdk?.selected == true)

        // Devices: 1 iOS sim + 1 iOS device + 1 Android emulator.
        assertEquals(3, env.devices.size)
        assertTrue(env.devices.any { it.name == "iPhone 15" && it.target == TiTarget.IOS_SIMULATOR })
        assertTrue(env.devices.any { it.name == "My iPhone" && it.target == TiTarget.IOS_DEVICE })
        assertTrue(env.devices.any { it.name == "Pixel 8 API 34" && it.target == TiTarget.ANDROID_EMULATOR })

        // Certs come from certs.keychains.<keychain>.{developer,distribution}.
        assertEquals(2, env.iosCertificates.size)

        // Provisioning across development/distribution/adhoc/enterprise.
        assertEquals(1, env.iosProfiles.size)
        assertEquals("UUID1", env.iosProfiles.first().uuid)

        // Issues aggregated from ios.issues + android.issues.
        assertEquals(listOf("API too new"), env.issues)
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
          "titaniumCLI": { "version": "8.1.5" },
          "titanium": {
            "13.2.0.GA": { "version": "13.2.0", "path": "/sdks/13.2.0.GA" },
            "12.8.0.GA": { "version": "12.8.0", "path": "/sdks/12.8.0.GA" }
          },
          "ios": {
            "tisdk": "13.2.0.GA",
            "simulators": { "ios": { "26.5": [ { "udid": "ABC", "name": "iPhone 15", "version": "26.5" } ] } },
            "devices": [ { "udid": "DEV1", "name": "My iPhone" } ],
            "certs": { "keychains": { "/login.keychain": {
              "developer": [ { "name": "Dev: Acme", "invalid": false } ],
              "distribution": [ { "name": "Dist: Acme", "invalid": false } ]
            } } },
            "provisioning": {
              "development": [ { "uuid": "UUID1", "name": "Dev Profile", "appPrefix": "ABCDE", "expired": false } ],
              "distribution": [], "adhoc": [], "enterprise": []
            },
            "issues": []
          },
          "android": {
            "tisdk": "13.2.0.GA",
            "emulators": [ { "id": "Pixel_8_API_34", "name": "Pixel 8 API 34", "target": "Android 14" } ],
            "devices": [],
            "issues": [ { "id": "x", "type": "warning", "message": "API too new" } ]
          }
        }
        """.trimIndent()
    }
}
