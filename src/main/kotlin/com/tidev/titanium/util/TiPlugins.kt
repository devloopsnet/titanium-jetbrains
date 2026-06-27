package com.tidev.titanium.util

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId

/**
 * Runtime capability detection. Core code uses this to branch *before* calling into the
 * optional JS layer, so nothing in the universal code path ever touches a class that only
 * exists when the JavaScript plugin is present.
 */
object TiPlugins {
    private val JAVASCRIPT = PluginId.getId("JavaScript")

    /**
     * True when the bundled JavaScript plugin is installed (WebStorm, IDEA Ultimate, PhpStorm,
     * PyCharm Pro, …). False on Community IDEs and Android Studio.
     *
     * Uses the stable [PluginManager.isPluginInstalled] rather than reaching into the JS plugin's
     * descriptor/classloader (which is going @Internal and is a Marketplace-ban risk).
     */
    val isJavaScriptAvailable: Boolean
        get() = PluginManager.isPluginInstalled(JAVASCRIPT)
}
