package com.ustad.personalassistant.appcontrol

import com.ustad.personalassistant.accessibility.AccessibilityActionEngine
import com.ustad.personalassistant.accessibility.AccessibilityActionStatus
import com.ustad.personalassistant.capability.CapabilityEngine
import com.ustad.personalassistant.permissions.Capability
import com.ustad.personalassistant.security.SecurityManager
import kotlinx.coroutines.CancellationException

interface ActionVerificationEngine {
    fun verifyForeground(packageName: String, timeoutMs: Long = 3_000L): Boolean
    fun verifyVisibleText(text: String, timeoutMs: Long = 3_000L): Boolean
}

class AndroidActionVerificationEngine(private val accessibility: AccessibilityActionEngine) : ActionVerificationEngine {
    override fun verifyForeground(packageName: String, timeoutMs: Long): Boolean {
        val end = System.currentTimeMillis() + timeoutMs.coerceIn(0L, 10_000L)
        while (System.currentTimeMillis() <= end) {
            val snapshot = accessibility.snapshot()
            if (snapshot.isSuccess && snapshot.value?.packageName == packageName) return true
            Thread.sleep(100L)
        }
        return false
    }
    override fun verifyVisibleText(text: String, timeoutMs: Long): Boolean = accessibility.verifyAction(text, timeoutMs).isSuccess
}

class AndroidAppAutomationEngine(
    private val resolver: AppResolver,
    private val accessibility: AccessibilityActionEngine,
    private val verification: ActionVerificationEngine,
    private val capabilityEngine: CapabilityEngine,
    private val securityManager: SecurityManager,
    private val policy: com.ustad.personalassistant.accessibility.AutomationPolicy,
    private val adapters: List<AppAutomationAdapter> = listOf(GenericAndroidAppAdapter())
) {
    @Volatile private var cancelled = false
    fun cancel() { cancelled = true; accessibility.cancelPendingOperations() }
    fun resetCancellation() { cancelled = false }

    fun openApp(query: String): AutomationEngineResult<Unit> {
        resetCancellation()
        val resolved = resolver.resolve(query)
        if (resolved.status == AppResolveStatus.NOT_INSTALLED || resolved.apps.isEmpty()) return AutomationEngineResult(AutomationEngineStatus.APP_NOT_FOUND)
        if (resolved.status == AppResolveStatus.AMBIGUOUS) return AutomationEngineResult(AutomationEngineStatus.AMBIGUOUS_APP, message = resolved.apps.joinToString { it.label })
        val app = resolved.apps.single()
        if (!preflight(app.packageName, "open_app")) return AutomationEngineResult(AutomationEngineStatus.SECURITY_BLOCKED)
        if (cancelled) return AutomationEngineResult(AutomationEngineStatus.CANCELLED)
        val result = accessibility.openApp(app.packageName)
        if (!result.isSuccess) return map(result.status)
        return if (verification.verifyForeground(app.packageName, 5_000L)) AutomationEngineResult(AutomationEngineStatus.SUCCESS)
        else AutomationEngineResult(AutomationEngineStatus.VERIFICATION_FAILED)
    }

    fun execute(plan: AutomationPlan): AutomationEngineResult<List<String>> {
        resetCancellation()
        val outputs = mutableListOf<String>()
        var completed = 0
        return try {
            for (step in plan.steps) {
                if (cancelled) return AutomationEngineResult(AutomationEngineStatus.CANCELLED, completedSteps = completed)
                val result: AutomationEngineResult<*> = when (step) {
                    is AutomationStep.OpenApp -> openApp(step.query)
                    is AutomationStep.FindAndClick -> click(step.text, step.timeoutMs, step.expectedText)
                    is AutomationStep.SetText -> setText(step.text, step.timeoutMs, step.expectedText)
                    is AutomationStep.Scroll -> scroll(step.forward, step.maxAttempts, step.timeoutMs, step.expectedText)
                    is AutomationStep.PressBack -> back(step.timeoutMs, step.expectedText)
                    is AutomationStep.ReadVisibleText -> read(step.timeoutMs)
                }
                if (result.status != AutomationEngineStatus.SUCCESS) return AutomationEngineResult(result.status, outputs, result.message, completed)
                if (result.value is String) outputs += result.value as String
                completed++
            }
            AutomationEngineResult(AutomationEngineStatus.SUCCESS, outputs, completedSteps = completed)
        } catch (_: CancellationException) { AutomationEngineResult(AutomationEngineStatus.CANCELLED, outputs, completedSteps = completed) }
          catch (_: Exception) { AutomationEngineResult(AutomationEngineStatus.ERROR, outputs, completedSteps = completed) }
    }

    private fun click(text: String, timeout: Long, expected: String?): AutomationEngineResult<String> {
        if (!preflight(null, "click:$text")) return AutomationEngineResult(AutomationEngineStatus.SECURITY_BLOCKED)
        if (!accessibility.waitForNode(text, timeout).isSuccess) return AutomationEngineResult(AutomationEngineStatus.NODE_NOT_FOUND)
        val result = accessibility.clickNode(text)
        if (!result.isSuccess) return map(result.status)
        return if (expected == null || verification.verifyVisibleText(expected, timeout)) AutomationEngineResult(AutomationEngineStatus.SUCCESS)
        else AutomationEngineResult(AutomationEngineStatus.VERIFICATION_FAILED)
    }

    private fun setText(text: String, timeout: Long, expected: String?): AutomationEngineResult<String> {
        if (!preflight(null, "set_text")) return AutomationEngineResult(AutomationEngineStatus.SECURITY_BLOCKED)
        if (!accessibility.findEditableField().isSuccess) return AutomationEngineResult(AutomationEngineStatus.NODE_NOT_FOUND)
        val result = accessibility.setText(text)
        if (!result.isSuccess) return map(result.status)
        return if (expected == null || verification.verifyVisibleText(expected, timeout)) AutomationEngineResult(AutomationEngineStatus.SUCCESS)
        else AutomationEngineResult(AutomationEngineStatus.VERIFICATION_FAILED)
    }

    private fun scroll(forward: Boolean, attempts: Int, timeout: Long, expected: String?): AutomationEngineResult<String> {
        if (!preflight(null, "scroll")) return AutomationEngineResult(AutomationEngineStatus.SECURITY_BLOCKED)
        var lastStatus = AccessibilityActionStatus.ACTION_NOT_SUPPORTED
        repeat(attempts.coerceIn(1, 10)) {
            if (cancelled) return AutomationEngineResult(AutomationEngineStatus.CANCELLED)
            val result = if (forward) accessibility.scrollForward() else accessibility.scrollBackward()
            lastStatus = result.status
            if (!result.isSuccess) return@repeat
            if (expected == null || verification.verifyVisibleText(expected, timeout)) return AutomationEngineResult(AutomationEngineStatus.SUCCESS)
        }
        return if (expected == null) map(lastStatus) else AutomationEngineResult(AutomationEngineStatus.TIMEOUT)
    }

    private fun back(timeout: Long, expected: String?): AutomationEngineResult<String> {
        if (!preflight(null, "back")) return AutomationEngineResult(AutomationEngineStatus.SECURITY_BLOCKED)
        val result = accessibility.pressBack()
        if (!result.isSuccess) return map(result.status)
        return if (expected == null || verification.verifyVisibleText(expected, timeout)) AutomationEngineResult(AutomationEngineStatus.SUCCESS)
        else AutomationEngineResult(AutomationEngineStatus.VERIFICATION_FAILED)
    }

    private fun read(timeout: Long): AutomationEngineResult<String> {
        if (!preflight(null, "read_visible")) return AutomationEngineResult(AutomationEngineStatus.SECURITY_BLOCKED)
        val result = accessibility.readVisibleText()
        return if (result.isSuccess) AutomationEngineResult(AutomationEngineStatus.SUCCESS, result.value?.joinToString("\n")) else map(result.status)
    }

    private fun preflight(packageName: String?, action: String): Boolean {
        if (!capabilityEngine.isAvailable(Capability.APP_CONTROL)) return false
        if (!securityManager.isActionAuthorized(action)) return false
        if (packageName != null && securityManager.isProtectedApp(packageName)) return false
        if (packageName != null && policy.decision(packageName, action) == com.ustad.personalassistant.accessibility.AutomationDecision.BLOCKED) return false
        return packageName == null || adapters.any { it.supports(packageName) }
    }

    private fun <T> map(status: AccessibilityActionStatus): AutomationEngineResult<T> = AutomationEngineResult(
        when (status) {
            AccessibilityActionStatus.SUCCESS -> AutomationEngineStatus.SUCCESS
            AccessibilityActionStatus.NODE_NOT_FOUND -> AutomationEngineStatus.NODE_NOT_FOUND
            AccessibilityActionStatus.ACTION_NOT_SUPPORTED -> AutomationEngineStatus.ACTION_NOT_SUPPORTED
            AccessibilityActionStatus.SERVICE_DISABLED -> AutomationEngineStatus.SERVICE_DISABLED
            AccessibilityActionStatus.TIMEOUT -> AutomationEngineStatus.TIMEOUT
            AccessibilityActionStatus.SECURITY_BLOCKED -> AutomationEngineStatus.SECURITY_BLOCKED
            AccessibilityActionStatus.PERMISSION_REQUIRED -> AutomationEngineStatus.PERMISSION_REQUIRED
            AccessibilityActionStatus.VERIFICATION_FAILED -> AutomationEngineStatus.VERIFICATION_FAILED
            AccessibilityActionStatus.APP_NOT_SUPPORTED -> AutomationEngineStatus.ERROR
            AccessibilityActionStatus.ERROR -> AutomationEngineStatus.ERROR
        }
    )
}
