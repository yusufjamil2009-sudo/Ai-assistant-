package com.ustad.personalassistant.appcontrol

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

data class ResolvedApp(val packageName: String, val label: String, val launchIntent: Intent)
enum class AppResolveStatus { RESOLVED, NOT_INSTALLED, NOT_LAUNCHABLE, AMBIGUOUS }
data class AppResolveResult(val status: AppResolveStatus, val apps: List<ResolvedApp> = emptyList())

interface AppResolver {
    fun resolve(query: String): AppResolveResult
    fun isInstalled(packageName: String): Boolean
    fun launcherIntent(packageName: String): Intent?
}

object AppQueryNormalizer {
    private val commandWords = setOf("open", "launch", "start", "khol", "kholo", "kholna", "open karo", "khol do", "kholkar")
    fun normalize(value: String): String = value.lowercase()
        .replace(Regex("[^a-z0-9\\u0900-\\u097f ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .split(" ")
        .filterNot { commandWords.contains(it) }
        .joinToString(" ")
        .trim()
}

class AndroidAppResolver(private val context: Context) : AppResolver {
    private val pm: PackageManager get() = context.packageManager

    override fun resolve(query: String): AppResolveResult {
        val normalized = AppQueryNormalizer.normalize(query)
        if (normalized.isBlank()) return AppResolveResult(AppResolveStatus.NOT_INSTALLED)
        val candidates = launchableApps().map { app ->
            val label = app.loadLabel(pm).toString()
            Triple(app, label, score(normalized, AppQueryNormalizer.normalize(label), app.packageName))
        }.filter { it.third > 0 }.sortedByDescending { it.third }
        if (candidates.isEmpty()) return AppResolveResult(AppResolveStatus.NOT_INSTALLED)
        val best = candidates.first().third
        val top = candidates.filter { it.third == best }.mapNotNull { item ->
            pm.getLaunchIntentForPackage(item.first.packageName)?.let { ResolvedApp(item.first.packageName, item.second, it) }
        }
        return when {
            top.isEmpty() -> AppResolveResult(AppResolveStatus.NOT_LAUNCHABLE)
            top.size > 1 -> AppResolveResult(AppResolveStatus.AMBIGUOUS, top)
            else -> AppResolveResult(AppResolveStatus.RESOLVED, top)
        }
    }

    override fun isInstalled(packageName: String): Boolean = runCatching { pm.getApplicationInfo(packageName, 0); true }.getOrDefault(false)
    override fun launcherIntent(packageName: String): Intent? = pm.getLaunchIntentForPackage(packageName)

    private fun launchableApps(): List<ApplicationInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return if (Build.VERSION.SDK_INT >= 33) pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0)).map { it.activityInfo.applicationInfo }.distinctBy { it.packageName }
        else @Suppress("DEPRECATION") pm.queryIntentActivities(intent, 0).map { it.activityInfo.applicationInfo }.distinctBy { it.packageName }
    }

    private fun score(query: String, label: String, packageName: String): Int {
        val p = packageName.lowercase()
        return when {
            query == label -> 100
            label.startsWith(query) -> 90
            label.contains(query) -> 80
            p.contains(query.replace(" ", "")) -> 75
            query.split(" ").all { token -> label.contains(token) || p.contains(token) } -> 60
            else -> 0
        }
    }
}
