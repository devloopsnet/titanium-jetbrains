package com.tidev.titanium.debug

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.GenericProgramRunner
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.util.net.NetUtils
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.tidev.titanium.run.TiRunConfiguration
import java.io.IOException

/**
 * Handles the Debug executor for Titanium run configurations: rebuilds the app with the debug
 * host enabled, starts an XDebugSession, and attaches a [TiDebugProcess].
 */
class TiDebugRunner : GenericProgramRunner<RunnerSettings>() {

    override fun getRunnerId(): String = "TitaniumDebugRunner"

    override fun canRun(executorId: String, profile: RunProfile): Boolean =
        executorId == DefaultDebugExecutor.EXECUTOR_ID && profile is TiRunConfiguration

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        FileDocumentManager.getInstance().saveAllDocuments()
        val config = environment.runProfile as? TiRunConfiguration ?: return null
        val port = try {
            NetUtils.findAvailableSocketPort()
        } catch (e: IOException) {
            9000
        }
        val debugState = config.createDebugState(environment, port)

        val session = XDebuggerManager.getInstance(environment.project).startSession(
            environment,
            object : XDebugProcessStarter() {
                override fun start(session: XDebugSession): XDebugProcess {
                    val result = debugState.execute(environment.executor, this@TiDebugRunner)
                    return TiDebugProcess(session, result, "127.0.0.1", port)
                }
            },
        )
        return session.runContentDescriptor
    }
}
