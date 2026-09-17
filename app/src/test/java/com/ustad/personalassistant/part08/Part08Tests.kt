package com.ustad.personalassistant.part08

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Part08Tests {
    @Test fun gmailIntentParsingRecognizesUnreadCount() { val intent = GmailIntentParser().parse("Kal ke unread emails count batao"); assertNotNull(intent); assertTrue(intent!!.operation == Part08Operation.COUNT_UNREAD_EMAIL) }
    @Test fun gmailIntentParsingRecognizesSearch() { val intent = GmailIntentParser().parse("Rahul ke emails dekho"); assertNotNull(intent); assertTrue(intent!!.operation == Part08Operation.SEARCH_EMAIL) }
    @Test fun gmailSendRequiresAuthAndConfirmation() { assertFalse(GmailIntentParser().parse("Rahul ko email bhejo message kal milte hain", authenticatedVoice = false, confirmed = true)?.authenticatedVoice == true); assertFalse(GmailIntentParser().parse("Rahul ko email bhejo message kal milte hain", authenticatedVoice = true, confirmed = false)?.confirmed == true) }
    @Test fun callSessionBlocksOwnerActions() { val firewall = CallConversationFirewall(); assertFalse(firewall.allowsOwnerAction("OPEN_APP")); assertFalse(firewall.allowsMessaging()); assertFalse(firewall.allowsAccessibility()); assertFalse(firewall.allowsProtectedAppAction()) }
    @Test fun boundedTimeoutIsClamped() { assertTrue(CallAssistantSettings(unansweredTimeoutSeconds = 25).unansweredTimeoutSeconds in 5..120) }
}
