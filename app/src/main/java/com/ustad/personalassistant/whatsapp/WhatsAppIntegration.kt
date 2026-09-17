package com.ustad.personalassistant.whatsapp

import android.content.Context
import android.content.Intent
import android.net.Uri

data class WhatsAppMessage(val recipient: String, val message: String)
enum class SendPolicy { AUTO_APPROVE, CONFIRM_BEFORE_SEND }
interface WhatsAppIntegration { fun openWhatsApp(context: Context): Boolean; fun prepareMessage(recipient: String, message: String): WhatsAppMessage }
class AndroidWhatsAppIntegration : WhatsAppIntegration {
    override fun openWhatsApp(context: Context): Boolean = try { context.startActivity(context.packageManager.getLaunchIntentForPackage("com.whatsapp") ?: return false); true } catch (_: Exception) { false }
    override fun prepareMessage(recipient: String, message: String) = WhatsAppMessage(recipient.trim(), message)
    fun createShareIntent(message: WhatsAppMessage): Intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, message.message); setPackage("com.whatsapp"); data = Uri.parse("smsto:${Uri.encode(message.recipient)}") }
}
interface WhatsAppConfirmationPolicy { fun requiresConfirmation(): Boolean }
class DefaultWhatsAppConfirmationPolicy : WhatsAppConfirmationPolicy { override fun requiresConfirmation() = true }
