package com.ustad.personalassistant.whatsapp

import android.content.Context
import com.ustad.personalassistant.domain.CapabilityStatus
import com.ustad.personalassistant.services.AdvancedAccessibilityService

class WhatsAppCapabilityChecker(private val context: Context) {
    fun status(): CapabilityStatus {
        val installed = runCatching { context.packageManager.getApplicationInfo("com.whatsapp", 0) }.isSuccess
        if (!installed) return CapabilityStatus.NOT_AVAILABLE
        return if (AdvancedAccessibilityService.active != null) CapabilityStatus.ON else CapabilityStatus.ACTION_REQUIRED
    }
    fun notificationCapabilityDescription() = "WhatsApp notification reading uses the existing Notification Access capability."
    fun accessibilityCapabilityDescription() = "WhatsApp UI interaction uses the existing App Control capability and Android accessibility service."
}
