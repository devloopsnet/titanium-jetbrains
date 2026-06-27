package com.tidev.titanium.debug

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredTextContainer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.intellij.xdebugger.frame.XExecutionStack
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XSuspendContext

/** A single stack frame: name + source position (variables not yet wired). */
class TiStackFrame(
    private val position: XSourcePosition?,
    private val title: String,
) : XStackFrame() {
    override fun getSourcePosition(): XSourcePosition? = position
    override fun customizePresentation(component: ColoredTextContainer) {
        component.append(title, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }
}

/** The (single) execution stack for a paused Titanium debug session. */
class TiExecutionStack(private val frames: List<TiStackFrame>) : XExecutionStack("Main thread") {
    override fun getTopFrame(): XStackFrame? = frames.firstOrNull()
    override fun computeStackFrames(firstFrameIndex: Int, container: XStackFrameContainer) {
        container.addStackFrames(frames.drop(firstFrameIndex), true)
    }
}

/** Suspend context wrapping a single thread/stack. */
class TiSuspendContext(private val stack: TiExecutionStack) : XSuspendContext() {
    override fun getActiveExecutionStack(): XExecutionStack = stack
}

/** Minimal editors provider for the debugger expression UI (plain text; evaluation TBD). */
class TiDebuggerEditorsProvider : XDebuggerEditorsProvider() {
    override fun getFileType(): FileType = PlainTextFileType.INSTANCE
    override fun createDocument(
        project: Project,
        expression: XExpression,
        sourcePosition: XSourcePosition?,
        mode: EvaluationMode,
    ): Document = EditorFactory.getInstance().createDocument(expression.expression)
}
