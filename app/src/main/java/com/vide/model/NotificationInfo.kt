package com.vide.model

import android.app.PendingIntent

enum class NotificationCategory {
    CALLS, MESSAGES, OTHER
}

data class NotificationInfo(
    val appName: String,
    val content: String,
    val timestamp: Long,
    val packageName: String,
    val key: String,
    val contentIntent: PendingIntent? = null,
    val category: NotificationCategory = NotificationCategory.OTHER,
    val isImportant: Boolean = false
)
