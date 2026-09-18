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
    override fun isActionAuthorized(action: String): Boolean {
        val normalized = action.trim().lowercase()
        if (normalized.isBlank()) return false
        val sensitive = listOf("password", "passcode", "otp", "one time password", "verification code", "recovery code", "pin", "biometric")
        if (sensitive.any(normalized::contains)) return false
        val kind = normalized.substringBefore(":").substringBefore("|").trim()
        val allowed = setOf(
            "open_app", "click", "set_text", "scroll", "scroll_forward", "scroll_backward",
            "back", "read_visible", "cancel", "read_messages", "read_notification",
            "summarize_messages", "send_message", "reply_message", "open_messaging_app",
            "read_email", "search_email", "summarize_email", "draft_email", "send_email",
            "reply_email", "call_contact", "device_diagnostics", "settings", "search", "summarize"
        )
        return kind in allowed
    }
    override fun isProtectedApp(packageName: String): Boolean = protectedAppPolicy.isProtected(packageName)
    override fun audit(event: String) {
        require(event.length <= 200) { UstadError.ConfigurationError.toString() }
    }
}
