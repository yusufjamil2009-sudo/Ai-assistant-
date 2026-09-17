package com.ustad.personalassistant.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
import com.ustad.personalassistant.capability.CapabilityEngine
import com.ustad.personalassistant.domain.CapabilityStatus
import com.ustad.personalassistant.permissions.Capability
import java.util.Locale

enum class DiagnosticStatus { HEALTHY, WARNING, ERROR, UNAVAILABLE, UNKNOWN }
enum class DiagnosticCategory { BATTERY, STORAGE, MEMORY, NETWORK, PERMISSION, APP, SERVICE, SYSTEM }

data class DiagnosticFinding(
    val category: DiagnosticCategory,
    val status: DiagnosticStatus,
    val title: String,
    val explanation: String,
    val actualValue: String? = null,
    val recommendedNextStep: String? = null,
    val userActionRequired: Boolean = false
)

data class DiagnosticReport(
    val timestamp: Long,
    val findings: List<DiagnosticFinding>
) {
    val overallStatus: DiagnosticStatus
        get() = when {
            findings.any { it.status == DiagnosticStatus.ERROR } -> DiagnosticStatus.ERROR
            findings.any { it.status == DiagnosticStatus.WARNING } -> DiagnosticStatus.WARNING
            findings.all { it.status == DiagnosticStatus.UNAVAILABLE || it.status == DiagnosticStatus.UNKNOWN } -> DiagnosticStatus.UNKNOWN
            else -> DiagnosticStatus.HEALTHY
        }
}

interface ServiceStateProbe {
    fun state(): String
}

class StaticServiceStateProbe(private val value: String) : ServiceStateProbe {
    override fun state(): String = value
}

class DeviceDiagnosticsEngine(
    private val context: Context,
    private val capabilityEngine: CapabilityEngine,
    private val serviceProbes: Map<String, ServiceStateProbe> = emptyMap()
) {
    fun run(): DiagnosticReport {
        val findings = buildList {
            add(battery())
            add(storage())
            add(memory())
            add(network())
            addAll(permissions())
            addAll(apps())
            addAll(services())
            add(system())
        }
        return DiagnosticReport(System.currentTimeMillis(), findings)
    }

    private fun battery(): DiagnosticFinding {
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            ?: return DiagnosticFinding(DiagnosticCategory.BATTERY, DiagnosticStatus.UNAVAILABLE, "Battery", "Battery API unavailable")
        val level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (level !in 0..100) return DiagnosticFinding(DiagnosticCategory.BATTERY, DiagnosticStatus.UNKNOWN, "Battery", "Battery level unavailable")
        val charging = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) manager.isCharging else false
        return DiagnosticFinding(DiagnosticCategory.BATTERY, DiagnosticStatus.HEALTHY, "Battery", "Battery status available", "$level%, ${if (charging) "charging" else "not charging"}")
    }

    private fun storage(): DiagnosticFinding {
        val stat = StatFs(context.filesDir.absolutePath)
        val total = stat.totalBytes
        val available = stat.availableBytes
        if (total <= 0L) return DiagnosticFinding(DiagnosticCategory.STORAGE, DiagnosticStatus.UNKNOWN, "Storage", "Storage information unavailable")
        val percent = available.toDouble() / total.toDouble() * 100.0
        val status = if (percent < 10.0) DiagnosticStatus.WARNING else DiagnosticStatus.HEALTHY
        return DiagnosticFinding(DiagnosticCategory.STORAGE, status, "Storage", "Available application-visible filesystem storage", "${formatBytes(available)} available / ${formatBytes(total)} total", if (status == DiagnosticStatus.WARNING) "Review Android storage settings" else null, status == DiagnosticStatus.WARNING)
    }

    private fun memory(): DiagnosticFinding {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return DiagnosticFinding(DiagnosticCategory.MEMORY, DiagnosticStatus.UNAVAILABLE, "Memory", "Memory API unavailable")
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        val status = if (info.lowMemory) DiagnosticStatus.WARNING else DiagnosticStatus.HEALTHY
        return DiagnosticFinding(DiagnosticCategory.MEMORY, status, "Memory", "System memory snapshot; not private per-app memory", "${formatBytes(info.availMem)} available / ${formatBytes(info.totalMem)} total", if (status == DiagnosticStatus.WARNING) "Close unused apps or review Android memory usage" else null, status == DiagnosticStatus.WARNING)
    }

    private fun network(): DiagnosticFinding {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return DiagnosticFinding(DiagnosticCategory.NETWORK, DiagnosticStatus.UNAVAILABLE, "Network", "Connectivity API unavailable")
        val network = manager.activeNetwork ?: return DiagnosticFinding(DiagnosticCategory.NETWORK, DiagnosticStatus.WARNING, "Network", "No active network", "disconnected", "Check Wi-Fi or mobile data", true)
        val caps = manager.getNetworkCapabilities(network) ?: return DiagnosticFinding(DiagnosticCategory.NETWORK, DiagnosticStatus.UNKNOWN, "Network", "Network capabilities unavailable")
        val transport = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "other"
        }
        val validated = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val status = if (validated) DiagnosticStatus.HEALTHY else DiagnosticStatus.WARNING
        return DiagnosticFinding(DiagnosticCategory.NETWORK, status, "Network", "Active network detected", "$transport, ${if (validated) "internet validated" else "internet not validated"}", if (!validated) "Check network connectivity" else null, !validated)
    }

    private fun permissions(): List<DiagnosticFinding> = Capability.entries.map { capability ->
        val status = capabilityEngine.check(capability)
        val diagnostic = when (status) {
            CapabilityStatus.ON, CapabilityStatus.CONNECTED -> DiagnosticStatus.HEALTHY
            CapabilityStatus.OFF, CapabilityStatus.ACTION_REQUIRED, CapabilityStatus.CONNECT -> DiagnosticStatus.WARNING
            CapabilityStatus.NOT_AVAILABLE, CapabilityStatus.NOT_ENROLLED -> DiagnosticStatus.UNAVAILABLE
            CapabilityStatus.UNKNOWN -> DiagnosticStatus.UNKNOWN
        }
        DiagnosticFinding(DiagnosticCategory.PERMISSION, diagnostic, capability.name, "Capability state from the existing CapabilityEngine", status.name, if (diagnostic == DiagnosticStatus.WARNING) "Open the existing Permission Center/settings flow" else null, diagnostic == DiagnosticStatus.WARNING)
    }

    private fun apps(): List<DiagnosticFinding> {
        val packages = listOf(context.packageName)
        val pm = context.packageManager
        return packages.map { packageName ->
            try {
                val info = pm.getApplicationInfo(packageName, 0)
                DiagnosticFinding(DiagnosticCategory.APP, DiagnosticStatus.HEALTHY, "Assistant app", "Application is installed and enabled", "${info.packageName}, version ${runCatching { pm.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: "unknown"}")
            } catch (_: PackageManager.NameNotFoundException) {
                DiagnosticFinding(DiagnosticCategory.APP, DiagnosticStatus.ERROR, "Assistant app", "Application package is unavailable", packageName)
            }
        }
    }

    private fun services(): List<DiagnosticFinding> = serviceProbes.map { (name, probe) ->
        val state = runCatching { probe.state() }.getOrElse { "ERROR" }.uppercase(Locale.US)
        val status = when (state) { "RUNNING" -> DiagnosticStatus.HEALTHY; "STOPPED", "REQUIRES_PERMISSION" -> DiagnosticStatus.WARNING; "UNAVAILABLE" -> DiagnosticStatus.UNAVAILABLE; "ERROR" -> DiagnosticStatus.ERROR; else -> DiagnosticStatus.UNKNOWN }
        DiagnosticFinding(DiagnosticCategory.SERVICE, status, name, "Runtime service state supplied by the existing service probe", state, if (status == DiagnosticStatus.WARNING) "Use the existing supported enable/start flow" else null, status == DiagnosticStatus.WARNING)
    }

    private fun system(): DiagnosticFinding = DiagnosticFinding(
        DiagnosticCategory.SYSTEM,
        DiagnosticStatus.HEALTHY,
        "System",
        "Safe Android system information",
        "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}), ${Build.MANUFACTURER} ${Build.MODEL}"
    )

    private fun formatBytes(value: Long): String {
        val gb = value.toDouble() / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1.0) String.format(Locale.US, "%.1f GB", gb) else String.format(Locale.US, "%.0f MB", value.toDouble() / (1024.0 * 1024.0))
    }
}
