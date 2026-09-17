package com.ustad.personalassistant.automation

enum class AutomationResultStatus { SUCCESS, APP_NOT_FOUND, SERVICE_DISABLED, NODE_NOT_FOUND, ACTION_NOT_SUPPORTED, TIMEOUT, PERMISSION_REQUIRED, SECURITY_BLOCKED, VERIFICATION_FAILED, NETWORK_ERROR, AUTH_REQUIRED, ERROR }
data class AutomationResult<T>(val status: AutomationResultStatus, val value: T? = null, val durationMs: Long = 0L, val message: String? = null)
data class AutomationLogEntry(val timestamp: Long, val action: String, val targetApp: String?, val capability: String?, val securityDecision: String, val result: AutomationResultStatus, val durationMs: Long)
interface AutomationLogger { fun log(entry: AutomationLogEntry) }
class InMemoryAutomationLogger : AutomationLogger {
    private val entries = mutableListOf<AutomationLogEntry>()
    override fun log(entry: AutomationLogEntry) { synchronized(entries) { entries += entry; if (entries.size > 100) entries.removeAt(0) } }
    fun snapshot(): List<AutomationLogEntry> = synchronized(entries) { entries.toList() }
}
