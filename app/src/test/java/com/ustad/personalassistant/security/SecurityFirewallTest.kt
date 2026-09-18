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

    private fun safeRequest(
        action: String,
        targetApp: String? = null,
        confirmed: Boolean = false
    ) = SecurityRequest(
        action = action,
        targetApp = targetApp,
        authenticated = true,
        confirmed = confirmed,
        capabilityAvailable = true,
        deviceUnlocked = true
    )

    @Test fun phonePeIsProtected() {
        assertTrue(DefaultProtectedAppPolicy().isProtected("com.phonepe.app"))
    }

    @Test fun financialPackageIsBlocked() {
        assertEquals(
            SecurityDecision.PROTECTED_APP,
            firewall.evaluate(safeRequest("open", "com.phonepe.app"))
        )
    }

    @Test fun bankingActionWithPinIsBlocked() {
        assertEquals(
            SecurityDecision.SENSITIVE_DATA_BLOCKED,
            firewall.evaluate(safeRequest("enter payment PIN", "com.example.bank"))
        )
    }

    @Test fun callerSessionCannotAuthorizeActions() {
        assertEquals(
            SecurityDecision.CALLER_SESSION_BLOCKED,
            firewall.evaluate(
                SecurityRequest(
                    "open WhatsApp",
                    sessionType = SecuritySessionType.CALL_CONVERSATION,
                    authenticated = true,
                    capabilityAvailable = true,
                    deviceUnlocked = true
                )
            )
        )
    }

    @Test fun unauthenticatedOwnerRequiresAuthentication() {
        assertEquals(
            SecurityDecision.AUTH_REQUIRED,
            firewall.evaluate(safeRequest("open WhatsApp").copy(authenticated = false))
        )
    }

    @Test fun outboundSendRequiresConfirmation() {
        assertEquals(
            SecurityDecision.CONFIRMATION_REQUIRED,
            firewall.evaluate(safeRequest("send SMS", confirmed = false))
        )
    }

    @Test fun confirmedSafeActionIsAllowed() {
        assertTrue(firewall.canExecute(safeRequest("open calculator", confirmed = true)))
    }

    @Test fun unknownCapabilityFailsClosed() {
        assertEquals(
            SecurityDecision.CAPABILITY_REQUIRED,
            firewall.evaluate(safeRequest("open calculator").copy(capabilityAvailable = false))
        )
    }

    @Test fun lockedDeviceIsBlocked() {
        assertEquals(
            SecurityDecision.DEVICE_LOCKED,
            firewall.evaluate(safeRequest("open calculator").copy(deviceUnlocked = false))
        )
    }

    @Test fun auditLogDoesNotContainActionBody() {
        val local = SecurityFirewall(SecurityManagerImpl(), DefaultProtectedAppPolicy(), DefaultAutomationPolicy())
        local.evaluate(
            SecurityRequest(
                "send password secret",
                authenticated = true,
                capabilityAvailable = true,
                deviceUnlocked = true
            )
        )
        assertFalse(local.auditSnapshot().first().actionCategory.contains("secret", ignoreCase = true))
    }
}
