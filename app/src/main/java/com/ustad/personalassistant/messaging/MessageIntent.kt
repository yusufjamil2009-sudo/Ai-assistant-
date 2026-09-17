package com.ustad.personalassistant.messaging

data class MessageIntent(val type: com.ustad.personalassistant.ai.IntentType, val platform: MessagingPlatform? = null, val contact: String? = null, val messageText: String? = null, val conversation: String? = null, val messageId: String? = null, val attachmentType: AttachmentType? = null, val requiresConfirmation: Boolean = false)
class MessagingIntentParser {
    fun parse(text: String): MessageIntent {
        val t = text.trim(); val lower = t.lowercase(); val platform = when { lower.contains("whatsapp") -> MessagingPlatform.WHATSAPP; lower.contains("messenger") -> MessagingPlatform.MESSENGER; lower.contains("sms") || lower.contains("text message") -> MessagingPlatform.SMS; else -> null }
        if (lower == "cancel" || lower == "stop" || lower.contains("mat bhejo")) return MessageIntent(com.ustad.personalassistant.ai.IntentType.CANCEL_MESSAGE, platform)
        val type = when { lower.contains("summar") -> com.ustad.personalassistant.ai.IntentType.SUMMARIZE_MESSAGES; lower.contains("reply") || lower.contains("respond") -> com.ustad.personalassistant.ai.IntentType.REPLY_MESSAGE; lower.contains("send") || lower.contains("bhej") || lower.contains("bolo") -> com.ustad.personalassistant.ai.IntentType.SEND_MESSAGE; lower.contains("read") || lower.contains("batao") -> com.ustad.personalassistant.ai.IntentType.READ_MESSAGES; platform != null -> com.ustad.personalassistant.ai.IntentType.OPEN_MESSAGING_APP; else -> com.ustad.personalassistant.ai.IntentType.CHAT }
        val contact = Regex("(?i)^([a-z][a-z0-9 _-]{1,40}?)\\s+(?:to|ko)\\s+(?:whatsapp|messenger|sms)(?:\\s+(?:par|on))?").find(t)?.groupValues?.getOrNull(1)?.trim()
            ?: Regex("(?i)(?:to|ko|for)\\s+(?!whatsapp\\b|messenger\\b|sms\\b)([a-z][a-z0-9 _-]{1,40}?)(?=\\s+(?:on|par|via)\\b|\\s+(?:message|bolo|batao|bhejo)\\b|$)").find(t)?.groupValues?.getOrNull(1)?.trim()
            ?: Regex("(?i)(?:whatsapp|messenger)\\s+(?:par|on)\\s+([a-z][a-z0-9 _-]{1,40}?)(?=\\s+(?:ka|ke|ki)\\b|\\s+(?:message|batao|read)\\b|$)").find(t)?.groupValues?.getOrNull(1)?.trim()
        val message = Regex("(?i)(?:message|bolo|kehdo|say)\\s+(.+)$").find(t)?.groupValues?.getOrNull(1)?.trim()
        return MessageIntent(type, platform, contact, message, contact, requiresConfirmation = type == com.ustad.personalassistant.ai.IntentType.SEND_MESSAGE || type == com.ustad.personalassistant.ai.IntentType.REPLY_MESSAGE)
    }
}
