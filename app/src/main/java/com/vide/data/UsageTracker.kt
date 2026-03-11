package com.vide.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import com.vide.model.AppInfo

class UsageTracker(private val context: Context) {

    private val prefs = context.getSharedPreferences("app_usage", Context.MODE_PRIVATE)

    fun recordLaunch(packageName: String) {
        val key = "count_$packageName"
        prefs.edit()
            .putInt(key, prefs.getInt(key, 0) + 1)
            .putString("last_opened", packageName)
            .apply()
    }

    fun getLastOpened(): String? = prefs.getString("last_opened", null)

    fun pinApp(packageName: String) {
        val pinned = getPinnedApps().toMutableSet()
        pinned.add(packageName)
        prefs.edit().putStringSet("pinned_apps", pinned).apply()
    }

    fun unpinApp(packageName: String) {
        val pinned = getPinnedApps().toMutableSet()
        pinned.remove(packageName)
        prefs.edit().putStringSet("pinned_apps", pinned).apply()
    }

    fun hideFromHome(packageName: String) {
        val hidden = getHiddenApps().toMutableSet()
        hidden.add(packageName)
        prefs.edit().putStringSet("hidden_apps", hidden).apply()
    }

    fun getPinnedApps(): Set<String> = prefs.getStringSet("pinned_apps", emptySet()) ?: emptySet()

    fun getHiddenApps(): Set<String> = prefs.getStringSet("hidden_apps", emptySet()) ?: emptySet()

    fun resetHome() {
        prefs.edit()
            .remove("hidden_apps")
            .remove("pinned_apps")
            .apply()
    }

    fun getTopApps(allApps: List<AppInfo>, count: Int): List<AppInfo> {
        val internalCounts = getInternalCounts()
        val systemStats = getSystemUsageStats()
        val lastPkg = getLastOpened()
        val pinned = getPinnedApps()
        val hidden = getHiddenApps()

        val available = allApps.filter { it.packageName !in hidden }

        val useSystem = systemStats.isNotEmpty()

        val scored = if (useSystem) {
            val maxSystem = systemStats.values.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
            available.map { app ->
                app to ((systemStats[app.packageName] ?: 0L) / maxSystem)
            }
        } else {
            // Fallback to internal counts only if system stats unavailable
            val maxInternal = internalCounts.values.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
            available.map { app ->
                app to ((internalCounts[app.packageName] ?: 0) / maxInternal)
            }
        }.sortedByDescending { it.second }

        val result = mutableListOf<AppInfo>()
        val added = mutableSetOf<String>()

        // Last opened first
        if (lastPkg != null && lastPkg !in hidden) {
            available.find { it.packageName == lastPkg }?.let {
                result.add(it)
                added.add(lastPkg)
            }
        }

        // Pinned apps next
        for (pkg in pinned) {
            if (result.size >= count) break
            if (pkg !in added) {
                available.find { it.packageName == pkg }?.let {
                    result.add(it)
                    added.add(pkg)
                }
            }
        }

        // Fill remaining with top scored
        for ((app, _) in scored) {
            if (result.size >= count) break
            if (app.packageName !in added) {
                result.add(app)
                added.add(app.packageName)
            }
        }

        return result
    }

    private fun getInternalCounts(): Map<String, Int> {
        return prefs.all
            .filter { it.key.startsWith("count_") }
            .mapKeys { it.key.removePrefix("count_") }
            .mapValues { (it.value as? Int) ?: 0 }
    }

    private fun getSystemUsageStats(): Map<String, Long> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE)
            as? UsageStatsManager ?: return emptyMap()

        val end = System.currentTimeMillis()
        val start = end - 30L * 24 * 60 * 60 * 1000

        return try {
            usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_MONTHLY, start, end)
                .filter { it.totalTimeInForeground > 0 }
                .associate { it.packageName to it.totalTimeInForeground }
        } catch (e: SecurityException) {
            emptyMap()
        }
    }

    companion object {
        fun isUsageStatsGranted(context: Context): Boolean {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }
    }
}
