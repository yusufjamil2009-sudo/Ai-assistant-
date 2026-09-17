package com.ustad.personalassistant.security

import com.ustad.personalassistant.accessibility.AutomationDecision
import com.ustad.personalassistant.accessibility.AutomationPolicy

/** Execution-time security decision. The default is fail-closed. */
enum class SecurityDecision {
    ALLOW,
    DENY,
    AUTH_REQUIRED,
    CONFIRMATION_REQUIRED,
    CAPABILITY_REQUIRED,
    PROTECTED_APP,
    DEVICE_LOCKED,
    CALLER_SESSION_BLOCKED,
    SENSITIVE_DATA_BLOCKED,
    UNSUPPORTED,
    SECURITY_STATE_UNKNOWN
}

enum class SecuritySessionType { OWNER, CALL_CONVERSATION }

data class SecurityRequest(
    val action: String,
    val targetApp: String? = null,
    val sessionType: SecuritySessionType = SecuritySessionType.OWNER,
    val authenticated: Boolean = true,
    val confirmed: Boolean = false,
    val capabilityAvailable: Boolean = true,
    val deviceUnlocked: Boolean = true
)

data class SecurityAuditEvent(
    val timestamp: Long,
    val actionCategory: String,
    val decision: SecurityDecision,
    val reason: String,
    val applicationPackage: String?,
    val sessionType: SecuritySessionType
)

interface SecurityAuditLogger {
    fun record(event: SecurityAuditEvent)
    fun snapshot(): List<SecurityAuditEvent>
}

class BoundedSecurityAuditLogger(private val maxEntries: Int = 100) : SecurityAuditLogger {
    private val entries = ArrayDeque<SecurityAuditEvent>()
    override fun record(event: SecurityAuditEvent) = synchronized(entries) {
        entries.addLast(event)
        while (entries.size > maxEntries.coerceAtLeast(1)) entries.removeFirst()
    }
    override fun snapshot(): List<SecurityAuditEvent> = synchronized(entries) { entries.toList() }
}

class SecurityFirewall(
    private val securityManager: SecurityManager,
    private val protectedAppPolicy: ProtectedAppPolicy,
    private val automationPolicy: AutomationPolicy,
    private val auditLogger: SecurityAuditLogger = BoundedSecurityAuditLogger()
) {
    fun evaluate(request: SecurityRequest): SecurityDecision {
        val decision = when {
            request.action.isBlank() -> SecurityDecision.DENY
            request.sessionType == SecuritySessionType.CALL_CONVERSATION -> SecurityDecision.CALLER_SESSION_BLOCKED
            !request.authenticated -> SecurityDecision.AUTH_REQUIRED
            !request.capabilityAvailable -> SecurityDecision.CAPABILITY_REQUIRED
            !request.deviceUnlocked -> SecurityDecision.DEVICE_LOCKED
            containsSensitiveData(request.action) -> SecurityDecision.SENSITIVE_DATA_BLOCKED
            request.targetApp != null && protectedAppPolicy.isProtected(request.targetApp) -> SecurityDecision.PROTECTED_APP
            !securityManager.isActionAuthorized(request.action) -> SecurityDecision.DENY
            request.targetApp != null && automationPolicy.decision(request.targetApp, request.action) == AutomationDecision.BLOCKED -> SecurityDecision.DENY
            requiresConfirmation(request.action) && !request.confirmed -> SecurityDecision.CONFIRMATION_REQUIRED
            else -> SecurityDecision.ALLOW
        }
        auditLogger.record(SecurityAuditEvent(System.currentTimeMillis(), actionCategory(request.action), decision, reasonFor(decision), request.targetApp, request.sessionType))
        return decision
    }

    fun canExecute(request: SecurityRequest): Boolean = evaluate(request) == SecurityDecision.ALLOW

    fun auditSnapshot(): List<SecurityAuditEvent> = auditLogger.snapshot()

    /** Audit logs contain only a stable category, never command text or message content. */
    private fun actionCategory(action: String): String {
        val normalized = action.trim().lowercase()
        return when {
            normalized.isBlank() -> "unknown_action"
            normalized.contains("send") -> "send"
            normalized.contains("reply") -> "reply"
            normalized.contains("delete") || normalized.contains("remove") -> "delete"
            normalized.contains("post") || normalized.contains("publish") -> "publish"
            normalized.contains("call") -> "call"
            normalized.contains("open") || normalized.contains("launch") -> "open"
            normalized.contains("read") || normalized.contains("search") -> "read"
            normalized.contains("battery") -> "diagnostic_battery"
            normalized.contains("storage") -> "diagnostic_storage"
            normalized.contains("network") || normalized.contains("internet") -> "diagnostic_network"
            normalized.contains("diagnostic") || normalized.contains("status") -> "diagnostic"
            else -> "other"
        }
    }

    private fun requiresConfirmation(action: String): Boolean {
        val normalized = action.lowercase()
        return listOf("send", "reply", "delete", "remove", "post", "publish", "call").any(normalized::contains)
    }

    private fun containsSensitiveData(action: String): Boolean {
        val normalized = action.lowercase()
        val markers = listOf("otp", "one time password", "verification code", "authentication code", "recovery code", "password", "pin")
        return markers.any(normalized::contains)
    }

    private fun reasonFor(decision: SecurityDecision): String = when (decision) {
        SecurityDecision.ALLOW -> "allowed"
        SecurityDecision.DENY -> "security policy denied"
        SecurityDecision.AUTH_REQUIRED -> "owner authentication required"
        SecurityDecision.CONFIRMATION_REQUIRED -> "explicit confirmation required"
        SecurityDecision.CAPABILITY_REQUIRED -> "required capability unavailable"
        SecurityDecision.PROTECTED_APP -> "protected application"
        SecurityDecision.DEVICE_LOCKED -> "device authentication required"
        SecurityDecision.CALLER_SESSION_BLOCKED -> "caller conversation session cannot authorize device actions"
        SecurityDecision.SENSITIVE_DATA_BLOCKED -> "sensitive credential or verification data blocked"
        SecurityDecision.UNSUPPORTED -> "operation unsupported"
        SecurityDecision.SECURITY_STATE_UNKNOWN -> "security state could not be determined"
    }
}
