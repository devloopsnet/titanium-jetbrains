package com.tidev.titanium.util

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

/** Convenience accessors for the "Titanium" notification group (declared in plugin.xml). */
object TiNotifications {
    private const val GROUP = "Titanium"

    fun info(project: Project?, content: String) = notify(project, content, NotificationType.INFORMATION)
    fun warn(project: Project?, content: String) = notify(project, content, NotificationType.WARNING)
    fun error(project: Project?, content: String) = notify(project, content, NotificationType.ERROR)

    private fun notify(project: Project?, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP)
            .createNotification(content, type)
            .notify(project)
    }
}
