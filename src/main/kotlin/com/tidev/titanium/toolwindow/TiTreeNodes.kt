package com.tidev.titanium.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.tidev.titanium.TiIcons
import com.tidev.titanium.cli.model.TiDevice
import com.tidev.titanium.cli.model.TiPlatform
import com.tidev.titanium.cli.model.TiSdk
import com.tidev.titanium.cli.model.TiTarget
import javax.swing.Icon
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

/** Payloads attached to tree nodes in the build explorer. */
sealed interface TiNode {
    val label: String
    val icon: Icon?
}

data class ProjectNode(val name: String, val type: String) : TiNode {
    override val label get() = "$name  ($type)"
    override val icon get() = TiIcons.Titanium
}

data class PlatformNode(val platform: TiPlatform) : TiNode {
    override val label get() = platform.label
    override val icon get() = AllIcons.Nodes.Module
}

data class TargetNode(val platform: TiPlatform, val target: TiTarget) : TiNode {
    override val label get() = target.label
    override val icon get() = AllIcons.Nodes.Folder
}

data class DeviceNode(val device: TiDevice) : TiNode {
    override val label get() = device.display
    override val icon get() = AllIcons.Actions.Execute
}

data class SdksGroupNode(val count: Int) : TiNode {
    override val label get() = "Titanium SDKs"
    override val icon get() = AllIcons.Nodes.PpLib
}

data class SdkNode(val sdk: TiSdk) : TiNode {
    override val label get() = sdk.version + if (sdk.selected) "  (selected)" else ""
    override val icon get() = AllIcons.Nodes.Library
}

data class MessageNode(override val label: String, val warning: Boolean = false) : TiNode {
    override val icon get() = if (warning) AllIcons.General.Warning else AllIcons.General.Information
}

fun tiNode(payload: TiNode): DefaultMutableTreeNode = DefaultMutableTreeNode(payload)

/** Renders [TiNode] payloads with icons and dimmed secondary text. */
class TiTreeCellRenderer : ColoredTreeCellRenderer() {
    override fun customizeCellRenderer(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean,
    ) {
        val payload = (value as? DefaultMutableTreeNode)?.userObject as? TiNode ?: return
        icon = payload.icon
        val attrs = if (payload is MessageNode && !payload.warning) {
            SimpleTextAttributes.GRAYED_ATTRIBUTES
        } else {
            SimpleTextAttributes.REGULAR_ATTRIBUTES
        }
        append(payload.label, attrs)
    }
}
