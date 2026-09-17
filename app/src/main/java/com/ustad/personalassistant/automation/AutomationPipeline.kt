package com.ustad.personalassistant.automation

import com.ustad.personalassistant.accessibility.AutomationDecision
import com.ustad.personalassistant.accessibility.AutomationPolicy
import com.ustad.personalassistant.capability.CapabilityEngine
import com.ustad.personalassistant.permissions.Capability
import com.ustad.personalassistant.security.SecurityDecision
import com.ustad.personalassistant.security.SecurityFirewall
import com.ustad.personalassistant.security.SecurityRequest
import com.ustad.personalassistant.security.SecuritySessionType
import com.ustad.personalassistant.security.DefaultProtectedAppPolicy
import com.ustad.personalassistant.security.SecurityManager

class AutomationPipeline(
    private val capabilityEngine: CapabilityEngine,
    private val securityManager: SecurityManager,
    private val automationPolicy: AutomationPolicy,
    private val actionExecutor: (String, String?) -> Result<Unit>,
    private val logger: AutomationLogger = InMemoryAutomationLogger(),
    private val securityFirewall: SecurityFirewall = SecurityFirewall(securityManager, DefaultProtectedAppPolicy(), automationPolicy)
) {
    fun execute(
        action: String,
        targetApp: String?,
        capabilities: List<Capability>,
        sessionType: SecuritySessionType = SecuritySessionType.OWNER
    ): AutomationResult<Unit> {
        val start = System.currentTimeMillis()
        fun finish(status: AutomationResultStatus, message: String? = null): AutomationResult<Unit> {
            val duration = System.currentTimeMillis() - start
            logger.log(AutomationLogEntry(System.currentTimeMillis(), sanitizeActionName(action), targetApp, capabilities.firstOrNull()?.name, status.name, status, duration))
            return AutomationResult(status, durationMs = duration, message = message)
        }
        if (capabilities.any { !capabilityEngine.isAvailable(it) }) return finish(AutomationResultStatus.PERMISSION_REQUIRED)

        val securityDecision = securityFirewall.evaluate(
            SecurityRequest(
                action = action,
                targetApp = targetApp,
                sessionType = sessionType,
                authenticated = sessionType == SecuritySessionType.OWNER,
                confirmed = true,
                capabilityAvailable = true,
                deviceUnlocked = true
            )
        )
        if (securityDecision != SecurityDecision.ALLOW) {
            val status = when (securityDecision) {
                SecurityDecision.AUTH_REQUIRED -> AutomationResultStatus.AUTH_REQUIRED
                SecurityDecision.CAPABILITY_REQUIRED -> AutomationResultStatus.PERMISSION_REQUIRED
                SecurityDecision.CONFIRMATION_REQUIRED -> AutomationResultStatus.AUTH_REQUIRED
                else -> AutomationResultStatus.SECURITY_BLOCKED
            }
            return finish(status, securityDecision.name)
        }
        if (targetApp != null && securityManager.isProtectedApp(targetApp)) return finish(AutomationResultStatus.SECURITY_BLOCKED, SecurityDecision.PROTECTED_APP.name)
        if (targetApp != null && automationPolicy.decision(targetApp, action) == AutomationDecision.BLOCKED) return finish(AutomationResultStatus.SECURITY_BLOCKED)
        return actionExecutor(action, targetApp).fold({ finish(AutomationResultStatus.SUCCESS) }, { finish(AutomationResultStatus.ERROR) })
    }

    private fun sanitizeActionName(action: String): String = action.substringBefore(":").substringBefore("|").trim().take(60).ifBlank { "automation_action" }
}
