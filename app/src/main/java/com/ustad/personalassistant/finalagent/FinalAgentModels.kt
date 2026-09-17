package com.ustad.personalassistant.finalagent

import com.ustad.personalassistant.ai.ActionPlan
import com.ustad.personalassistant.ai.AiResponse
import com.ustad.personalassistant.permissions.Capability

/** Final orchestration session types. CALL_CONVERSATION can never authorize actions. */
enum class AssistantSessionType {
    OWNER_SESSION,
    BACKGROUND_ASSISTANT_SESSION,
    CALL_CONVERSATION_SESSION,
    UNAUTHENTICATED_SESSION
}

enum class ActionSensitivity { LOW, NORMAL, SENSITIVE, HIGHLY_SENSITIVE, PROTECTED }

enum class FinalInputType { TEXT, VOICE, WAKE_WORD, CALL_AUDIO_CONVERSATION }

enum class FinalResultStatus {
    SUCCESS,
    CHAT_RESPONSE,
    CONFIRMATION_REQUIRED,
    AUTH_REQUIRED,
    CAPABILITY_REQUIRED,
    SECURITY_BLOCKED,
    CALLER_SESSION_BLOCKED,
    PROTECTED_APP,
    SENSITIVE_DATA_BLOCKED,
    CANCELLED,
    DUPLICATE_REQUEST,
    NETWORK_UNAVAILABLE,
    AI_SERVICE_UNAVAILABLE,
    UNSUPPORTED,
    VERIFICATION_UNAVAILABLE,
    TIMEOUT,
    ERROR
}

data class AssistantSessionContext(
    val sessionId: String,
    val type: AssistantSessionType,
    val authenticated: Boolean,
    val voiceAuthenticated: Boolean = false,
    val deviceUnlocked: Boolean = true,
    val currentRequestId: String? = null,
    val cancellationRequested: Boolean = false
) {
    fun isPrivilegedOwner(): Boolean =
        type == AssistantSessionType.OWNER_SESSION && authenticated
}

data class FinalActionRequest(
    val requestId: String,
    val session: AssistantSessionContext,
    val actionPlan: ActionPlan,
    val sensitivity: ActionSensitivity,
    val confirmed: Boolean,
    val requiredCapabilities: List<Capability> = actionPlan.requiredCapabilities
)

data class FinalAgentResult(
    val status: FinalResultStatus,
    val response: AiResponse? = null,
    val message: String? = null,
    val requestId: String? = null
)
