package com.ustad.personalassistant.finalagent

import com.ustad.personalassistant.ai.AiBrain
import com.ustad.personalassistant.ai.AiRequest
import com.ustad.personalassistant.ai.AiResponse
import com.ustad.personalassistant.automation.AutomationPipeline
import com.ustad.personalassistant.automation.AutomationResult
import com.ustad.personalassistant.automation.AutomationResultStatus
import com.ustad.personalassistant.automation.ConfirmationDecision
import com.ustad.personalassistant.automation.ConfirmationPolicy
import com.ustad.personalassistant.capability.CapabilityEngine
import com.ustad.personalassistant.security.SecurityDecision
import com.ustad.personalassistant.security.SecurityFirewall
import com.ustad.personalassistant.security.SecurityRequest
import com.ustad.personalassistant.security.SecuritySessionType
import java.util.UUID

/** Final single entry point for text/voice-driven orchestration. Existing engines remain the executors. */
class AgentOrchestrator(
    private val brain: AiBrain,
    private val pipeline: AutomationPipeline,
    private val capabilityEngine: CapabilityEngine,
    private val securityFirewall: SecurityFirewall,
    private val confirmationPolicy: ConfirmationPolicy,
    private val registry: ActionRequestRegistry = ActionRequestRegistry()
) {
    fun processText(
        text: String,
        session: AssistantSessionContext,
        confirmed: Boolean = false
    ): FinalAgentResult {
        if (text.isBlank()) return FinalAgentResult(FinalResultStatus.ERROR, message = "Empty request")
        if (IntentNormalizer.isCancellation(text)) {
            val id = session.currentRequestId
            return if (id != null && registry.cancel(id)) {
                FinalAgentResult(FinalResultStatus.CANCELLED, message = "Request cancelled", requestId = id)
            } else {
                FinalAgentResult(FinalResultStatus.CANCELLED, message = "No pending request")
            }
        }

        val normalized = IntentNormalizer.normalize(text)
        val request = AiRequest(normalized)
        val response = brain.processRequest(request).getOrElse {
            return FinalAgentResult(FinalResultStatus.AI_SERVICE_UNAVAILABLE, message = it.message)
        }
        val plan = response.actionPlan ?: return FinalAgentResult(FinalResultStatus.CHAT_RESPONSE, response = response)
        if (plan.action.isBlank()) return FinalAgentResult(FinalResultStatus.ERROR, message = "Invalid action plan")

        val requestId = UUID.randomUUID().toString()
        val sensitivity = sensitivityFor(plan.action)
        val requiresConfirmation = plan.requiresConfirmation || confirmationPolicy.decision(plan.action) == ConfirmationDecision.CONFIRM_BEFORE_SEND

        if (session.type == AssistantSessionType.CALL_CONVERSATION_SESSION) {
            return FinalAgentResult(FinalResultStatus.CALLER_SESSION_BLOCKED, response, "Caller conversation cannot authorize device actions", requestId)
        }
        if (session.type == AssistantSessionType.UNAUTHENTICATED_SESSION || !session.authenticated) {
            return FinalAgentResult(FinalResultStatus.AUTH_REQUIRED, response, "Owner authentication required", requestId)
        }
        if (sensitivity == ActionSensitivity.PROTECTED) {
            return FinalAgentResult(FinalResultStatus.PROTECTED_APP, response, "Protected action blocked", requestId)
        }
        if (requiresConfirmation && !confirmed) {
            registry.putPending(requestId)
            return FinalAgentResult(FinalResultStatus.CONFIRMATION_REQUIRED, response.copy(requestId = requestId), "Explicit owner confirmation required", requestId)
        }
        if (!capabilityEngine.areAvailable(plan.requiredCapabilities)) {
            return FinalAgentResult(FinalResultStatus.CAPABILITY_REQUIRED, response, "Required capability unavailable", requestId)
        }

        val decision = securityFirewall.evaluate(
            SecurityRequest(
                action = plan.action,
                targetApp = plan.target,
                sessionType = SecuritySessionType.OWNER,
                authenticated = session.authenticated,
                confirmed = confirmed || !requiresConfirmation,
                capabilityAvailable = true,
                deviceUnlocked = session.deviceUnlocked
            )
        )
        val blocked = mapSecurityDecision(decision)
        if (blocked != null) return FinalAgentResult(blocked, response, decision.name, requestId)

        registry.putPending(requestId)
        if (!registry.beginExecution(requestId)) return FinalAgentResult(FinalResultStatus.DUPLICATE_REQUEST, response, "Request is no longer executable", requestId)
        val automation = pipeline.execute(plan.action, plan.target, plan.requiredCapabilities, SecuritySessionType.OWNER)
        registry.complete(requestId)
        return FinalAgentResult(mapAutomationStatus(automation.status), response, automation.message, requestId)
    }

    fun executeConfirmed(
        response: AiResponse,
        session: AssistantSessionContext,
        requestId: String
    ): FinalAgentResult {
        val plan = response.actionPlan ?: return FinalAgentResult(FinalResultStatus.ERROR, response, "No executable action", requestId)
        if (session.type == AssistantSessionType.CALL_CONVERSATION_SESSION) return FinalAgentResult(FinalResultStatus.CALLER_SESSION_BLOCKED, response, "Caller confirmation is not authorization", requestId)
        if (!session.isPrivilegedOwner()) return FinalAgentResult(FinalResultStatus.AUTH_REQUIRED, response, "Owner authentication required", requestId)
        if (registry.state(requestId) != ActionRequestRegistry.State.PENDING_CONFIRMATION) return FinalAgentResult(FinalResultStatus.DUPLICATE_REQUEST, response, "Confirmation is stale or already consumed", requestId)
        if (!registry.beginExecution(requestId)) return FinalAgentResult(FinalResultStatus.DUPLICATE_REQUEST, response, "Request is already executing or completed", requestId)
        val decision = securityFirewall.evaluate(
            SecurityRequest(
                action = plan.action,
                targetApp = plan.target,
                sessionType = SecuritySessionType.OWNER,
                authenticated = true,
                confirmed = true,
                capabilityAvailable = capabilityEngine.areAvailable(plan.requiredCapabilities),
                deviceUnlocked = session.deviceUnlocked
            )
        )
        if (decision != SecurityDecision.ALLOW) {
            registry.cancel(requestId)
            return FinalAgentResult(mapSecurityDecision(decision) ?: FinalResultStatus.SECURITY_BLOCKED, response, decision.name, requestId)
        }
        val automation = pipeline.execute(plan.action, plan.target, plan.requiredCapabilities, SecuritySessionType.OWNER)
        registry.complete(requestId)
        return FinalAgentResult(mapAutomationStatus(automation.status), response, automation.message, requestId)
    }

    fun cancel(requestId: String): Boolean = registry.cancel(requestId)

    private fun sensitivityFor(action: String): ActionSensitivity {
        val a = action.lowercase()
        if (listOf("bank", "banking", "upi", "payment", "wallet", "finance", "phonepe", "paytm", "gpay", "transfer").any(a::contains)) return ActionSensitivity.PROTECTED
        if (listOf("password", "pin", "otp", "verification code", "recovery code", "biometric").any(a::contains)) return ActionSensitivity.HIGHLY_SENSITIVE
        if (listOf("send", "reply", "delete", "remove", "post", "publish", "call").any(a::contains)) return ActionSensitivity.SENSITIVE
        if (listOf("open", "launch", "read", "search", "status", "diagnostic").any(a::contains)) return ActionSensitivity.NORMAL
        return ActionSensitivity.LOW
    }

    private fun mapSecurityDecision(decision: SecurityDecision): FinalResultStatus? = when (decision) {
        SecurityDecision.ALLOW -> null
        SecurityDecision.AUTH_REQUIRED -> FinalResultStatus.AUTH_REQUIRED
        SecurityDecision.CAPABILITY_REQUIRED -> FinalResultStatus.CAPABILITY_REQUIRED
        SecurityDecision.CONFIRMATION_REQUIRED -> FinalResultStatus.CONFIRMATION_REQUIRED
        SecurityDecision.PROTECTED_APP -> FinalResultStatus.PROTECTED_APP
        SecurityDecision.DEVICE_LOCKED -> FinalResultStatus.AUTH_REQUIRED
        SecurityDecision.CALLER_SESSION_BLOCKED -> FinalResultStatus.CALLER_SESSION_BLOCKED
        SecurityDecision.SENSITIVE_DATA_BLOCKED -> FinalResultStatus.SENSITIVE_DATA_BLOCKED
        SecurityDecision.UNSUPPORTED -> FinalResultStatus.UNSUPPORTED
        SecurityDecision.SECURITY_STATE_UNKNOWN, SecurityDecision.DENY -> FinalResultStatus.SECURITY_BLOCKED
    }

    private fun mapAutomationStatus(status: AutomationResultStatus): FinalResultStatus = when (status) {
        AutomationResultStatus.SUCCESS -> FinalResultStatus.SUCCESS
        AutomationResultStatus.AUTH_REQUIRED -> FinalResultStatus.AUTH_REQUIRED
        AutomationResultStatus.PERMISSION_REQUIRED -> FinalResultStatus.CAPABILITY_REQUIRED
        AutomationResultStatus.SECURITY_BLOCKED -> FinalResultStatus.SECURITY_BLOCKED
        AutomationResultStatus.TIMEOUT -> FinalResultStatus.TIMEOUT
        AutomationResultStatus.NETWORK_ERROR -> FinalResultStatus.NETWORK_UNAVAILABLE
        AutomationResultStatus.VERIFICATION_FAILED -> FinalResultStatus.VERIFICATION_UNAVAILABLE
        else -> FinalResultStatus.ERROR
    }
}

private fun CapabilityEngine.areAvailable(capabilities: List<com.ustad.personalassistant.permissions.Capability>): Boolean =
    capabilities.all(::isAvailable)
