package com.ustad.personalassistant.capability

import com.ustad.personalassistant.domain.CapabilityStatus
import com.ustad.personalassistant.permissions.Capability
import com.ustad.personalassistant.permissions.PermissionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityEngineTest {
    private class FakePermissionManager(private val states: Map<Capability, CapabilityStatus>) : PermissionManager {
        override fun currentStatus(capability: Capability) = states[capability] ?: CapabilityStatus.OFF
        override fun requestPermission(activity: android.app.Activity, capability: Capability) = Unit
        override fun openSystemSettings(activity: android.app.Activity, capability: Capability) = Unit
        override fun verifyPermission(capability: Capability) = currentStatus(capability)
        override fun explainPermission(capability: Capability) = "test"
    }

    @Test fun grantedCapabilityIsAvailable() {
        val engine = DefaultCapabilityEngine(FakePermissionManager(mapOf(Capability.OPEN_APPS to CapabilityStatus.ON)))
        assertTrue(engine.isAvailable(Capability.OPEN_APPS))
    }

    @Test fun offCapabilityIsUnavailable() {
        val engine = DefaultCapabilityEngine(FakePermissionManager(mapOf(Capability.MICROPHONE to CapabilityStatus.OFF)))
        assertEquals(false, engine.isAvailable(Capability.MICROPHONE))
    }

    @Test fun gateReportsEveryMissingRequirement() {
        val engine = DefaultCapabilityEngine(FakePermissionManager(mapOf(Capability.OPEN_APPS to CapabilityStatus.ON)))
        val result = CapabilityGate(engine).check(
            listOf(CapabilityRequirement(Capability.OPEN_APPS), CapabilityRequirement(Capability.NOTIFICATION_ACCESS))
        )
        assertTrue(result is CapabilityGateResult.Blocked)
        assertEquals(listOf(Capability.NOTIFICATION_ACCESS), (result as CapabilityGateResult.Blocked).missing)
    }
}
