package com.ustad.personalassistant.messaging

enum class MessagingPlatform { WHATSAPP, MESSENGER, SMS }
enum class MessageState { IDLE, RESOLVING_CONTACT, PREPARING_MESSAGE, WAITING_FOR_CONFIRMATION, SENDING, VERIFYING, SUCCESS, FAILED, CANCELLED }
enum class MessagingSessionType { NORMAL_ASSISTANT_SESSION, CALL_CONVERSATION_SESSION }
enum class AttachmentType { TEXT, IMAGE, FILE, DOCUMENT }
enum class MessageErrorCode { APP_NOT_INSTALLED, NOTIFICATION_ACCESS_DISABLED, ACCESSIBILITY_DISABLED, CONTACTS_PERMISSION_DENIED, SMS_PERMISSION_OR_ROLE_UNAVAILABLE, RECIPIENT_NOT_FOUND, MULTIPLE_RECIPIENTS, UI_CHANGED, SEND_FAILED, VERIFICATION_FAILED, TIMEOUT, USER_CANCELLED, PROTECTED_ACTION, UNSUPPORTED_FEATURE, SENSITIVE_CONTENT_BLOCKED, DUPLICATE_ACTION, CALL_SESSION_BLOCKED }

data class MessageAttachment(val type: AttachmentType, val uri: String? = null, val displayName: String? = null)
data class ContactMatch(val displayName: String, val phoneNumber: String, val contactId: Long? = null)
data class MessageRecord(
    val platform: MessagingPlatform,
    val sender: String,
    val text: String,
    val timestampMs: Long,
    val conversationKey: String? = null,
    val messageId: String? = null,
    val sensitive: Boolean = false
)
data class MessageSendRequest(
    val actionId: String,
    val platform: MessagingPlatform,
    val recipient: ContactMatch,
    val text: String,
    val attachments: List<MessageAttachment> = emptyList(),
    val authenticatedVoice: Boolean,
    val confirmed: Boolean,
    val sessionType: MessagingSessionType = MessagingSessionType.NORMAL_ASSISTANT_SESSION
)
data class MessageResult(
    val state: MessageState,
    val error: MessageErrorCode? = null,
    val message: String? = null,
    val verified: Boolean = false,
    val records: List<MessageRecord> = emptyList()
)
interface ContactResolver { fun resolve(nameOrNumber: String): List<ContactMatch> }
interface MessageProvider {
    val platform: MessagingPlatform
    fun isAvailable(): Boolean
    fun openApp(): Result<Unit>
    fun readMessages(contact: String? = null, limit: Int = 20): Result<List<MessageRecord>>
    fun send(request: MessageSendRequest): Result<MessageResult>
}
interface MessagingEngine {
    fun provider(platform: MessagingPlatform): MessageProvider?
    fun resolveContact(query: String): Result<List<ContactMatch>>
    fun prepare(request: MessageSendRequest): MessageResult
    fun send(request: MessageSendRequest): MessageResult
    fun read(platform: MessagingPlatform, contact: String? = null, limit: Int = 20): MessageResult
    fun summarizeAccessible(platform: MessagingPlatform, contact: String? = null, limit: Int = 20): Result<String>
    fun cancel(actionId: String): MessageResult
}
