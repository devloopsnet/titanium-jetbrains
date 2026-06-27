package com.tidev.titanium

import com.intellij.openapi.util.IconLoader

/** Icons used across the plugin. Referenced from plugin.xml via fully-qualified field paths. */
object TiIcons {
    @JvmField
    val Titanium = IconLoader.getIcon("/icons/titanium.svg", TiIcons::class.java)

    @JvmField
    val ToolWindow = IconLoader.getIcon("/icons/toolwindow.svg", TiIcons::class.java)
}
