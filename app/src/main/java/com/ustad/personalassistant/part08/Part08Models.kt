package com.ustad.personalassistant.part08

import com.ustad.personalassistant.gmail.GmailMessage

enum class Part08Operation { READ_EMAIL, SEARCH_EMAIL, SUMMARIZE_EMAIL, COUNT_UNREAD_EMAIL, COMPOSE_EMAIL, REPLY_EMAIL, CANCEL_EMAIL_ACTION }
data class GmailIntent(val operation: Part08Operation, val sender: String? = null, val recipient: String? = null, val subject: String? = null, val query: String? = null, val emailId: String? = null, val threadId: String? = null, val body: String? = null, val timeRange: String? = null, val authenticatedVoice: Boolean = false, val confirmed: Boolean = false)
data class GmailActionResult(val success: Boolean, val message: String, val emails: List<GmailMessage> = emptyList(), val draftId: String? = null, val remoteId: String? = null)
enum class CallAssistantState { IDLE, INCOMING_CALL, RINGING, WAITING_FOR_TIMEOUT, ANSWERING, CALL_CONVERSATION, CALL_PROCESSING, CALL_RESPONDING, CALL_ENDED, SUMMARY_PROCESSING, SUMMARY_READY, FAILED }
data class CallCapabilityStatus(val telecomAvailable: Boolean, val autoAnswerAvailable: Boolean, val microphoneAvailable: Boolean, val callAudioConversationAvailable: Boolean)
data class CallSession(val id: String, val number: String?, val displayName: String?, val startedAtMs: Long, var answeredAtMs: Long? = null, var endedAtMs: Long? = null, var conversationAvailable: Boolean = false, var purpose: String? = null, var callbackRequest: String? = null, var importantPoints: List<String> = emptyList())
data class CallSummary(val caller: String?, val durationMs: Long, val purpose: String?, val callbackRequest: String?, val importantPoints: List<String>, val conversationAvailable: Boolean, val text: String)
data class CallAssistantSettings(val enabled: Boolean = false, val unansweredTimeoutSeconds: Int = 25, val greetingEnabled: Boolean = true, val postCallSummaryEnabled: Boolean = true)
data class Part08Result(val ok: Boolean, val code: String, val message: String)
