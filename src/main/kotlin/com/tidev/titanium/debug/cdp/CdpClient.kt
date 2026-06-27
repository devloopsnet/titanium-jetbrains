package com.tidev.titanium.debug.cdp

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.diagnostic.logger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.WebSocket
import java.util.concurrent.CompletionStage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** A paused-execution frame reported by the debugger. */
data class CdpFrame(
    val functionName: String,
    val url: String,
    val lineNumber: Int,
    val columnNumber: Int,
    val scopeObjectIds: List<String> = emptyList(),
)

/** A `Debugger.paused` event. */
data class CdpPaused(val reason: String, val frames: List<CdpFrame>)

/** A variable/property from `Runtime.getProperties`. */
data class CdpProperty(val name: String, val value: String, val objectId: String?)

/**
 * Minimal Chrome DevTools Protocol client over a WebSocket, used to talk to the debugger the
 * Titanium runtime exposes (via `ti build --debug-host host:port`). Uses the JDK HttpClient/
 * WebSocket so it needs no extra dependencies.
 *
 * This is an initial implementation: it connects, enables the Debugger domain, sets breakpoints
 * by URL, and relays pause/resume. Source-map fidelity and full evaluation are future work and
 * need validation against a running device/simulator.
 */
class CdpClient(private val host: String, private val port: Int) {

    private val log = logger<CdpClient>()
    private val gson = Gson()
    private val nextId = AtomicInteger(1)
    private val buffer = StringBuilder()
    private val callbacks = ConcurrentHashMap<Int, (JsonObject) -> Unit>()

    @Volatile private var ws: WebSocket? = null

    var onPaused: ((CdpPaused) -> Unit)? = null
    var onResumed: (() -> Unit)? = null
    var onClosed: (() -> Unit)? = null

    val isConnected: Boolean get() = ws != null

    /** Resolve the WebSocket debugger URL from `http://host:port/json` and connect. */
    fun connect(): Boolean {
        val wsUrl = resolveWebSocketUrl() ?: return false
        return try {
            val listener = object : WebSocket.Listener {
                override fun onOpen(webSocket: WebSocket) {
                    webSocket.request(1)
                }

                override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
                    buffer.append(data)
                    if (last) {
                        val message = buffer.toString()
                        buffer.setLength(0)
                        handleMessage(message)
                    }
                    webSocket.request(1)
                    return null
                }

                override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*>? {
                    onClosed?.invoke()
                    return null
                }

                override fun onError(webSocket: WebSocket, error: Throwable) {
                    log.warn("CDP websocket error", error)
                    onClosed?.invoke()
                }
            }
            ws = HttpClient.newHttpClient().newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), listener)
                .join()
            send("Debugger.enable")
            send("Runtime.enable")
            send("Runtime.runIfWaitingForDebugger")
            true
        } catch (t: Throwable) {
            log.info("CDP connect failed: ${t.message}")
            false
        }
    }

    private fun resolveWebSocketUrl(): String? = try {
        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder(URI.create("http://$host:$port/json")).GET().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        val array = JsonParser.parseString(response.body()) as? JsonArray
        array?.firstNotNullOfOrNull { (it as? JsonObject)?.get("webSocketDebuggerUrl")?.asString }
    } catch (t: Throwable) {
        null
    }

    fun send(method: String, params: Map<String, Any?> = emptyMap(), onResult: ((JsonObject) -> Unit)? = null): Int {
        val id = nextId.getAndIncrement()
        if (onResult != null) callbacks[id] = onResult
        val payload = gson.toJson(mapOf("id" to id, "method" to method, "params" to params))
        ws?.sendText(payload, true)
        return id
    }

    fun setBreakpointByUrl(fileName: String, line: Int) {
        send(
            "Debugger.setBreakpointByUrl",
            mapOf("lineNumber" to line, "urlRegex" to ".*${Regex.escape(fileName)}.*"),
        )
    }

    /** Fetch the (own) properties of a remote object and deliver them to [onResult]. */
    fun getProperties(objectId: String, onResult: (List<CdpProperty>) -> Unit) {
        send("Runtime.getProperties", mapOf("objectId" to objectId, "ownProperties" to true)) { result ->
            val props = (result.getAsJsonArray("result") ?: JsonArray()).mapNotNull { el ->
                val p = el as? JsonObject ?: return@mapNotNull null
                val name = p.get("name")?.asString ?: return@mapNotNull null
                val value = p.getAsJsonObject("value")
                val description = value?.get("description")?.asString
                    ?: value?.get("value")?.asString
                    ?: value?.get("type")?.asString
                    ?: "undefined"
                CdpProperty(name, description, value?.get("objectId")?.asString)
            }
            onResult(props)
        }
    }

    fun resume() = send("Debugger.resume")
    fun stepOver() = send("Debugger.stepOver")
    fun stepInto() = send("Debugger.stepInto")
    fun stepOut() = send("Debugger.stepOut")

    fun close() {
        try {
            ws?.sendClose(WebSocket.NORMAL_CLOSURE, "done")
        } catch (ignored: Throwable) {
        }
        ws = null
    }

    private fun handleMessage(message: String) {
        val obj = try {
            JsonParser.parseString(message) as? JsonObject ?: return
        } catch (t: Throwable) {
            return
        }
        try {
            // Command responses carry an "id"; events carry a "method".
            if (obj.has("id") && obj.get("id").isJsonPrimitive) {
                val id = obj.get("id").asInt
                val callback = callbacks.remove(id)
                if (callback != null) {
                    callback(obj.getAsJsonObject("result") ?: JsonObject())
                    return
                }
            }
            when (obj.get("method")?.asString) {
                "Debugger.paused" -> onPaused?.invoke(parsePaused(obj.getAsJsonObject("params")))
                "Debugger.resumed" -> onResumed?.invoke()
            }
        } catch (t: Throwable) {
            log.warn("Failed to handle CDP message", t)
        }
    }

    private fun parsePaused(params: JsonObject?): CdpPaused {
        val reason = params?.get("reason")?.asString ?: "breakpoint"
        val frames = (params?.getAsJsonArray("callFrames") ?: JsonArray()).mapNotNull { el ->
            val f = el as? JsonObject ?: return@mapNotNull null
            val location = f.getAsJsonObject("location")
            val url = f.getAsJsonObject("functionLocation")?.get("scriptId")?.asString
                ?: f.get("url")?.asString ?: ""
            val scopeIds = (f.getAsJsonArray("scopeChain") ?: JsonArray()).mapNotNull { scope ->
                (scope as? JsonObject)?.getAsJsonObject("object")?.get("objectId")?.asString
            }
            CdpFrame(
                functionName = f.get("functionName")?.asString?.ifBlank { "anonymous" } ?: "anonymous",
                url = f.get("url")?.asString ?: url,
                lineNumber = location?.get("lineNumber")?.asInt ?: 0,
                columnNumber = location?.get("columnNumber")?.asInt ?: 0,
                scopeObjectIds = scopeIds,
            )
        }
        return CdpPaused(reason, frames)
    }
}
