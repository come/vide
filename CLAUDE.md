# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**vide/** is a minimalist Android launcher that replaces the native home screen. The interface is 100% text — no app icons, no colors, no animations. Three screens: home, search drawer, notifications.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material3)
- **Architecture:** MVVM (ViewModel + StateFlow)
- **Min SDK:** 26 (Android 8.0) / **Target SDK:** 34
- **Gradle:** 8.4 / **AGP:** 8.2.2 / **Kotlin:** 1.9.22

## Build & Run

```bash
./gradlew assembleDebug          # Build
./gradlew installDebug           # Install on device/emulator
./gradlew test                   # Run tests
./gradlew lint                   # Lint
```

Note: generate the Gradle wrapper first with `gradle wrapper` if `gradlew` doesn't exist.

## Architecture

```
app/src/main/java/com/vide/
├── MainActivity.kt              # Single activity, launcher entry, screen routing
├── model/
│   ├── AppInfo.kt               # (label, packageName)
│   └── NotificationInfo.kt      # (appName, content, timestamp, packageName, key)
├── viewmodel/
│   └── AppListViewModel.kt      # App listing, search filtering, StateFlow
├── service/
│   └── VideNotificationListener.kt  # NotificationListenerService, shared StateFlow
├── components/
│   ├── ClockWidget.kt           # HH:mm + date, updates every second
│   ├── AppItem.kt               # Clickable text row, no ripple
│   ├── NotificationItem.kt      # App name / content / relative timestamp
│   └── BottomBar.kt             # "vide/" label + search/notif/back icons
├── screens/
│   ├── HomeScreen.kt            # Clock top, apps anchored bottom, bottom bar
│   ├── SearchScreen.kt          # Search input + filtered app list
│   └── NotificationsScreen.kt   # Notification list + "Tout effacer"
└── ui/theme/
    ├── Color.kt                 # Monochrome palette (light/dark)
    ├── Type.kt                  # Inter via Google Fonts, typography scale
    └── Theme.kt                 # VideTheme composable
```

### Screen Navigation

State-based routing in `MainActivity` via `currentScreen` string (`"home"`, `"search"`, `"notifications"`). No Navigation Compose — screens switch directly.

### Key Patterns

- **AppListViewModel** exposes `StateFlow<List<AppInfo>>` from `PackageManager.queryIntentActivities()`, filtered and sorted alphabetically. Also handles search with `filteredApps` flow.
- **VideNotificationListener** is a system `NotificationListenerService` — shares state via companion `MutableStateFlow`. Requires user to grant notification access in Settings.
- **HomeScreen** uses `LazyColumn(verticalArrangement = Arrangement.Bottom)` to anchor the app list at the bottom of the screen.
- **Inter font** loaded via `androidx.compose.ui:ui-text-google-fonts` with GMS provider (certs in `res/values/font_certs.xml`).
- **Edge-to-edge** via `enableEdgeToEdge()`, composables handle window insets manually.

## Strict Design Constraints

These are **hard rules**, not suggestions:

- **Monochrome only** — white/black/gray palette, no other colors, no gradients, no shadows
- **No app icons** — never call `PackageManager.getApplicationIcon()`
- **No animations** — no `AnimatedVisibility`, no `animateContentSize`
- **No elevation** — `elevation = 0.dp` everywhere
- **No decorative rounded corners**
- **No Scaffold, TopAppBar, BottomNavigation**
- **Single font: Inter** — Light (300) for clock & search items, Regular (400) elsewhere
- **Ripple disabled** on all interactive items (`indication = null`)
- Status bar & nav bar: transparent, edge-to-edge
- Back press ignored on home screen (`BackHandler {}`)

## Key Behaviors

| Action | Result |
|---|---|
| Tap app | Launch via `getLaunchIntentForPackage()` |
| Long press app | Open system app details (`ACTION_APPLICATION_DETAILS_SETTINGS`) |
| Back button | Ignored on home; returns to home from search/notifications |
| Home button | Stay on vide/ |
| Search icon (bottom bar) | Open search drawer |
| Circle icon (bottom bar) | Open notifications screen |
| "Tout effacer" | Clear all notifications via `cancelAllNotifications()` |

## Permissions

- `QUERY_ALL_PACKAGES` — required to list installed apps
- `BIND_NOTIFICATION_LISTENER_SERVICE` — notification access (user must enable in Settings)
