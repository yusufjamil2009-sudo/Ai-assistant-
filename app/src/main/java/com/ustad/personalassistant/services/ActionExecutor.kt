package com.ustad.personalassistant.services

import com.ustad.personalassistant.capability.CapabilityEngine
import com.ustad.personalassistant.domain.UstadError
import com.ustad.personalassistant.domain.userMessage
import com.ustad.personalassistant.permissions.Capability
import com.ustad.personalassistant.security.SecurityManager

interface ActionExecutor {
    fun execute(action: String, packageName: String? = null): Result<Unit>

    fun execute(
        action: String,
        packageName: String? = null,
        requiredCapabilities: List<Capability>
    ): Result<Unit> = execute(action, packageName)
}

class GuardedActionExecutor(
    private val securityManager: SecurityManager,
    private val capabilityEngine: CapabilityEngine? = null
) : ActionExecutor {
    override fun execute(action: String, packageName: String?): Result<Unit> {
        if (packageName != null && securityManager.isProtectedApp(packageName)) {
            return Result.failure(IllegalStateException(UstadError.SecurityBlocked.userMessage()))
        }
        if (!securityManager.isActionAuthorized(action)) {
            return Result.failure(IllegalStateException(UstadError.AuthenticationRequired.userMessage()))
        }
        securityManager.audit("authorized action request")
        return Result.success(Unit)
    }

    override fun execute(
        action: String,
        packageName: String?,
        requiredCapabilities: List<Capability>
    ): Result<Unit> {
        val engine = capabilityEngine ?: return Result.failure(
            IllegalStateException(UstadError.CapabilityUnavailable.userMessage())
        )
        val missing = requiredCapabilities.distinct().filterNot(engine::isAvailable)
        if (missing.isNotEmpty()) {
            return Result.failure(IllegalStateException(
                "Required capability unavailable: ${missing.joinToString { it.name }}"
            ))
        }
        return execute(action, packageName)
    }
}
