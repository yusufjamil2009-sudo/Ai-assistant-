package com.ustad.personalassistant.accessibility

enum class AutomationDecision { ALLOWED, REQUIRES_CONFIRMATION, BLOCKED }
interface AutomationPolicy { fun decision(packageName: String, action: String): AutomationDecision }
class DefaultAutomationPolicy : AutomationPolicy {
    private val blockedPackageMarkers = listOf("bank", "banking", "upi", "wallet", "payment", "finance", "finserv", "paytm", "phonepe", "gpay", "paisa")
    override fun decision(packageName: String, action: String): AutomationDecision { val normalized = packageName.lowercase(); if (blockedPackageMarkers.any { normalized.contains(it) }) return AutomationDecision.BLOCKED; if (action.contains("send", true) || action.contains("delete", true)) return AutomationDecision.REQUIRES_CONFIRMATION; return AutomationDecision.ALLOWED }
}
