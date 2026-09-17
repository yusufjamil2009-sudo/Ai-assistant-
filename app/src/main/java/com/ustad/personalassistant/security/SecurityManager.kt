package com.ustad.personalassistant.security

import com.ustad.personalassistant.accessibility.DefaultAutomationPolicy
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
    private val blockedMarkers = listOf("bank", "banking", "upi", "wallet", "payment", "finance", "finserv", "paytm", "phonepe", "gpay")
    override fun isProtected(packageName: String): Boolean = blockedMarkers.any { packageName.lowercase().contains(it) }
}

class SecurityManagerImpl(
    private val protectedAppPolicy: ProtectedAppPolicy = DefaultProtectedAppPolicy()
) : SecurityManager {
    override fun isActionAuthorized(action: String): Boolean = action.isNotBlank()
    override fun isProtectedApp(packageName: String): Boolean = protectedAppPolicy.isProtected(packageName)
    override fun audit(event: String) { require(event.length <= 200) { UstadError.ConfigurationError.toString() } }
}
