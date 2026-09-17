package com.ustad.personalassistant.whatsapp

import com.ustad.personalassistant.automation.AutomationResult
import com.ustad.personalassistant.automation.AutomationResultStatus

data class WhatsAppSendRequest(val recipient: String, val message: String, val policy: SendPolicy = SendPolicy.CONFIRM_BEFORE_SEND)
interface WhatsAppActionExecutor { fun prepareSend(request: WhatsAppSendRequest): AutomationResult<WhatsAppMessage> }
class DefaultWhatsAppActionExecutor(private val integration: WhatsAppIntegration) : WhatsAppActionExecutor {
    override fun prepareSend(request: WhatsAppSendRequest): AutomationResult<WhatsAppMessage> {
        if (request.recipient.isBlank() || request.message.isBlank()) return AutomationResult(AutomationResultStatus.ERROR, message = "Recipient and message are required")
        return AutomationResult(AutomationResultStatus.SUCCESS, integration.prepareMessage(request.recipient, request.message))
    }
}
