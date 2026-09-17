package com.ustad.personalassistant.ai

import com.ustad.personalassistant.automation.AutomationPipeline
import com.ustad.personalassistant.automation.AutomationResult
import com.ustad.personalassistant.automation.AutomationResultStatus
import com.ustad.personalassistant.automation.ConfirmationDecision
import com.ustad.personalassistant.automation.ConfirmationPolicy
import com.ustad.personalassistant.permissions.Capability

class AiAutomationOrchestrator(
    private val brain: AiBrain,
    private val pipeline: AutomationPipeline,
    private val confirmationPolicy: ConfirmationPolicy
) {
    fun process(request: AiRequest): Result<AiResponse> {
        val response = brain.processRequest(request).getOrElse { return Result.failure(it) }
        val validated = response.validatedActionPlan()
        if (validated.isFailure) return Result.failure(AiException(AiErrorCode.INVALID_AI_RESPONSE))
        val plan = validated.getOrNull()
        if (plan == null) return Result.success(response)
        val decision = confirmationPolicy.decision(plan.action)
        if (decision == ConfirmationDecision.BLOCKED) return Result.failure(AiException(AiErrorCode.SECURITY_BLOCKED))
        if (decision == ConfirmationDecision.CONFIRM_BEFORE_SEND || plan.requiresConfirmation) {
            return Result.success(response.copy(requiresConfirmation = true))
        }
        return Result.success(response)
    }

    fun executeConfirmed(response: AiResponse): AutomationResult<Unit> {
        val plan = response.validatedActionPlan().getOrNull() ?: return AutomationResult(AutomationResultStatus.VERIFICATION_FAILED, message = AiErrorCode.INVALID_AI_RESPONSE.name)
        if (plan == null || !response.requiresConfirmation && confirmationPolicy.decision(plan.action) == ConfirmationDecision.CONFIRM_BEFORE_SEND) {
            return AutomationResult(AutomationResultStatus.SECURITY_BLOCKED, message = "Explicit confirmation required")
        }
        return pipeline.execute(plan.action, plan.target, plan.requiredCapabilities)
    }
}
