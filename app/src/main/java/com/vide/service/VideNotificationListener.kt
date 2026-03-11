package com.vide.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.vide.model.NotificationCategory
import com.vide.model.NotificationInfo
import kotlinx.coroutines.flow.MutableStateFlow

class VideNotificationListener : NotificationListenerService() {

    companion object {
        val notifications = MutableStateFlow<List<NotificationInfo>>(emptyList())
        var instance: VideNotificationListener? = null
        val importantKeys = mutableSetOf<String>()
    }

    override fun onListenerConnected() {
        instance = this
        refreshNotifications()
    }

    override fun onListenerDisconnected() {
        instance = null
        notifications.value = emptyList()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshNotifications()
    }

    fun refreshNotifications() {
        val active = try {
            activeNotifications
        } catch (e: Exception) {
            return
        }

        val pm = packageManager
        notifications.value = active
            .filter { sbn -> sbn.isClearable }
            .map { sbn ->
                val appName = try {
                    pm.getApplicationLabel(
                        pm.getApplicationInfo(sbn.packageName, 0)
                    ).toString()
                } catch (e: Exception) {
                    sbn.packageName
                }

                val extras = sbn.notification.extras
                val text = extras.getCharSequence("android.text")?.toString()
                    ?: extras.getCharSequence("android.title")?.toString()
                    ?: ""

                NotificationInfo(
                    appName = appName,
                    content = text,
                    timestamp = sbn.postTime,
                    packageName = sbn.packageName,
                    key = sbn.key,
                    contentIntent = sbn.notification.contentIntent,
                    category = categorize(sbn),
                    isImportant = sbn.key in importantKeys
                )
            }
            .sortedWith(compareByDescending<NotificationInfo> { it.isImportant }.thenByDescending { it.timestamp })
    }

    fun toggleImportant(key: String) {
        if (key in importantKeys) {
            importantKeys.remove(key)
        } else {
            importantKeys.add(key)
        }
        refreshNotifications()
    }

    fun clearAllNotifications() {
        if (importantKeys.isEmpty()) {
            cancelAllNotifications()
        } else {
            try {
                activeNotifications.forEach { sbn ->
                    if (sbn.key !in importantKeys) {
                        cancelNotification(sbn.key)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private val callPackages = setOf(
        "com.android.dialer", "com.google.android.dialer",
        "com.samsung.android.incallui", "com.android.phone"
    )

    private val messagePackages = setOf(
        "com.whatsapp", "com.whatsapp.w4b",
        "com.google.android.gm", "com.microsoft.office.outlook",
        "com.android.mms", "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "org.telegram.messenger", "com.Slack",
        "com.facebook.orca", "com.discord",
        "com.mattermost.rn", "com.microsoft.teams",
        "com.skype.raider", "com.viber.voip",
        "com.linkedin.android"
    )

    private fun categorize(sbn: StatusBarNotification): NotificationCategory {
        val pkg = sbn.packageName
        val androidCategory = sbn.notification.category

        if (pkg in callPackages || androidCategory == Notification.CATEGORY_CALL ||
            androidCategory == Notification.CATEGORY_MISSED_CALL) {
            return NotificationCategory.CALLS
        }

        if (pkg in messagePackages || androidCategory == Notification.CATEGORY_MESSAGE ||
            androidCategory == Notification.CATEGORY_EMAIL) {
            return NotificationCategory.MESSAGES
        }

        return NotificationCategory.OTHER
    }
}
