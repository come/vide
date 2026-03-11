@file:OptIn(ExperimentalFoundationApi::class)

package com.vide.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vide.components.BottomBar
import com.vide.components.ClockWidget
import com.vide.model.AppInfo
import com.vide.model.CalendarEvent

@Composable
fun HomeScreen(
    homeApps: List<AppInfo>,
    lastOpenedPackage: String?,
    pinnedApps: Set<String>,
    calendarEvents: List<CalendarEvent> = emptyList(),
    onAppClick: (AppInfo) -> Unit,
    onPinApp: (AppInfo) -> Unit,
    onUnpinApp: (AppInfo) -> Unit,
    onRemoveApp: (AppInfo) -> Unit,
    onAppInfo: (AppInfo) -> Unit,
    onResetHome: () -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onEventClick: (CalendarEvent) -> Unit = {}
) {
    val homeStyle = MaterialTheme.typography.titleLarge
    var expandedPackage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { expandedPackage = null }
            )
    ) {
        ClockWidget(
            events = calendarEvents,
            onEventClick = onEventClick,
            modifier = Modifier.padding(horizontal = 24.dp).padding(top = 48.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            items(homeApps, key = { it.packageName }) { app ->
                val isLastOpened = app.packageName == lastOpenedPackage
                val isPinned = app.packageName in pinnedApps
                val isExpanded = app.packageName == expandedPackage

                if (isExpanded) {
                    // Expanded action menu
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = app.label,
                            style = homeStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { expandedPackage = null },
                                    onLongClick = { expandedPackage = null }
                                )
                                .padding(vertical = 10.dp)
                        )

                        HomeActionItem(
                            label = if (isPinned) "désépingler" else "épingler",
                            onClick = {
                                if (isPinned) onUnpinApp(app) else onPinApp(app)
                                expandedPackage = null
                            }
                        )

                        HomeActionItem(
                            label = "retirer",
                            onClick = {
                                onRemoveApp(app)
                                expandedPackage = null
                            }
                        )

                        HomeActionItem(
                            label = "infos",
                            onClick = {
                                onAppInfo(app)
                                expandedPackage = null
                            }
                        )

                        HomeActionItem(
                            label = "réinitialiser",
                            onClick = {
                                onResetHome()
                                expandedPackage = null
                            }
                        )
                    }
                } else {
                    // Normal item
                    Text(
                        text = app.label,
                        style = homeStyle,
                        color = if (isLastOpened)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    expandedPackage = null
                                    onAppClick(app)
                                },
                                onLongClick = { expandedPackage = app.packageName }
                            )
                            .padding(vertical = 10.dp)
                    )

                    if (isLastOpened) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        BottomBar(
            onSearchClick = onSearchClick,
            onNotificationsClick = onNotificationsClick,
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
        )
    }
}

@Composable
private fun HomeActionItem(
    label: String,
    onClick: () -> Unit
) {
    Text(
        text = "— $label",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(start = 16.dp, top = 6.dp, bottom = 6.dp)
    )
}
