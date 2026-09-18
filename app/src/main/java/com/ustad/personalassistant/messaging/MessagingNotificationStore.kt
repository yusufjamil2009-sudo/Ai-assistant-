package com.ustad.personalassistant.messaging

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification

object MessagingNotificationStore {
    private const val MAX_ITEMS = 100
    private val records = ArrayDeque<MessageRecord>()
    private val lock = Any()

    fun ingest(sbn: StatusBarNotification, smsPackage: String? = null) {
        val packageName = sbn.packageName.lowercase()
        val platform = when {
            packageName == "com.whatsapp" -> MessagingPlatform.WHATSAPP
            packageName == "com.facebook.orca" -> MessagingPlatform.MESSENGER
            !smsPackage.isNullOrBlank() && packageName == smsPackage.lowercase() -> MessagingPlatform.SMS
            else -> return
        }
        val extras: Bundle = sbn.notification.extras ?: return
        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()?.trim().orEmpty()
        if (sender.isBlank() && text.isBlank()) return
        val record = MessageRecord(platform, sender.ifBlank { platform.name }, text, sbn.postTime, sender)
        synchronized(lock) {
            records.removeAll { it.platform == record.platform && it.conversationKey == record.conversationKey && it.timestampMs == record.timestampMs }
            records.addLast(record)
            while (records.size > MAX_ITEMS) records.removeFirst()
        }
    }

    fun recent(platform: MessagingPlatform, contact: String? = null, limit: Int = 20): List<MessageRecord> = synchronized(lock) {
        records.toList().asReversed().asSequence().filter { it.platform == platform }.filter { contact.isNullOrBlank() || it.sender.contains(contact, true) }.take(limit.coerceIn(1, 50)).toList()
    }
}

object SensitiveMessageDetector {
    private val patterns = listOf(
        Regex("\\b(?:otp|one[- ]time password)\\b.{0,40}\\b\\d{4,8}\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:verification|auth(?:entication)?|recovery|security)\\s*(?:code|otp|password)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:password|passcode|pin)\\b", RegexOption.IGNORE_CASE)
    )
    fun isSensitive(text: String): Boolean = patterns.any { it.containsMatchIn(text) }
    fun redact(record: MessageRecord): MessageRecord = if (isSensitive(record.text)) record.copy(text = "[sensitive security content withheld]", sensitive = true) else record
}
