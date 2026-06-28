package com.tidev.titanium.cli.model

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Parses the JSON emitted by `ti info --output json` (Titanium CLI v8) into a [TiEnvironment].
 *
 * Real structure (abridged):
 * ```
 * {
 *   "titaniumCLI": { "version": "8.1.5" },
 *   "titanium": { "13.2.0.GA": { "version": "13.2.0", "path": "…" }, … },
 *   "ios": {
 *     "tisdk": "13.2.0.GA",
 *     "simulators": { "ios": { "26.5": [ { "udid": "…", "name": "…", "version": "26.5" } ] } },
 *     "devices": [ … ],
 *     "certs": { "keychains": { "<path>": { "developer": [ … ], "distribution": [ … ] } } },
 *     "provisioning": { "development": [ … ], "distribution": [ … ], "adhoc": [ … ], "enterprise": [ … ] },
 *     "issues": [ { "message": "…" } ]
 *   },
 *   "android": { "tisdk": "…", "emulators": [ … ], "devices": [ … ], "issues": [ … ] }
 * }
 * ```
 */
object TiInfoParser {

    fun parse(root: JsonElement?, cliVersion: String? = null): TiEnvironment {
        val obj = root as? JsonObject ?: return TiEnvironment(cliVersion = cliVersion)
        val selectedSdk = obj.path("ios", "tisdk")?.string ?: obj.path("android", "tisdk")?.string
        return TiEnvironment(
            cliVersion = cliVersion ?: obj.path("titaniumCLI", "version")?.string,
            sdks = parseSdks(obj.obj("titanium"), selectedSdk),
            devices = parseIos(obj.obj("ios")) + parseAndroid(obj.obj("android")),
            iosCertificates = parseCerts(obj.obj("ios")),
            iosProfiles = parseProfiles(obj.path("ios", "provisioning")?.asObjOrNull()),
            issues = parseIssues(obj),
        )
    }

    private fun parseSdks(node: JsonObject?, selectedKey: String?): List<TiSdk> {
        node ?: return emptyList()
        return node.entrySet().map { (key, value) ->
            val v = value as? JsonObject
            TiSdk(
                version = key, // map key carries the full id, e.g. 13.2.0.GA
                path = v?.string("path"),
                selected = key == selectedKey,
            )
        }.sortedByDescending { it.version }
    }

    private fun parseIos(ios: JsonObject?): List<TiDevice> {
        ios ?: return emptyList()
        val out = mutableListOf<TiDevice>()
        // simulators.ios.<runtime> -> [ { udid, name, version } ]
        ios.obj("simulators")?.obj("ios")?.entrySet()?.forEach { (_, arr) ->
            (arr as? JsonArray)?.forEachObject { sim ->
                val udid = sim.string("udid") ?: return@forEachObject
                out += TiDevice(
                    id = udid,
                    name = sim.string("name") ?: "iOS Simulator",
                    platform = TiPlatform.IOS,
                    target = TiTarget.IOS_SIMULATOR,
                    osVersion = sim.string("version"),
                )
            }
        }
        ios.arr("devices")?.forEachObject { dev ->
            val udid = dev.string("udid") ?: dev.string("id") ?: return@forEachObject
            out += TiDevice(udid, dev.string("name") ?: "iOS Device", TiPlatform.IOS, TiTarget.IOS_DEVICE)
        }
        return out
    }

    private fun parseAndroid(android: JsonObject?): List<TiDevice> {
        android ?: return emptyList()
        val out = mutableListOf<TiDevice>()
        // v8 uses "emulators" (older CLIs used "avds").
        (android.arr("emulators") ?: android.arr("avds"))?.forEachObject { emu ->
            val id = emu.string("id") ?: emu.string("name") ?: return@forEachObject
            out += TiDevice(id, emu.string("name") ?: id, TiPlatform.ANDROID, TiTarget.ANDROID_EMULATOR, emu.string("target"))
        }
        android.arr("devices")?.forEachObject { dev ->
            val id = dev.string("id") ?: dev.string("serialno") ?: dev.string("udid") ?: return@forEachObject
            val name = dev.string("name") ?: dev.string("model") ?: id
            out += TiDevice(id, name, TiPlatform.ANDROID, TiTarget.ANDROID_DEVICE, dev.string("release"))
        }
        return out
    }

    private fun parseCerts(ios: JsonObject?): List<TiCertificate> {
        val keychains = ios?.obj("certs")?.obj("keychains") ?: return emptyList()
        val out = mutableListOf<TiCertificate>()
        keychains.entrySet().forEach { (_, kc) ->
            val keychain = kc as? JsonObject ?: return@forEach
            for (kind in listOf("developer", "distribution")) {
                keychain.arr(kind)?.forEachObject { c ->
                    val name = c.string("name") ?: return@forEachObject
                    out += TiCertificate(name, c.string("fullname"), c.bool("invalid") || c.bool("expired"))
                }
            }
        }
        return out
    }

    private fun parseProfiles(prov: JsonObject?): List<TiProvisioningProfile> {
        prov ?: return emptyList()
        val out = mutableListOf<TiProvisioningProfile>()
        for (key in listOf("development", "distribution", "adhoc", "enterprise")) {
            prov.arr(key)?.forEachObject { p ->
                val uuid = p.string("uuid") ?: return@forEachObject
                out += TiProvisioningProfile(
                    uuid = uuid,
                    name = p.string("name") ?: uuid,
                    appId = p.string("appId") ?: p.string("appPrefix"),
                    expired = p.bool("expired"),
                )
            }
        }
        return out
    }

    private fun parseIssues(obj: JsonObject): List<String> {
        val out = mutableListOf<String>()
        listOf(obj.path("ios", "issues"), obj.path("android", "issues"), obj.get("issues")).forEach { node ->
            (node as? JsonArray)?.forEach { el ->
                when {
                    el.isJsonPrimitive -> out += el.asString
                    el is JsonObject -> (el.string("message") ?: el.string("title"))?.let { out += it }
                }
            }
        }
        return out
    }

    // ---- Defensive JSON helpers -------------------------------------------------------------

    private val JsonElement.string: String? get() = if (isJsonPrimitive) asString else null
    private fun JsonObject.string(key: String): String? = get(key)?.takeIf { it.isJsonPrimitive }?.asString
    private fun JsonObject.bool(key: String): Boolean =
        get(key)?.takeIf { it.isJsonPrimitive }?.runCatching { asBoolean }?.getOrDefault(false) ?: false
    private fun JsonObject.obj(key: String): JsonObject? = get(key) as? JsonObject
    private fun JsonObject.arr(key: String): JsonArray? = get(key) as? JsonArray
    private fun JsonElement.asObjOrNull(): JsonObject? = this as? JsonObject

    private fun JsonObject.path(vararg keys: String): JsonElement? {
        var cur: JsonElement = this
        for (k in keys) cur = (cur as? JsonObject)?.get(k) ?: return null
        return cur
    }

    private inline fun JsonArray.forEachObject(action: (JsonObject) -> Unit) {
        for (el in this) (el as? JsonObject)?.let(action)
    }
}
