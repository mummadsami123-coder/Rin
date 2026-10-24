package com.example.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

class AppLauncher(private val context: Context) {
    private val packageManager: PackageManager = context.packageManager

    fun getInstalledApps(): List<AppInfo> {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
        val appList = mutableListOf<AppInfo>()

        for (resolveInfo in resolveInfos) {
            val packageName = resolveInfo.activityInfo.packageName
            val appName = resolveInfo.loadLabel(packageManager).toString()
            val icon = resolveInfo.loadIcon(packageManager)
            appList.add(AppInfo(name = appName, packageName = packageName, icon = icon))
        }

        return appList.distinctBy { it.packageName }.sortedBy { it.name.lowercase() }
    }

    fun launchAppByName(query: String): LaunchResult {
        val cleanQuery = query.trim().lowercase()
            .removePrefix("open ")
            .removePrefix("launch ")
            .removePrefix("kholo ")
            .removeSuffix(" kholo")
            .removeSuffix(" open karo")
            .trim()

        val apps = getInstalledApps()

        // 1. Exact or starts-with match
        val matchedApp = apps.find { it.name.lowercase() == cleanQuery }
            ?: apps.find { it.name.lowercase().startsWith(cleanQuery) }
            ?: apps.find { it.name.lowercase().contains(cleanQuery) }
            ?: apps.find { cleanQuery.contains(it.name.lowercase()) }
            // Special mappings for common aliases
            ?: findByAlias(cleanQuery, apps)

        if (matchedApp == null) {
            return LaunchResult.NotFound("Application '$query' not found on your device.")
        }

        return launchByPackage(matchedApp.packageName, matchedApp.name)
    }

    fun launchByPackage(packageName: String, appName: String = packageName): LaunchResult {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return LaunchResult.Success(appName)
            } else {
                return LaunchResult.Error("Cannot launch $appName: Launch intent unavailable.")
            }
        } catch (e: Exception) {
            Log.e("AppLauncher", "Failed to launch $packageName", e)
            return LaunchResult.Error("Failed to launch $appName: ${e.message}")
        }
    }

    private fun findByAlias(query: String, apps: List<AppInfo>): AppInfo? {
        val aliasMap = mapOf(
            "youtube" to listOf("com.google.android.youtube", "youtube"),
            "whatsapp" to listOf("com.whatsapp", "whatsapp"),
            "chrome" to listOf("com.android.chrome", "chrome", "browser", "internet"),
            "camera" to listOf("camera", "com.google.android.GoogleCamera"),
            "settings" to listOf("settings", "com.android.settings"),
            "photos" to listOf("photos", "gallery", "com.google.android.apps.photos"),
            "music" to listOf("music", "spotify", "com.spotify.music"),
            "maps" to listOf("maps", "com.google.android.apps.maps"),
            "dialer" to listOf("phone", "dialer", "contacts"),
            "messages" to listOf("messages", "messaging", "sms")
        )

        for ((key, aliases) in aliasMap) {
            if (query.contains(key) || aliases.any { query.contains(it) }) {
                val app = apps.find { appInfo ->
                    aliases.any { alias ->
                        appInfo.packageName.lowercase().contains(alias) ||
                                appInfo.name.lowercase().contains(alias)
                    }
                }
                if (app != null) return app
            }
        }

        return null
    }

    sealed class LaunchResult {
        data class Success(val appName: String) : LaunchResult()
        data class NotFound(val reason: String) : LaunchResult()
        data class Error(val errorMessage: String) : LaunchResult()
    }
}
