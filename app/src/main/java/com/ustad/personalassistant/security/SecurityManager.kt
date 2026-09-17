package com.ustad.personalassistant.security

import com.ustad.personalassistant.domain.UstadError

interface SecurityManager {
    fun isActionAuthorized(action: String): Boolean
    fun isProtectedApp(packageName: String): Boolean
    fun audit(event: String)
}

interface ProtectedAppPolicy {
    fun isProtected(packageName: String): Boolean
}

class DefaultProtectedAppPolicy : ProtectedAppPolicy {
    override fun isProtected(packageName: String): Boolean = false
}

class SecurityManagerImpl(
    private val protectedAppPolicy: ProtectedAppPolicy = DefaultProtectedAppPolicy()
) : SecurityManager {
    override fun isActionAuthorized(action: String): Boolean = action.isNotBlank()
    override fun isProtectedApp(packageName: String): Boolean = protectedAppPolicy.isProtected(packageName)
    override fun audit(event: String) {
        // Part 01 intentionally records no sensitive payloads and performs no actions.
        require(event.length <= 200) { UstadError.ConfigurationError.toString() }
    }
}
