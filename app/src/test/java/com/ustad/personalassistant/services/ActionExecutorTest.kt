package com.ustad.personalassistant.services

import com.ustad.personalassistant.capability.CapabilityEngine
import com.ustad.personalassistant.domain.CapabilityStatus
import com.ustad.personalassistant.permissions.Capability
import com.ustad.personalassistant.security.SecurityManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionExecutorTest {
    private class FakeEngine(private val available: Set<Capability>) : CapabilityEngine {
        override fun check(capability: Capability) = if (capability in available) CapabilityStatus.ON else CapabilityStatus.OFF
        override fun isAvailable(capability: Capability) = capability in available
        override fun refreshAll() = Unit
    }

    private class FakeSecurity(private val protected: Boolean = false) : SecurityManager {
        override fun isActionAuthorized(action: String) = action.isNotBlank()
        override fun isProtectedApp(packageName: String) = protected
        override fun audit(event: String) = Unit
    }

    @Test fun missingCapabilityBlocksAction() {
        val executor = GuardedActionExecutor(FakeSecurity(), FakeEngine(emptySet()))
        assertFalse(executor.execute("open messages", requiredCapabilities = listOf(Capability.NOTIFICATION_ACCESS)).isSuccess)
    }

    @Test fun protectedAppBlocksAction() {
        val executor = GuardedActionExecutor(FakeSecurity(protected = true), FakeEngine(setOf(Capability.OPEN_APPS)))
        assertFalse(executor.execute("open payment app", "com.example.payment", listOf(Capability.OPEN_APPS)).isSuccess)
    }

    @Test fun availableAndAuthorizedActionPassesGate() {
        val executor = GuardedActionExecutor(FakeSecurity(), FakeEngine(setOf(Capability.OPEN_APPS)))
        assertTrue(executor.execute("open app", requiredCapabilities = listOf(Capability.OPEN_APPS)).isSuccess)
    }
}
