package com.ustad.personalassistant.automation

import com.ustad.personalassistant.accessibility.AutomationDecision
import com.ustad.personalassistant.accessibility.DefaultAutomationPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class AutomationFoundationTest {
    @Test fun protectedFinancialAppIsBlocked() { assertEquals(AutomationDecision.BLOCKED, DefaultAutomationPolicy().decision("com.example.bank", "click")) }
    @Test fun normalAppIsAllowed() { assertEquals(AutomationDecision.ALLOWED, DefaultAutomationPolicy().decision("com.example.notes", "click")) }
    @Test fun outboundSendRequiresConfirmation() { assertEquals(ConfirmationDecision.CONFIRM_BEFORE_SEND, DefaultConfirmationPolicy().decision("send")) }
    @Test fun loggerStoresOnlyStructuredMetadata() { val logger = InMemoryAutomationLogger(); logger.log(AutomationLogEntry(1L, "send", "com.whatsapp", "APP_CONTROL", "ALLOW", AutomationResultStatus.SUCCESS, 10L)); assertEquals("send", logger.snapshot().single().action) }
}
