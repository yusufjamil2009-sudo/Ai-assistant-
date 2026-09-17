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
    private val protectedPackages = setOf(
        "com.phonepe.app",
        "com.google.android.apps.nbu.paisa.user",
        "com.google.android.apps.walletnfcrel",
        "net.one97.paytm"
    )
    private val protectedMarkers = listOf(
        "bank", "banking", "upi", "wallet", "payment", "finance", "finserv", "paytm", "phonepe", "gpay"
    )

    override fun isProtected(packageName: String): Boolean {
        val normalized = packageName.trim().lowercase()
        if (normalized.isBlank()) return false
        return normalized in protectedPackages || protectedMarkers.any { normalized.contains(it) }
    }
}

class SecurityManagerImpl(
    private val protectedAppPolicy: ProtectedAppPolicy = DefaultProtectedAppPolicy()
) : SecurityManager {
    override fun isActionAuthorized(action: String): Boolean = action.isNotBlank()
    override fun isProtectedApp(packageName: String): Boolean = protectedAppPolicy.isProtected(packageName)
    override fun audit(event: String) {
        require(event.length <= 200) { UstadError.ConfigurationError.toString() }
    }
}
