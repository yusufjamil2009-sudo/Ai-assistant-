package com.ustad.personalassistant.capability

import com.ustad.personalassistant.domain.CapabilityStatus
import com.ustad.personalassistant.permissions.Capability
import com.ustad.personalassistant.permissions.PermissionManager

/** Single entry point for capability checks used by future assistant actions. */
interface CapabilityEngine {
    fun check(capability: Capability): CapabilityStatus
    fun isAvailable(capability: Capability): Boolean
    fun refreshAll()
}

class DefaultCapabilityEngine(
    private val permissionManager: PermissionManager
) : CapabilityEngine {
    override fun check(capability: Capability): CapabilityStatus = permissionManager.verifyPermission(capability)

    override fun isAvailable(capability: Capability): Boolean = when (check(capability)) {
        CapabilityStatus.ON, CapabilityStatus.CONNECTED -> true
        else -> false
    }

    override fun refreshAll() {
        Capability.entries.forEach(permissionManager::verifyPermission)
    }
}

data class CapabilityRequirement(val capability: Capability)

sealed class CapabilityGateResult {
    data object Allowed : CapabilityGateResult()
    data class Blocked(val missing: List<Capability>) : CapabilityGateResult()
}

class CapabilityGate(private val engine: CapabilityEngine) {
    fun check(required: List<CapabilityRequirement>): CapabilityGateResult {
        val missing = required.map { it.capability }.distinct().filterNot(engine::isAvailable)
        return if (missing.isEmpty()) CapabilityGateResult.Allowed else CapabilityGateResult.Blocked(missing)
    }
}
