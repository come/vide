package com.vide.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vide.components.NotificationItem
import com.vide.model.NotificationCategory
import com.vide.model.NotificationInfo

private enum class NotifFilter(val label: String) {
    ALL("tous"),
    IMPORTANT("⭑"),
    CALLS("appels"),
    MESSAGES("messages"),
    OTHER("autres")
}

@Composable
fun NotificationsScreen(
    notifications: List<NotificationInfo>,
    onNotificationClick: (NotificationInfo) -> Unit,
    onNotificationLongClick: (NotificationInfo) -> Unit,
    onDismiss: (NotificationInfo) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var activeFilter by remember { mutableStateOf(NotifFilter.ALL) }

    val filtered = remember(notifications, activeFilter) {
        when (activeFilter) {
            NotifFilter.ALL -> notifications
            NotifFilter.IMPORTANT -> notifications.filter { it.isImportant }
            NotifFilter.CALLS -> notifications.filter { it.category == NotificationCategory.CALLS }
            NotifFilter.MESSAGES -> notifications.filter { it.category == NotificationCategory.MESSAGES }
            NotifFilter.OTHER -> notifications.filter { it.category == NotificationCategory.OTHER }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "VIDE/",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBack
                )
            )
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Filter tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NotifFilter.entries.forEach { filter ->
                Text(
                    text = filter.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (filter == activeFilter)
                        MaterialTheme.colorScheme.onBackground
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { activeFilter = filter }
                        )
                        .padding(vertical = 4.dp)
                )
            }
        }

        // Notification list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp)
        ) {
            items(filtered, key = { it.key }) { notification ->
                NotificationItem(
                    notification = notification,
                    onClick = { onNotificationClick(notification) },
                    onLongClick = { onNotificationLongClick(notification) },
                    onDismiss = { onDismiss(notification) }
                )
            }
        }

        // Clear all
        if (notifications.isNotEmpty()) {
            Text(
                text = "Tout effacer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClearAll
                    )
                    .padding(24.dp)
            )
        }
    }
}
