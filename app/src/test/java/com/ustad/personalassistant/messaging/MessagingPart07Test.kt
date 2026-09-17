package com.ustad.personalassistant.messaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagingPart07Test {
    @Test fun parserBuildsStructuredSendIntent() { val intent = MessagingIntentParser().parse("Rahul ko WhatsApp par bolo kal milna"); assertEquals(com.ustad.personalassistant.ai.IntentType.SEND_MESSAGE, intent.type); assertEquals(MessagingPlatform.WHATSAPP, intent.platform); assertEquals("Rahul", intent.contact); assertEquals("kal milna", intent.messageText); assertTrue(intent.requiresConfirmation) }
    @Test fun parserReadsWhatsappConversation() { val intent = MessagingIntentParser().parse("WhatsApp par Rahul ka message batao"); assertEquals(com.ustad.personalassistant.ai.IntentType.READ_MESSAGES, intent.type); assertEquals("Rahul", intent.contact) }
    @Test fun sensitiveContentIsDetectedAndRedacted() { val record = MessageRecord(MessagingPlatform.SMS, "Service", "Your OTP is 123456", 1L); val safe = SensitiveMessageDetector.redact(record); assertTrue(safe.sensitive); assertTrue(safe.text.contains("withheld")) }
    @Test fun prepareRequiresAuthenticatedVoiceAndConfirmation() { val request = MessageSendRequest("a1", MessagingPlatform.WHATSAPP, ContactMatch("Rahul", "9999999999"), "kal milna", authenticatedVoice = false, confirmed = false); val engine = FakeMessagingEngine(); assertEquals(MessageErrorCode.PROTECTED_ACTION, engine.prepare(request).error); val authenticated = request.copy(authenticatedVoice = true); assertEquals(MessageState.WAITING_FOR_CONFIRMATION, engine.prepare(authenticated).state) }
    @Test fun callSessionCannotSend() { val engine = FakeMessagingEngine(); val request = MessageSendRequest("a2", MessagingPlatform.SMS, ContactMatch("Rahul", "9999999999"), "hello", authenticatedVoice = true, confirmed = true, sessionType = MessagingSessionType.CALL_CONVERSATION_SESSION); assertEquals(MessageErrorCode.CALL_SESSION_BLOCKED, engine.prepare(request).error) }
    @Test fun cancellationIsExplicit() { val engine = FakeMessagingEngine(); assertEquals(MessageState.CANCELLED, engine.cancel("cancel-me").state) }
    private class FakeMessagingEngine : MessagingEngine {
        override fun provider(platform: MessagingPlatform): MessageProvider? = null; override fun resolveContact(query: String): Result<List<ContactMatch>> = Result.success(emptyList())
        override fun prepare(request: MessageSendRequest): MessageResult = when { request.sessionType == MessagingSessionType.CALL_CONVERSATION_SESSION -> MessageResult(MessageState.FAILED, MessageErrorCode.CALL_SESSION_BLOCKED); !request.authenticatedVoice -> MessageResult(MessageState.FAILED, MessageErrorCode.PROTECTED_ACTION); SensitiveMessageDetector.isSensitive(request.text) -> MessageResult(MessageState.FAILED, MessageErrorCode.SENSITIVE_CONTENT_BLOCKED); !request.confirmed -> MessageResult(MessageState.WAITING_FOR_CONFIRMATION); else -> MessageResult(MessageState.PREPARING_MESSAGE) }
        override fun send(request: MessageSendRequest): MessageResult = prepare(request); override fun read(platform: MessagingPlatform, contact: String?, limit: Int): MessageResult = MessageResult(MessageState.SUCCESS); override fun summarizeAccessible(platform: MessagingPlatform, contact: String?, limit: Int): Result<String> = Result.success("summary"); override fun cancel(actionId: String): MessageResult = MessageResult(MessageState.CANCELLED, MessageErrorCode.USER_CANCELLED)
    }
}
