package com.ustad.personalassistant.services

import com.ustad.personalassistant.domain.UstadError
import com.ustad.personalassistant.security.SecurityManager

interface ActionExecutor {
    fun execute(action: String, packageName: String? = null): Result<Unit>
}

class GuardedActionExecutor(private val securityManager: SecurityManager) : ActionExecutor {
    override fun execute(action: String, packageName: String?): Result<Unit> {
        if (packageName != null && securityManager.isProtectedApp(packageName)) {
            return Result.failure(IllegalStateException(UstadError.PermissionUnavailable.toString()))
        }
        if (!securityManager.isActionAuthorized(action)) {
            return Result.failure(IllegalStateException(UstadError.AuthenticationRequired.toString()))
        }
        securityManager.audit("authorized action request")
        return Result.success(Unit)
    }
}
