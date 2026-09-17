package com.ustad.personalassistant.gmail

import com.ustad.personalassistant.automation.AutomationResult
import com.ustad.personalassistant.automation.AutomationResultStatus

data class GmailSendRequest(val draftId: String, val policy: GmailSendPolicy = GmailSendPolicy.CONFIRM_BEFORE_SEND)
interface GmailActionExecutor { fun send(request: GmailSendRequest): AutomationResult<String> }
class DefaultGmailActionExecutor(private val repository: GmailRepository, private val auth: GmailAuthManager) : GmailActionExecutor {
    override fun send(request: GmailSendRequest): AutomationResult<String> {
        if (!auth.isConnected()) return AutomationResult(AutomationResultStatus.AUTH_REQUIRED)
        if (request.policy != GmailSendPolicy.AUTHORIZED_AUTO_SEND) return AutomationResult(AutomationResultStatus.AUTH_REQUIRED, message = "Confirmation is required before sending")
        return repository.sendEmail(request.draftId).fold({ AutomationResult(AutomationResultStatus.SUCCESS, it) }, { AutomationResult(AutomationResultStatus.NETWORK_ERROR, message = it.message) })
    }
}
