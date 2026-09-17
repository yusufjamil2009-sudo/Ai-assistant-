package com.ustad.personalassistant.messaging

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import java.util.concurrent.ConcurrentHashMap

private abstract class BaseIntentMessagingAdapter(
    protected val context: Context,
    override val platform: MessagingPlatform,
    private val packageName: String
) : MessageProvider {
    override fun isAvailable(): Boolean = runCatching { context.packageManager.getApplicationInfo(packageName, 0); true }.getOrDefault(false)

    override fun openApp(): Result<Unit> = runCatching {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: error(MessageErrorCode.APP_NOT_INSTALLED.name)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override fun readMessages(contact: String?, limit: Int): Result<List<MessageRecord>> = Result.success(
        MessagingNotificationStore.recent(platform, contact, limit).map(SensitiveMessageDetector::redact)
    )

    protected fun openComposer(request: MessageSendRequest): MessageResult = runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, request.text)
            putExtra(Intent.EXTRA_TITLE, request.recipient.displayName)
            setPackage(packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) == null) throw IllegalStateException(MessageErrorCode.UNSUPPORTED_FEATURE.name)
        context.startActivity(intent)
        MessageResult(MessageState.SUCCESS, message = "Message composer opened; delivery cannot be independently verified.", verified = false)
    }.getOrElse { MessageResult(MessageState.FAILED, MessageErrorCode.SEND_FAILED, it.message) }
}

class WhatsAppAdapter(context: Context) : BaseIntentMessagingAdapter(context, MessagingPlatform.WHATSAPP, "com.whatsapp") {
    override fun send(request: MessageSendRequest): Result<MessageResult> = Result.success(openComposer(request))
}

class MessengerAdapter(context: Context) : BaseIntentMessagingAdapter(context, MessagingPlatform.MESSENGER, "com.facebook.orca") {
    override fun send(request: MessageSendRequest): Result<MessageResult> = Result.success(openComposer(request))
}

class SmsAdapter(private val context: Context) : MessageProvider {
    override val platform = MessagingPlatform.SMS
    override fun isAvailable(): Boolean = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_MESSAGING)

    override fun openApp(): Result<Unit> = runCatching {
        val intent = context.packageManager.getLaunchIntentForPackage(Telephony.Sms.getDefaultSmsPackage(context) ?: "")
            ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override fun readMessages(contact: String?, limit: Int): Result<List<MessageRecord>> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED || !isDefaultSmsApp()) {
            return Result.failure(IllegalStateException(MessageErrorCode.SMS_PERMISSION_OR_ROLE_UNAVAILABLE.name))
        }
        return runCatching {
            val result = mutableListOf<MessageRecord>()
            val projection = arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.THREAD_ID)
            context.contentResolver.query(Telephony.Sms.Inbox.CONTENT_URI, projection, null, null, Telephony.Sms.DATE + " DESC")?.use { cursor ->
                val id = cursor.getColumnIndex(Telephony.Sms._ID); val address = cursor.getColumnIndex(Telephony.Sms.ADDRESS); val body = cursor.getColumnIndex(Telephony.Sms.BODY); val date = cursor.getColumnIndex(Telephony.Sms.DATE); val thread = cursor.getColumnIndex(Telephony.Sms.THREAD_ID)
                while (cursor.moveToNext() && result.size < limit.coerceIn(1, 50)) {
                    val sender = cursor.getString(address).orEmpty(); if (!contact.isNullOrBlank() && !sender.contains(contact, true)) continue
                    val record = MessageRecord(MessagingPlatform.SMS, sender, cursor.getString(body).orEmpty(), cursor.getLong(date), cursor.getString(thread), cursor.getString(id))
                    result += SensitiveMessageDetector.redact(record)
                }
            }
            result
        }
    }

    override fun send(request: MessageSendRequest): Result<MessageResult> {
        if (!isAvailable() || ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) return Result.success(MessageResult(MessageState.FAILED, MessageErrorCode.SMS_PERMISSION_OR_ROLE_UNAVAILABLE))
        return runCatching {
            val manager = SmsManager.getDefault()
            val parts = manager.divideMessage(request.text)
            if (parts.size == 1) manager.sendTextMessage(request.recipient.phoneNumber, null, request.text, null, null)
            else manager.sendMultipartTextMessage(request.recipient.phoneNumber, null, parts, null, null)
            MessageResult(MessageState.SUCCESS, message = "SMS submission accepted by Android; delivery cannot be independently verified.", verified = false)
        }.getOrElse { MessageResult(MessageState.FAILED, MessageErrorCode.SEND_FAILED, it.message) }
    }

    private fun isDefaultSmsApp(): Boolean = Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
}

class DefaultMessagingEngine(
    private val context: Context,
    private val contacts: ContactResolver,
    private val aiBrain: com.ustad.personalassistant.ai.AiBrain,
    providers: List<MessageProvider>
) : MessagingEngine {
    private val providersByPlatform = providers.associateBy { it.platform }
    private val cancelled = ConcurrentHashMap.newKeySet<String>()
    private val completedActions = ConcurrentHashMap<String, Long>()
    private val cooldownMs = 5_000L

    override fun provider(platform: MessagingPlatform): MessageProvider? = providersByPlatform[platform]

    override fun resolveContact(query: String): Result<List<ContactMatch>> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return Result.failure(IllegalStateException(MessageErrorCode.CONTACTS_PERMISSION_DENIED.name))
        val matches = contacts.resolve(query)
        return when { matches.isEmpty() -> Result.failure(IllegalStateException(MessageErrorCode.RECIPIENT_NOT_FOUND.name)); matches.size > 1 -> Result.failure(IllegalStateException(MessageErrorCode.MULTIPLE_RECIPIENTS.name)); else -> Result.success(matches) }
    }

    override fun prepare(request: MessageSendRequest): MessageResult {
        if (request.sessionType == MessagingSessionType.CALL_CONVERSATION_SESSION) return MessageResult(MessageState.FAILED, MessageErrorCode.CALL_SESSION_BLOCKED)
        if (!request.authenticatedVoice) return MessageResult(MessageState.FAILED, MessageErrorCode.PROTECTED_ACTION, "Authenticated assistant voice session required.")
        if (request.text.isBlank() || SensitiveMessageDetector.isSensitive(request.text)) return MessageResult(MessageState.FAILED, MessageErrorCode.SENSITIVE_CONTENT_BLOCKED)
        if (request.attachments.isNotEmpty() && request.attachments.any { it.type != AttachmentType.TEXT }) return MessageResult(MessageState.FAILED, MessageErrorCode.UNSUPPORTED_FEATURE)
        if (request.confirmed) return MessageResult(MessageState.PREPARING_MESSAGE)
        return MessageResult(MessageState.WAITING_FOR_CONFIRMATION, message = "Confirmation required before sending.")
    }

    override fun send(request: MessageSendRequest): MessageResult {
        val prepared = prepare(request.copy(confirmed = false))
        if (prepared.state != MessageState.WAITING_FOR_CONFIRMATION) return prepared
        if (!request.confirmed) return prepared
        if (cancelled.contains(request.actionId)) return MessageResult(MessageState.CANCELLED, MessageErrorCode.USER_CANCELLED)
        val now = System.currentTimeMillis()
        val previous = completedActions[request.actionId]
        if (previous != null && now - previous < cooldownMs) return MessageResult(MessageState.FAILED, MessageErrorCode.DUPLICATE_ACTION)
        val provider = providersByPlatform[request.platform] ?: return MessageResult(MessageState.FAILED, MessageErrorCode.UNSUPPORTED_FEATURE)
        if (!provider.isAvailable()) return MessageResult(MessageState.FAILED, MessageErrorCode.APP_NOT_INSTALLED)
        if (cancelled.contains(request.actionId)) return MessageResult(MessageState.CANCELLED, MessageErrorCode.USER_CANCELLED)
        completedActions[request.actionId] = now
        val result = provider.send(request).getOrElse { MessageResult(MessageState.FAILED, MessageErrorCode.SEND_FAILED, it.message) }
        return if (result.state == MessageState.SUCCESS) result.copy(state = MessageState.SUCCESS, message = result.message ?: "Message request completed.") else result
    }

    override fun read(platform: MessagingPlatform, contact: String?, limit: Int): MessageResult {
        val provider = providersByPlatform[platform] ?: return MessageResult(MessageState.FAILED, MessageErrorCode.UNSUPPORTED_FEATURE)
        val records = provider.readMessages(contact, limit).getOrElse { return MessageResult(MessageState.FAILED, runCatching { MessageErrorCode.valueOf(it.message.orEmpty()) }.getOrDefault(MessageErrorCode.SEND_FAILED)) }
        return MessageResult(MessageState.SUCCESS, records = records)
    }

    override fun summarizeAccessible(platform: MessagingPlatform, contact: String?, limit: Int): Result<String> {
        val records = read(platform, contact, limit)
        if (records.state != MessageState.SUCCESS) return Result.failure(IllegalStateException(records.error?.name ?: MessageErrorCode.SEND_FAILED.name))
        val safe = records.records.filterNot { it.sensitive }.take(20)
        if (safe.isEmpty()) return Result.success("No non-sensitive accessible messages are available to summarize.")
        val compact = safe.joinToString("\n") { "${it.sender}: ${it.text.take(500)}" }
        return aiBrain.summarize(com.ustad.personalassistant.ai.AiRequest("Summarize these accessible ${platform.name} messages only. Do not infer missing content.\n$compact", setOf(com.ustad.personalassistant.ai.AiCapability.SUMMARIZATION, com.ustad.personalassistant.ai.AiCapability.TEXT_GENERATION))).map { it.text }
    }

    override fun cancel(actionId: String): MessageResult { cancelled += actionId; return MessageResult(MessageState.CANCELLED, MessageErrorCode.USER_CANCELLED) }
}
