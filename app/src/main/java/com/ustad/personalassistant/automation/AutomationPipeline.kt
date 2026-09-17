package com.ustad.personalassistant.automation

import com.ustad.personalassistant.accessibility.AutomationDecision
import com.ustad.personalassistant.accessibility.AutomationPolicy
import com.ustad.personalassistant.capability.CapabilityEngine
import com.ustad.personalassistant.permissions.Capability
import com.ustad.personalassistant.security.SecurityManager

class AutomationPipeline(private val capabilityEngine: CapabilityEngine, private val securityManager: SecurityManager, private val automationPolicy: AutomationPolicy, private val actionExecutor: (String, String?) -> Result<Unit>, private val logger: AutomationLogger = InMemoryAutomationLogger()) {
    fun execute(action: String, targetApp: String?, capabilities: List<Capability>): AutomationResult<Unit> {
        val start = System.currentTimeMillis()
        fun finish(status: AutomationResultStatus): AutomationResult<Unit> {
            val duration = System.currentTimeMillis() - start
            logger.log(AutomationLogEntry(System.currentTimeMillis(), sanitizeActionName(action), targetApp, capabilities.firstOrNull()?.name, status.name, status, duration))
            return AutomationResult(status, durationMs = duration)
        }
        if (targetApp != null && (securityManager.isProtectedApp(targetApp) || automationPolicy.decision(targetApp, action) == AutomationDecision.BLOCKED)) return finish(AutomationResultStatus.SECURITY_BLOCKED)
        if (capabilities.any { !capabilityEngine.isAvailable(it) }) return finish(AutomationResultStatus.PERMISSION_REQUIRED)
        if (!securityManager.isActionAuthorized(action)) return finish(AutomationResultStatus.SECURITY_BLOCKED)
        return actionExecutor(action, targetApp).fold({ finish(AutomationResultStatus.SUCCESS) }, { finish(AutomationResultStatus.ERROR) })
    }
    private fun sanitizeActionName(action: String): String = action.substringBefore(":").substringBefore("|").trim().take(60).ifBlank { "automation_action" }
}
