package com.ustad.personalassistant.services

import com.ustad.personalassistant.appcontrol.AndroidAppAutomationEngine
import com.ustad.personalassistant.appcontrol.AutomationEngineResult
import com.ustad.personalassistant.appcontrol.AutomationEngineStatus
import com.ustad.personalassistant.appcontrol.AutomationPlan
import com.ustad.personalassistant.appcontrol.AutomationStep
import com.ustad.personalassistant.capability.CapabilityEngine
import com.ustad.personalassistant.domain.UstadError
import com.ustad.personalassistant.domain.userMessage
import com.ustad.personalassistant.permissions.Capability
import com.ustad.personalassistant.security.SecurityManager

interface ActionExecutor {
    fun execute(action: String, packageName: String? = null): Result<Unit>
    fun execute(action: String, packageName: String? = null, requiredCapabilities: List<Capability>): Result<Unit> = execute(action, packageName)
}

class GuardedActionExecutor(
    private val securityManager: SecurityManager,
    private val capabilityEngine: CapabilityEngine? = null,
    private val appAutomationEngine: AndroidAppAutomationEngine? = null
) : ActionExecutor {
    override fun execute(action: String, packageName: String?): Result<Unit> {
        if (action.isBlank()) return Result.failure(IllegalArgumentException("Action is blank"))
        if (packageName != null && securityManager.isProtectedApp(packageName)) return Result.failure(IllegalStateException(UstadError.SecurityBlocked.userMessage()))
        if (!securityManager.isActionAuthorized(action)) return Result.failure(IllegalStateException(UstadError.AuthenticationRequired.userMessage()))
        val kind = action.substringBefore(":").trim().lowercase()
        if (appAutomationEngine != null && kind in setOf("open_app", "click", "set_text", "scroll_forward", "scroll_backward", "back", "read_visible", "cancel")) {
            val result: Result<Unit> = when (kind) {
                "open_app" -> appAutomationEngine.openApp(action.substringAfter(":").trim()).asUnitResult()
                "click" -> appAutomationEngine.execute(AutomationPlan(listOf(AutomationStep.FindAndClick(action.substringAfter(":").trim())))).asUnitResult()
                "set_text" -> appAutomationEngine.execute(AutomationPlan(listOf(AutomationStep.SetText(action.substringAfter(":").trim())))).asUnitResult()
                "scroll_forward" -> appAutomationEngine.execute(AutomationPlan(listOf(AutomationStep.Scroll(true)))).asUnitResult()
                "scroll_backward" -> appAutomationEngine.execute(AutomationPlan(listOf(AutomationStep.Scroll(false)))).asUnitResult()
                "back" -> appAutomationEngine.execute(AutomationPlan(listOf(AutomationStep.PressBack()))).asUnitResult()
                "read_visible" -> appAutomationEngine.execute(AutomationPlan(listOf(AutomationStep.ReadVisibleText()))).asUnitResult()
                else -> { appAutomationEngine.cancel(); Result.success(Unit) }
            }
            if (result.isFailure) return result
        }
        securityManager.audit("authorized action request")
        return Result.success(Unit)
    }

    override fun execute(action: String, packageName: String?, requiredCapabilities: List<Capability>): Result<Unit> {
        val engine = capabilityEngine ?: return Result.failure(IllegalStateException(UstadError.CapabilityUnavailable.userMessage()))
        val missing = requiredCapabilities.distinct().filterNot(engine::isAvailable)
        if (missing.isNotEmpty()) return Result.failure(IllegalStateException("Required capability unavailable: ${missing.joinToString { it.name }}"))
        return execute(action, packageName)
    }

    private fun <T> AutomationEngineResult<T>.asUnitResult(): Result<Unit> = if (status == AutomationEngineStatus.SUCCESS) Result.success(Unit) else Result.failure(IllegalStateException(status.name))
}
