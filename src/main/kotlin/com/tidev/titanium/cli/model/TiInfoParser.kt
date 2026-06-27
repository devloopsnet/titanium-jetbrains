package com.tidev.titanium.cli.model

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Parses the JSON emitted by `ti info --output json` into a [TiEnvironment].
 *
 * The CLI's `info` JSON shape drifts across SDK/CLI versions, so every access here is
 * defensive: missing keys yield empty lists rather than exceptions. The known top-level
 * keys are `titanium` (installed SDKs), `ios`, `android`, `titaniumCLI`, and `issues`.
 */
object TiInfoParser {

    fun parse(root: JsonElement?, cliVersion: String? = null): TiEnvironment {
        val obj = root as? JsonObject ?: return TiEnvironment(cliVersion = cliVersion)
        return TiEnvironment(
            cliVersion = cliVersion ?: obj.path("titaniumCLI", "version")?.string,
            sdks = parseSdks(obj.obj("titanium")),
            devices = parseIos(obj.obj("ios")) + parseAndroid(obj.obj("android")),
            iosCertificates = parseCerts(obj.path("ios", "certs")?.asObjOrNull()),
            iosProfiles = parseProfiles(obj.path("ios", "provisioning")?.asObjOrNull()),
            issues = parseIssues(obj.get("issues")),
        )
    }

    private fun parseSdks(node: JsonObject?): List<TiSdk> {
        node ?: return emptyList()
        // `titanium` is a map of version -> { version, path, ... }
        return node.entrySet().mapNotNull { (version, value) ->
            val v = value as? JsonObject ?: return@mapNotNull TiSdk(version)
            TiSdk(
                version = v.string("version") ?: version,
                path = v.string("path"),
                selected = v.bool("selected"),
            )
        }.sortedByDescending { it.version }
    }

    private fun parseIos(ios: JsonObject?): List<TiDevice> {
        ios ?: return emptyList()
        val out = mutableListOf<TiDevice>()
        // Simulators: usually an object keyed by runtime; collect any node carrying a udid.
        collectObjectsWith(ios.get("simulators"), "udid") { sim ->
            out += TiDevice(
                id = sim.string("udid") ?: return@collectObjectsWith,
                name = sim.string("name") ?: "iOS Simulator",
                platform = TiPlatform.IOS,
                target = TiTarget.IOS_SIMULATOR,
                osVersion = sim.string("version") ?: sim.string("runtime"),
            )
        }
        // Physical devices.
        ios.arr("devices")?.forEachObject { dev ->
            val udid = dev.string("udid") ?: dev.string("id") ?: return@forEachObject
            out += TiDevice(udid, dev.string("name") ?: "iOS Device", TiPlatform.IOS, TiTarget.IOS_DEVICE)
        }
        return out
    }

    private fun parseAndroid(android: JsonObject?): List<TiDevice> {
        android ?: return emptyList()
        val out = mutableListOf<TiDevice>()
        android.arr("avds")?.forEachObject { avd ->
            val name = avd.string("name") ?: return@forEachObject
            out += TiDevice(name, name, TiPlatform.ANDROID, TiTarget.ANDROID_EMULATOR, avd.string("target"))
        }
        android.arr("devices")?.forEachObject { dev ->
            val id = dev.string("id") ?: dev.string("serial") ?: return@forEachObject
            out += TiDevice(id, dev.string("model") ?: id, TiPlatform.ANDROID, TiTarget.ANDROID_DEVICE, dev.string("release"))
        }
        return out
    }

    private fun parseCerts(certs: JsonObject?): List<TiCertificate> {
        certs ?: return emptyList()
        val out = mutableListOf<TiCertificate>()
        for (key in listOf("developer", "distribution")) {
            certs.arr(key)?.forEachObject { c ->
                val name = c.string("name") ?: return@forEachObject
                out += TiCertificate(name, c.string("fullname"), c.bool("invalid"))
            }
        }
        return out
    }

    private fun parseProfiles(prov: JsonObject?): List<TiProvisioningProfile> {
        prov ?: return emptyList()
        val out = mutableListOf<TiProvisioningProfile>()
        for (key in listOf("development", "distribution", "adhoc")) {
            prov.arr(key)?.forEachObject { p ->
                val uuid = p.string("uuid") ?: return@forEachObject
                out += TiProvisioningProfile(uuid, p.string("name") ?: uuid, p.string("appId"), p.bool("expired"))
            }
        }
        return out
    }

    private fun parseIssues(node: JsonElement?): List<String> {
        val arr = node as? JsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            when {
                el.isJsonPrimitive -> el.asString
                el is JsonObject -> el.string("message") ?: el.string("title")
                else -> null
            }
        }
    }

    // ---- Defensive JSON helpers -------------------------------------------------------------

    private val JsonElement.string: String? get() = if (isJsonPrimitive) asString else null
    private fun JsonObject.string(key: String): String? = get(key)?.takeIf { it.isJsonPrimitive }?.asString
    private fun JsonObject.bool(key: String): Boolean = get(key)?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
    private fun JsonObject.obj(key: String): JsonObject? = get(key) as? JsonObject
    private fun JsonObject.arr(key: String): JsonArray? = get(key) as? JsonArray
    private fun JsonElement.asObjOrNull(): JsonObject? = this as? JsonObject

    private fun JsonObject.path(vararg keys: String): JsonElement? {
        var cur: JsonElement = this
        for (k in keys) {
            cur = (cur as? JsonObject)?.get(k) ?: return null
        }
        return cur
    }

    private inline fun JsonArray.forEachObject(action: (JsonObject) -> Unit) {
        for (el in this) (el as? JsonObject)?.let(action)
    }

    /** Recursively walk [node] and invoke [action] for every object that contains [marker]. */
    private fun collectObjectsWith(node: JsonElement?, marker: String, action: (JsonObject) -> Unit) {
        when (node) {
            is JsonObject -> {
                if (node.has(marker)) action(node) else node.entrySet().forEach { collectObjectsWith(it.value, marker, action) }
            }
            is JsonArray -> node.forEach { collectObjectsWith(it, marker, action) }
            else -> {}
        }
    }
}
