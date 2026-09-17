package com.ustad.personalassistant.security

import com.ustad.personalassistant.accessibility.DefaultAutomationPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityFirewallTest {
    private val firewall = SecurityFirewall(
        SecurityManagerImpl(),
        DefaultProtectedAppPolicy(),
        DefaultAutomationPolicy()
    )

    @Test fun phonePeIsProtected() {
        assertTrue(DefaultProtectedAppPolicy().isProtected("com.phonepe.app"))
    }

    @Test fun financialPackageIsBlocked() {
        assertEquals(SecurityDecision.PROTECTED_APP, firewall.evaluate(SecurityRequest("open", "com.phonepe.app")))
    }

    @Test fun bankingActionWithPinIsBlocked() {
        assertEquals(SecurityDecision.SENSITIVE_DATA_BLOCKED, firewall.evaluate(SecurityRequest("enter payment PIN", "com.example.bank")))
    }

    @Test fun callerSessionCannotAuthorizeActions() {
        assertEquals(SecurityDecision.CALLER_SESSION_BLOCKED, firewall.evaluate(SecurityRequest("open WhatsApp", sessionType = SecuritySessionType.CALL_CONVERSATION)))
    }

    @Test fun unauthenticatedOwnerRequiresAuthentication() {
        assertEquals(SecurityDecision.AUTH_REQUIRED, firewall.evaluate(SecurityRequest("open WhatsApp", authenticated = false)))
    }

    @Test fun outboundSendRequiresConfirmation() {
        assertEquals(SecurityDecision.CONFIRMATION_REQUIRED, firewall.evaluate(SecurityRequest("send SMS", confirmed = false)))
    }

    @Test fun confirmedSafeActionIsAllowed() {
        assertTrue(firewall.canExecute(SecurityRequest("open calculator", confirmed = true)))
    }

    @Test fun unknownCapabilityFailsClosed() {
        assertEquals(SecurityDecision.CAPABILITY_REQUIRED, firewall.evaluate(SecurityRequest("open calculator", capabilityAvailable = false)))
    }

    @Test fun lockedDeviceIsBlocked() {
        assertEquals(SecurityDecision.DEVICE_LOCKED, firewall.evaluate(SecurityRequest("open calculator", deviceUnlocked = false)))
    }

    @Test fun auditLogDoesNotContainActionBody() {
        val local = SecurityFirewall(SecurityManagerImpl(), DefaultProtectedAppPolicy(), DefaultAutomationPolicy())
        local.evaluate(SecurityRequest("send password secret"))
        assertFalse(local.auditSnapshot().first().actionCategory.contains("secret", ignoreCase = true))
    }
}
