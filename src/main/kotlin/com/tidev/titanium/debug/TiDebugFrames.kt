package com.tidev.titanium.debug

import com.intellij.icons.AllIcons
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
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XExecutionStack
import com.intellij.xdebugger.frame.XNamedValue
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XSuspendContext
import com.intellij.xdebugger.frame.XValueChildrenList
import com.intellij.xdebugger.frame.XValueNode
import com.intellij.xdebugger.frame.XValuePlace
import com.tidev.titanium.debug.cdp.CdpClient

/** A single stack frame: name + source position, with locals fetched lazily over CDP. */
class TiStackFrame(
    private val position: XSourcePosition?,
    private val title: String,
    private val cdp: CdpClient,
    private val scopeObjectIds: List<String>,
    private val callFrameId: String = "",
) : XStackFrame() {

    override fun getSourcePosition(): XSourcePosition? = position

    override fun getEvaluator(): XDebuggerEvaluator = TiEvaluator(cdp, callFrameId)

    override fun customizePresentation(component: ColoredTextContainer) {
        component.append(title, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }

    override fun computeChildren(node: XCompositeNode) {
        val scopeId = scopeObjectIds.firstOrNull()
        if (scopeId == null) {
            node.addChildren(XValueChildrenList.EMPTY, true)
            return
        }
        cdp.getProperties(scopeId) { props ->
            val list = XValueChildrenList()
            props.forEach { list.add(TiValue(it.name, it.value, it.objectId, cdp)) }
            node.addChildren(list, true)
        }
    }
}

/** A debugger variable; expandable when it references a remote object. */
class TiValue(
    name: String,
    private val value: String,
    private val objectId: String?,
    private val cdp: CdpClient,
) : XNamedValue(name) {

    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        node.setPresentation(AllIcons.Debugger.Value, null, value, objectId != null)
    }

    override fun computeChildren(node: XCompositeNode) {
        val id = objectId
        if (id == null) {
            super.computeChildren(node)
            return
        }
        cdp.getProperties(id) { props ->
            val list = XValueChildrenList()
            props.forEach { list.add(TiValue(it.name, it.value, it.objectId, cdp)) }
            node.addChildren(list, true)
        }
    }
}

/** Evaluates watch/console expressions in the paused frame via CDP. */
class TiEvaluator(private val cdp: CdpClient, private val callFrameId: String) : XDebuggerEvaluator() {
    override fun evaluate(
        expression: String,
        callback: XEvaluationCallback,
        expressionPosition: XSourcePosition?,
    ) {
        if (callFrameId.isBlank()) {
            callback.errorOccurred("No active stack frame")
            return
        }
        cdp.evaluateOnCallFrame(callFrameId, expression) { prop ->
            if (prop == null) callback.errorOccurred("Could not evaluate '$expression'")
            else callback.evaluated(TiValue(expression, prop.value, prop.objectId, cdp))
        }
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

/** Minimal editors provider for the debugger expression UI (plain text). */
class TiDebuggerEditorsProvider : XDebuggerEditorsProvider() {
    override fun getFileType(): FileType = PlainTextFileType.INSTANCE
    override fun createDocument(
        project: Project,
        expression: XExpression,
        sourcePosition: XSourcePosition?,
        mode: EvaluationMode,
    ): Document = EditorFactory.getInstance().createDocument(expression.expression)
}
