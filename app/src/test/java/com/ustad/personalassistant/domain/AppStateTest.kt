package com.ustad.personalassistant.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AppStateTest {
    @Test
    fun defaultStateDoesNotClaimAdvancedCapabilities() {
        val state = AppState()
        assertEquals(CapabilityStatus.NOT_AVAILABLE, state.voiceAuthentication)
        assertEquals(CapabilityStatus.NOT_AVAILABLE, state.backgroundAssistantStatus)
        assertEquals(CapabilityStatus.CONNECT, state.gmailConnection)
        assertEquals(CapabilityStatus.CONNECT, state.googleAccountConnection)
    }
}
