package com.vide.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vide.data.CalendarReader
import com.vide.data.ContactSearcher
import com.vide.data.SmartSearch
import com.vide.data.UsageTracker
import com.vide.model.AppInfo
import com.vide.model.CalendarEvent
import com.vide.model.ContactAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val usageTracker = UsageTracker(application)
    private val contactSearcher = ContactSearcher(application)

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _homeApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val homeApps: StateFlow<List<AppInfo>> = _homeApps.asStateFlow()

    private val _lastOpenedPackage = MutableStateFlow<String?>(null)
    val lastOpenedPackage: StateFlow<String?> = _lastOpenedPackage.asStateFlow()

    private val _pinnedApps = MutableStateFlow<Set<String>>(emptySet())
    val pinnedApps: StateFlow<Set<String>> = _pinnedApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _lastSearchQuery = MutableStateFlow("")
    val lastSearchQuery: StateFlow<String> = _lastSearchQuery.asStateFlow()

    private val _filteredApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val filteredApps: StateFlow<List<AppInfo>> = _filteredApps.asStateFlow()

    private val _contactActions = MutableStateFlow<List<ContactAction>>(emptyList())
    val contactActions: StateFlow<List<ContactAction>> = _contactActions.asStateFlow()

    private val _calendarEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEvent>> = _calendarEvents.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val appList = withContext(Dispatchers.IO) {
                val pm = getApplication<Application>().packageManager
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }

                val resolveInfos: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.queryIntentActivities(intent, 0)
                }

                resolveInfos
                    .map { ri ->
                        AppInfo(
                            label = ri.loadLabel(pm).toString(),
                            packageName = ri.activityInfo.packageName
                        )
                    }
                    .filter { it.packageName != getApplication<Application>().packageName }
                    .distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase() }
            }

            _apps.value = appList
            _filteredApps.value = appList
            updateHomeApps()
        }
    }

    private fun updateHomeApps() {
        viewModelScope.launch {
            val topApps = withContext(Dispatchers.IO) {
                usageTracker.getTopApps(_apps.value, HOME_APP_COUNT)
            }
            _lastOpenedPackage.value = usageTracker.getLastOpened()
            _pinnedApps.value = usageTracker.getPinnedApps()
            _homeApps.value = topApps
        }
    }

    fun recordLaunch(packageName: String) {
        usageTracker.recordLaunch(packageName)
        updateHomeApps()
    }

    fun pinApp(packageName: String) {
        usageTracker.pinApp(packageName)
        updateHomeApps()
    }

    fun unpinApp(packageName: String) {
        usageTracker.unpinApp(packageName)
        updateHomeApps()
    }

    fun removeFromHome(packageName: String) {
        usageTracker.hideFromHome(packageName)
        updateHomeApps()
    }

    fun resetHome() {
        usageTracker.resetHome()
        updateHomeApps()
    }

    fun updateSearch(query: String) {
        _searchQuery.value = query
        _filteredApps.value = SmartSearch.search(query, _apps.value)

        _contactActions.value = try {
            contactSearcher.search(query)
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    fun enterSearch() {
        _lastSearchQuery.value = _searchQuery.value
        updateSearch("")
    }

    fun refreshApps() {
        updateHomeApps()
    }

    fun refreshCalendar() {
        viewModelScope.launch {
            val events = withContext(Dispatchers.IO) {
                CalendarReader.getEvents(getApplication())
            }
            _calendarEvents.value = events
        }
    }

    companion object {
        const val HOME_APP_COUNT = 8
    }
}
