package com.ustad.personalassistant.part08

class GmailIntentParser {
    fun parse(text: String, authenticatedVoice: Boolean = false, confirmed: Boolean = false): GmailIntent? {
        val value = text.trim(); val lower = value.lowercase(); if (value.isBlank()) return null
        val operation = when {
            lower.contains("unread") && (lower.contains("count") || lower.contains("kitne") || lower.contains("kitni")) -> Part08Operation.COUNT_UNREAD_EMAIL
            lower.contains("summar") || lower.contains("summarize") || lower.contains("summary") -> Part08Operation.SUMMARIZE_EMAIL
            lower.contains("reply") || lower.contains("jawab") -> Part08Operation.REPLY_EMAIL
            lower.contains("compose") || lower.contains("send email") || lower.contains("gmail par") || lower.contains("mail bhej") -> Part08Operation.COMPOSE_EMAIL
            lower.contains("search") || lower.contains("email") || lower.contains("mail") -> Part08Operation.SEARCH_EMAIL
            else -> return null
        }
        val sender = Regex("(?:from|se|ke|ki)\\s+([A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+)", RegexOption.IGNORE_CASE).find(value)?.groupValues?.getOrNull(1)
        val subject = Regex("(?:subject|vishay)\\s+(.+?)(?:\\s+(?:bhej|send|dikhao|dekho)\\b|$)", RegexOption.IGNORE_CASE).find(value)?.groupValues?.getOrNull(1)?.trim()
        val recipient = Regex("(?:to|ko)\\s+([A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+)", RegexOption.IGNORE_CASE).find(value)?.groupValues?.getOrNull(1)
        val body = if (operation == Part08Operation.COMPOSE_EMAIL || operation == Part08Operation.REPLY_EMAIL) value.substringAfter("message", "").substringAfter("bolo", "").trim().ifBlank { null } else null
        return GmailIntent(operation, sender = sender, recipient = recipient, subject = subject, query = value.take(200), body = body?.take(20_000), authenticatedVoice = authenticatedVoice, confirmed = confirmed)
    }
}

class CallConversationFirewall {
    fun allowsOwnerAction(action: String): Boolean = false
    fun allowsMessaging(): Boolean = false
    fun allowsAccessibility(): Boolean = false
    fun allowsProtectedAppAction(): Boolean = false
}
