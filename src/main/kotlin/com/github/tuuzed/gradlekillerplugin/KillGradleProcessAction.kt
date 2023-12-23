package com.github.tuuzed.gradlekillerplugin

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.jetbrains.annotations.NotNull

class KillGradleProcessAction : AnAction() {

    companion object {
        val PROCESS_ID_REGEX = "\\d+".toRegex()
    }

    private val mNotificationGroup
        get() = NotificationGroupManager.getInstance().getNotificationGroup("GradleKillerNotificationGroup")

    override fun actionPerformed(@NotNull event: AnActionEvent) {
        queryGradleProcess().onFailure {
            showNotification("Kill gradle error due to exception.", NotificationType.ERROR)
        }.onSuccess { pids ->
            if (pids.isEmpty()) {
                showNotification("Kill gradle success.", NotificationType.INFORMATION)
            } else if (pids.all { killProcess(it) }) {
                showNotification("Kill gradle success.", NotificationType.INFORMATION)
            } else {
                showNotification("Kill gradle may failed.", NotificationType.WARNING)
            }
        }
    }

    private fun showNotification(message: String, notificationType: NotificationType) {
        val notification = mNotificationGroup.createNotification(message, notificationType)
        Notifications.Bus.notify(notification)
    }

    private fun killProcess(@NotNull pid: String): Boolean {
        return kotlin.runCatching {
            val process = Runtime.getRuntime().exec("taskkill /F /PID $pid")
            process.waitFor()
            process.destroy()
        }.isSuccess
    }

    private fun queryGradleProcess(): Result<List<String>> {
        return kotlin.runCatching {
            val process = Runtime.getRuntime().exec(
                "wmic process where \"commandline like '%gradle-launcher%' and name like '%java%'\" get processid"
            )
            process.waitFor()
            val pids = process.inputStream
                .reader()
                .readLines()
                .map { it.trim() }
                .filter { PROCESS_ID_REGEX.matches(it) }
            process.destroy()
            pids
        }
    }
}
