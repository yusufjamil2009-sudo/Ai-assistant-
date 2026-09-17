package com.ustad.personalassistant.automation

enum class ConfirmationDecision { AUTO_APPROVE, CONFIRM_BEFORE_SEND, BLOCKED }
interface ConfirmationPolicy { fun decision(action: String): ConfirmationDecision }
class DefaultConfirmationPolicy : ConfirmationPolicy {
    override fun decision(action: String): ConfirmationDecision = when {
        action.contains("send", true) || action.contains("reply", true) -> ConfirmationDecision.CONFIRM_BEFORE_SEND
        else -> ConfirmationDecision.AUTO_APPROVE
    }
}
