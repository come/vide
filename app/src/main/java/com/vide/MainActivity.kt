package com.vide

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vide.data.UsageTracker
import com.vide.screens.HomeScreen
import com.vide.screens.NotificationsScreen
import com.vide.screens.SearchScreen
import com.vide.service.VideNotificationListener
import com.vide.ui.theme.VideTheme
import com.vide.viewmodel.AppListViewModel

class MainActivity : ComponentActivity() {

    private val appViewModel: AppListViewModel by viewModels()
    private val currentScreen = mutableStateOf("home")
    private val notifGranted = mutableStateOf(false)
    private val usageGranted = mutableStateOf(false)
    private val contactsGranted = mutableStateOf(false)
    private val calendarGranted = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            VideTheme {
                val isNotifGranted by remember { notifGranted }
                val isUsageGranted by remember { usageGranted }
                val isContactsGranted by remember { contactsGranted }
                val isCalendarGranted by remember { calendarGranted }
                var promptDismissed by rememberSaveable { mutableStateOf(false) }
                var screen by remember { currentScreen }

                val contactsLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    contactsGranted.value = granted
                }

                val calendarLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    calendarGranted.value = granted
                    if (granted) appViewModel.refreshCalendar()
                }

                val homeApps by appViewModel.homeApps.collectAsState()
                val lastOpenedPackage by appViewModel.lastOpenedPackage.collectAsState()
                val pinnedApps by appViewModel.pinnedApps.collectAsState()
                val filteredApps by appViewModel.filteredApps.collectAsState()
                val contactActions by appViewModel.contactActions.collectAsState()
                val searchQuery by appViewModel.searchQuery.collectAsState()
                val lastSearchQuery by appViewModel.lastSearchQuery.collectAsState()
                val notifications by VideNotificationListener.notifications.collectAsState()
                val calendarEvents by appViewModel.calendarEvents.collectAsState()

                BackHandler { /* Ignore back on home screen */ }

                val allGranted = isNotifGranted && isUsageGranted && isContactsGranted && isCalendarGranted
                if (!allGranted && !promptDismissed) {
                    PermissionsPrompt(
                        usageGranted = isUsageGranted,
                        notifGranted = isNotifGranted,
                        contactsGranted = isContactsGranted,
                        calendarGranted = isCalendarGranted,
                        onEnableUsage = { openUsageStatsSettings() },
                        onEnableNotif = { openNotificationListenerSettings() },
                        onEnableContacts = {
                            contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                        },
                        onEnableCalendar = {
                            calendarLauncher.launch(Manifest.permission.READ_CALENDAR)
                        },
                        onContinue = { promptDismissed = true }
                    )
                } else {
                    when (screen) {
                        "home" -> HomeScreen(
                            homeApps = homeApps,
                            lastOpenedPackage = lastOpenedPackage,
                            pinnedApps = pinnedApps,
                            calendarEvents = calendarEvents,
                            onAppClick = {
                                appViewModel.recordLaunch(it.packageName)
                                launchApp(it.packageName)
                            },
                            onPinApp = { appViewModel.pinApp(it.packageName) },
                            onUnpinApp = { appViewModel.unpinApp(it.packageName) },
                            onRemoveApp = { appViewModel.removeFromHome(it.packageName) },
                            onAppInfo = { openAppInfo(it.packageName) },
                            onResetHome = { appViewModel.resetHome() },
                            onSearchClick = {
                                appViewModel.enterSearch()
                                screen = "search"
                            },
                            onNotificationsClick = { screen = "notifications" },
                            onEventClick = { openCalendarEvent(it.id) }
                        )

                        "search" -> SearchScreen(
                            query = searchQuery,
                            placeholder = lastSearchQuery,
                            apps = filteredApps,
                            contactActions = contactActions,
                            onQueryChange = { appViewModel.updateSearch(it) },
                            onAppClick = {
                                appViewModel.recordLaunch(it.packageName)
                                launchApp(it.packageName)
                                screen = "home"
                            },
                            onAppLongClick = { openAppInfo(it.packageName) },
                            onContactAction = { handleContactAction(it) },
                            onGoogleSearch = { googleSearch(it) },
                            onBack = { screen = "home" }
                        )

                        "notifications" -> NotificationsScreen(
                            notifications = notifications,
                            onNotificationClick = { notif ->
                                val pi = notif.contentIntent
                                if (pi != null) {
                                    try {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                            val options = android.app.ActivityOptions.makeBasic()
                                            options.setPendingIntentBackgroundActivityStartMode(
                                                android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                                            )
                                            pi.send(this@MainActivity, 0, null, null, null, null, options.toBundle())
                                        } else {
                                            pi.send()
                                        }
                                    } catch (e: Exception) {
                                        launchApp(notif.packageName)
                                    }
                                } else {
                                    launchApp(notif.packageName)
                                }
                            },
                            onNotificationLongClick = { notif ->
                                VideNotificationListener.instance?.toggleImportant(notif.key)
                            },
                            onDismiss = { notif ->
                                try {
                                    VideNotificationListener.instance?.cancelNotification(notif.key)
                                } catch (_: Exception) {}
                            },
                            onClearAll = {
                                VideNotificationListener.instance?.clearAllNotifications()
                            },
                            onBack = { screen = "home" }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        currentScreen.value = "home"
        appViewModel.refreshApps()
        notifGranted.value = isNotificationListenerEnabled()
        usageGranted.value = UsageTracker.isUsageStatsGranted(this)
        contactsGranted.value = checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        calendarGranted.value = checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED
        if (calendarGranted.value) appViewModel.refreshCalendar()
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(packageName) == true
    }

    private fun openNotificationListenerSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun openUsageStatsSettings() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        startActivity(intent)
    }

    private fun googleSearch(query: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}"))
        startActivity(intent)
    }

    private fun openCalendarEvent(eventId: Long) {
        try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            startActivity(Intent(Intent.ACTION_VIEW).apply { data = uri })
        } catch (_: Exception) {}
    }

    private fun openAppInfo(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private fun handleContactAction(action: com.vide.model.ContactAction) {
        val intent = when (action.actionType) {
            com.vide.model.ContactActionType.CALL -> Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${action.data}")
            }
            com.vide.model.ContactActionType.SMS -> Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${action.data}")
            }
            com.vide.model.ContactActionType.WHATSAPP -> Intent(Intent.ACTION_VIEW).apply {
                val cleanNumber = action.data.replace(Regex("[^+\\d]"), "")
                data = Uri.parse("https://wa.me/$cleanNumber")
                setPackage("com.whatsapp")
            }
            com.vide.model.ContactActionType.EMAIL -> Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${action.data}")
            }
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // App not available
        }
    }
}

@Composable
private fun PermissionsPrompt(
    usageGranted: Boolean,
    notifGranted: Boolean,
    contactsGranted: Boolean,
    calendarGranted: Boolean,
    onEnableUsage: () -> Unit,
    onEnableNotif: () -> Unit,
    onEnableContacts: () -> Unit,
    onEnableCalendar: () -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "permissions",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "vide/ fonctionne mieux avec ces accès.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        PermissionRow(
            label = "données d'utilisation",
            granted = usageGranted,
            onEnable = onEnableUsage
        )

        Spacer(modifier = Modifier.height(8.dp))

        PermissionRow(
            label = "notifications",
            granted = notifGranted,
            onEnable = onEnableNotif
        )

        Spacer(modifier = Modifier.height(8.dp))

        PermissionRow(
            label = "contacts",
            granted = contactsGranted,
            onEnable = onEnableContacts
        )

        Spacer(modifier = Modifier.height(8.dp))

        PermissionRow(
            label = "agenda",
            granted = calendarGranted,
            onEnable = onEnableCalendar
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "continuer",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onContinue
                )
                .padding(vertical = 12.dp)
        )
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    onEnable: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (granted) {
            Text(
                text = "activé",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "activer",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onEnable
                    )
                    .padding(start = 16.dp)
            )
        }
    }
}
