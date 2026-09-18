package com.ustad.personalassistant.ai

import com.ustad.personalassistant.automation.AutomationPipeline
import com.ustad.personalassistant.automation.AutomationResult
import com.ustad.personalassistant.automation.AutomationResultStatus
import com.ustad.personalassistant.automation.ConfirmationDecision
import com.ustad.personalassistant.automation.ConfirmationPolicy
import com.ustad.personalassistant.permissions.Capability
import com.ustad.personalassistant.finalagent.AgentOrchestrator
import com.ustad.personalassistant.finalagent.AssistantSessionContext
import com.ustad.personalassistant.finalagent.AssistantSessionType
import com.ustad.personalassistant.finalagent.FinalResultStatus
import java.util.UUID

class AiAutomationOrchestrator(
    private val brain: AiBrain,
    private val pipeline: AutomationPipeline,
    private val confirmationPolicy: ConfirmationPolicy,
    private val finalAgent: AgentOrchestrator? = null
) {
    @Volatile private var callConversationMode = false

    fun setCallConversationMode(enabled: Boolean) {
        callConversationMode = enabled
    }

    fun process(request: AiRequest): Result<AiResponse> {
        finalAgent?.let { agent ->
            val session = AssistantSessionContext(
                sessionId = UUID.randomUUID().toString(),
                type = if (callConversationMode) AssistantSessionType.CALL_CONVERSATION_SESSION else AssistantSessionType.OWNER_SESSION,
                authenticated = !callConversationMode,
                voiceAuthenticated = !callConversationMode
            )
            return when (val result = agent.processText(request.text, session)) {
                is com.ustad.personalassistant.finalagent.FinalAgentResult ->
                    when (result.status) {
                        FinalResultStatus.SUCCESS,
                        FinalResultStatus.CHAT_RESPONSE -> Result.success(result.response ?: AiResponse(result.message.orEmpty()))
                        FinalResultStatus.CONFIRMATION_REQUIRED -> Result.success((result.response ?: AiResponse(result.message.orEmpty())).copy(requiresConfirmation = true))
                        else -> Result.failure(AiException(AiErrorCode.SECURITY_BLOCKED))
                    }
            }
        }
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
        finalAgent?.let { agent ->
            val result = agent.executeConfirmed(
                response,
                AssistantSessionContext(
                    sessionId = UUID.randomUUID().toString(),
                    type = AssistantSessionType.OWNER_SESSION,
                    authenticated = true,
                    voiceAuthenticated = true
                ),
                response.actionPlan?.action ?: UUID.randomUUID().toString()
            )
            return when (result.status) {
                FinalResultStatus.SUCCESS -> AutomationResult(AutomationResultStatus.SUCCESS, message = result.message)
                FinalResultStatus.AUTH_REQUIRED -> AutomationResult(AutomationResultStatus.AUTH_REQUIRED, message = result.message)
                FinalResultStatus.CAPABILITY_REQUIRED -> AutomationResult(AutomationResultStatus.PERMISSION_REQUIRED, message = result.message)
                FinalResultStatus.SECURITY_BLOCKED, FinalResultStatus.PROTECTED_APP, FinalResultStatus.SENSITIVE_DATA_BLOCKED -> AutomationResult(AutomationResultStatus.SECURITY_BLOCKED, message = result.message)
                FinalResultStatus.TIMEOUT -> AutomationResult(AutomationResultStatus.TIMEOUT, message = result.message)
                FinalResultStatus.NETWORK_UNAVAILABLE -> AutomationResult(AutomationResultStatus.NETWORK_ERROR, message = result.message)
                FinalResultStatus.VERIFICATION_UNAVAILABLE -> AutomationResult(AutomationResultStatus.VERIFICATION_FAILED, message = result.message)
                else -> AutomationResult(AutomationResultStatus.ERROR, message = result.message)
            }
        }
        val plan = response.validatedActionPlan().getOrNull() ?: return AutomationResult(AutomationResultStatus.VERIFICATION_FAILED, message = AiErrorCode.INVALID_AI_RESPONSE.name)
        if (plan == null || !response.requiresConfirmation && confirmationPolicy.decision(plan.action) == ConfirmationDecision.CONFIRM_BEFORE_SEND) {
            return AutomationResult(AutomationResultStatus.SECURITY_BLOCKED, message = "Explicit confirmation required")
        }
        return pipeline.execute(plan.action, plan.target, plan.requiredCapabilities)
    }
}
