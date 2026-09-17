package com.ustad.personalassistant.ai

import com.ustad.personalassistant.permissions.Capability

interface IntentParser { fun parse(text: String): IntentType }

class DefaultIntentParser : IntentParser {
    override fun parse(text: String): IntentType {
        val t = text.trim().lowercase()
        return when {
            t.startsWith("open ") || t.startsWith("launch ") || t.contains(" kholo") || t.endsWith(" khol") || t.startsWith("khol ") -> IntentType.OPEN_APP
            t.contains("notification") -> IntentType.READ_NOTIFICATION
            t.contains("email") && (t.contains("send") || t.contains("mail to")) -> IntentType.SEND_EMAIL
            t.contains("email") && t.contains("search") -> IntentType.SEARCH_EMAIL
            t.contains("email") && t.contains("summar") -> IntentType.SUMMARIZE_EMAIL
            t.contains("email") && t.contains("draft") -> IntentType.DRAFT_EMAIL
            t.contains("email") || t.contains("gmail") -> IntentType.READ_EMAIL
            t.contains("whatsapp") && (t.contains("reply") || t.contains("respond")) -> IntentType.REPLY_MESSAGE
            t.contains("whatsapp") && (t.contains("send") || t.contains("message")) -> IntentType.SEND_MESSAGE
            t.startsWith("call ") || t.contains("call ") -> IntentType.CALL_CONTACT
            t.contains("setting") -> IntentType.SETTINGS
            t.contains("diagnostic") || t.contains("battery") || t.contains("storage") -> IntentType.DEVICE_DIAGNOSTICS
            t.startsWith("search ") || t.contains("search for ") -> IntentType.SEARCH
            t.contains("summar") -> IntentType.SUMMARIZE
            else -> IntentType.CHAT
        }
    }
}

interface ActionPlanner { fun plan(response: AiResponse): ActionPlan? }

class DefaultActionPlanner(private val validator: ActionPlanValidator = DefaultActionPlanValidator()) : ActionPlanner {
    override fun plan(response: AiResponse): ActionPlan? {
        val candidate = response.actionPlan ?: return null
        if (!validator.validate(candidate)) return null
        return candidate
    }
}

object IntentCapabilities {
    fun required(intent: IntentType): List<Capability> = when (intent) {
        IntentType.OPEN_APP -> listOf(Capability.OPEN_APPS)
        IntentType.READ_NOTIFICATION, IntentType.SEND_MESSAGE, IntentType.REPLY_MESSAGE -> listOf(Capability.NOTIFICATION_ACCESS)
        IntentType.READ_EMAIL, IntentType.SEARCH_EMAIL, IntentType.SUMMARIZE_EMAIL, IntentType.DRAFT_EMAIL, IntentType.SEND_EMAIL -> listOf(Capability.GMAIL)
        IntentType.CALL_CONTACT -> listOf(Capability.PHONE_CALLS, Capability.CONTACTS)
        else -> emptyList()
    }
}

class InvalidAiResponseException : RuntimeException(AiErrorCode.INVALID_AI_RESPONSE.name)

fun AiResponse.validatedActionPlan(validator: ActionPlanValidator = DefaultActionPlanValidator()): Result<ActionPlan?> = runCatching {
    actionPlan?.let { if (validator.validate(it)) it else throw InvalidAiResponseException() }
}
