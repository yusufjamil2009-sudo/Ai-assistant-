package com.ustad.personalassistant.ai

import com.ustad.personalassistant.permissions.Capability

enum class AiCapability { TEXT_GENERATION, CHAT, JSON_OUTPUT, TOOL_PLANNING, LONG_CONTEXT, VISION, STREAMING, FUNCTION_CALLING, REASONING, SUMMARIZATION }
enum class OnDeviceAiState { AVAILABLE, UNAVAILABLE, INITIALIZING, ERROR, NOT_SUPPORTED }
enum class RoutingPolicy { PRIVACY_FIRST, BALANCED, CLOUD_FIRST }
enum class IntentType { CHAT, OPEN_APP, READ_NOTIFICATION, SEND_MESSAGE, READ_EMAIL, SEARCH_EMAIL, SUMMARIZE_EMAIL, DRAFT_EMAIL, SEND_EMAIL, REPLY_MESSAGE, CALL_CONTACT, DEVICE_DIAGNOSTICS, SETTINGS, SEARCH, SUMMARIZE, UNKNOWN }
enum class EntityType { APP_NAME, CONTACT_NAME, MESSAGE_TEXT, EMAIL_ADDRESS, EMAIL_SUBJECT, SEARCH_QUERY, LOCATION, DATE, TIME }

data class AiEntity(val type: EntityType, val value: String, val confidence: Double = 1.0)
data class ActionPlan(val action: String, val target: String? = null, val requiredCapabilities: List<Capability> = emptyList(), val requiresConfirmation: Boolean = false)
data class AiUsage(val inputTokens: Long? = null, val outputTokens: Long? = null, val totalTokens: Long? = null, val estimatedCost: Double? = null)
data class AiError(val code: AiErrorCode, val message: String)
data class AiResponse(val text: String, val intent: IntentType = IntentType.CHAT, val confidence: Double = 0.0, val entities: List<AiEntity> = emptyList(), val actionPlan: ActionPlan? = null, val requiresConfirmation: Boolean = false, val providerId: String? = null, val model: String? = null, val usage: AiUsage? = null, val error: AiError? = null)
enum class AiErrorCode { INVALID_AI_RESPONSE, NO_PROVIDER_AVAILABLE, PROVIDER_TIMEOUT, PROVIDER_RATE_LIMITED, PROVIDER_AUTH_ERROR, PROVIDER_NETWORK_ERROR, PROVIDER_SERVER_ERROR, PROVIDER_INVALID_RESPONSE, PROVIDER_UNSUPPORTED_CAPABILITY, ALL_PROVIDERS_FAILED, OFFLINE, ON_DEVICE_UNAVAILABLE }

enum class AiStreamEventType { START, TOKEN, PROGRESS, TOOL_INTENT, COMPLETE, ERROR }
data class AiStreamEvent(val type: AiStreamEventType, val text: String? = null, val response: AiResponse? = null, val error: AiError? = null)

data class AiRequest(val text: String, val requiredCapabilities: Set<AiCapability> = setOf(AiCapability.CHAT), val context: ConversationContext = ConversationContext())
data class ConversationContext(val recentMessages: List<String> = emptyList(), val currentTask: String? = null, val currentAction: String? = null, val currentApp: String? = null, val previousIntent: IntentType? = null, val entities: List<AiEntity> = emptyList(), val maxMessages: Int = 12) {
    fun bounded(): ConversationContext = copy(recentMessages = recentMessages.takeLast(maxMessages.coerceIn(1, 50)))
}
