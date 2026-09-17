package com.ustad.personalassistant.appcontrol

import com.ustad.personalassistant.accessibility.AutomationDecision
import com.ustad.personalassistant.accessibility.DefaultAutomationPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppAutomationEngineTest {
    @Test fun hindiOpenCommand_normalizesToAppName() {
        assertEquals("whatsapp", AppQueryNormalizer.normalize("WhatsApp kholo"))
        assertEquals("chatgpt", AppQueryNormalizer.normalize("ChatGPT khol do"))
    }

    @Test fun protectedAppsAreBlockedByPolicy() {
        val policy = DefaultAutomationPolicy()
        assertEquals(AutomationDecision.BLOCKED, policy.decision("com.phonepe.app", "open_app"))
        assertEquals(AutomationDecision.BLOCKED, policy.decision("com.google.android.apps.nbu.paisa.user", "click"))
    }

    @Test fun consequentialActionsRequireConfirmation() {
        val policy = DefaultAutomationPolicy()
        assertEquals(AutomationDecision.REQUIRES_CONFIRMATION, policy.decision("com.example.chat", "send_message"))
        assertEquals(AutomationDecision.REQUIRES_CONFIRMATION, policy.decision("com.example.chat", "delete_message"))
    }

    @Test fun safeNavigationIsAllowed() {
        assertEquals(AutomationDecision.ALLOWED, DefaultAutomationPolicy().decision("com.example.reader", "scroll_forward"))
    }

    @Test fun planHasBoundedStepCount() {
        val plan = AutomationPlan(listOf(AutomationStep.PressBack()))
        assertTrue(plan.steps.size <= 20)
    }
}
