package com.tidev.titanium.debug

import com.intellij.execution.ExecutionResult
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.intellij.xdebugger.frame.XSuspendContext
import com.tidev.titanium.debug.cdp.CdpClient
import com.tidev.titanium.debug.cdp.CdpFrame
import com.tidev.titanium.debug.cdp.CdpPaused
import java.util.concurrent.ConcurrentHashMap

/**
 * Drives a Titanium debug session: launches the build (already started via [executionResult]),
 * connects to the runtime's Chrome DevTools endpoint, and bridges breakpoints / stepping / pauses
 * into the IDE's XDebugger UI.
 */
class TiDebugProcess(
    session: XDebugSession,
    private val executionResult: ExecutionResult,
    private val host: String,
    private val port: Int,
) : XDebugProcess(session) {

    private val project: Project get() = session.project
    private val cdp = CdpClient(host, port)
    private val editorsProvider = TiDebuggerEditorsProvider()
    private val breakpoints = ConcurrentHashMap<String, XLineBreakpoint<XBreakpointProperties<*>>>()

    override fun getEditorsProvider(): XDebuggerEditorsProvider = editorsProvider
    override fun doGetProcessHandler(): ProcessHandler = executionResult.processHandler
    override fun createConsole(): ExecutionConsole = executionResult.executionConsole
    override fun getBreakpointHandlers(): Array<XBreakpointHandler<*>> =
        arrayOf<XBreakpointHandler<*>>(TiBreakpointHandler(this))

    override fun sessionInitialized() {
        cdp.onPaused = { paused -> handlePaused(paused) }
        ApplicationManager.getApplication().executeOnPooledThread { connectWithRetry() }
    }

    private fun connectWithRetry() {
        // The app build + launch takes time before the debugger endpoint is up; retry ~60s.
        repeat(60) {
            if (cdp.connect()) {
                breakpoints.values.forEach { sendBreakpoint(it) }
                return
            }
            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                return
            }
        }
    }

    fun registerBreakpoint(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
        val key = bpKey(breakpoint) ?: return
        breakpoints[key] = breakpoint
        if (cdp.isConnected) sendBreakpoint(breakpoint)
    }

    fun unregisterBreakpoint(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
        bpKey(breakpoint)?.let { breakpoints.remove(it) }
    }

    private fun sendBreakpoint(bp: XLineBreakpoint<*>) {
        val file = bp.sourcePosition?.file ?: return
        cdp.setBreakpointByUrl(file.name, bp.line)
    }

    private fun bpKey(bp: XLineBreakpoint<*>): String? {
        val pos = bp.sourcePosition ?: return null
        return "${pos.file.path}:${pos.line}"
    }

    override fun resume(context: XSuspendContext?) {
        cdp.resume()
    }

    override fun startStepOver(context: XSuspendContext?) {
        cdp.stepOver()
    }

    override fun startStepInto(context: XSuspendContext?) {
        cdp.stepInto()
    }

    override fun startStepOut(context: XSuspendContext?) {
        cdp.stepOut()
    }

    override fun stop() {
        cdp.close()
    }

    private fun handlePaused(paused: CdpPaused) {
        val frames = paused.frames.map {
            TiStackFrame(sourcePosition(it), it.functionName, cdp, it.scopeObjectIds, it.callFrameId)
        }
        session.positionReached(TiSuspendContext(TiExecutionStack(frames)))
    }

    private fun sourcePosition(frame: CdpFrame): XSourcePosition? {
        val path = frame.url.removePrefix("file://")
        val vf = LocalFileSystem.getInstance().findFileByPath(path) ?: return null
        return XDebuggerUtil.getInstance().createPosition(vf, frame.lineNumber)
    }
}

/** Bridges IDE breakpoints of [TiDebugBreakpointType] into the [TiDebugProcess]. */
class TiBreakpointHandler(private val process: TiDebugProcess) :
    XBreakpointHandler<XLineBreakpoint<XBreakpointProperties<*>>>(TiDebugBreakpointType::class.java) {

    override fun registerBreakpoint(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
        process.registerBreakpoint(breakpoint)
    }

    override fun unregisterBreakpoint(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>, temporary: Boolean) {
        process.unregisterBreakpoint(breakpoint)
    }
}
