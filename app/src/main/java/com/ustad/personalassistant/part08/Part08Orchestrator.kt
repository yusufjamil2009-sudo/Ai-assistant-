package com.ustad.personalassistant.part08

import com.ustad.personalassistant.ai.AiBrain
import com.ustad.personalassistant.capability.CapabilityGate
import com.ustad.personalassistant.capability.CapabilityRequirement
import com.ustad.personalassistant.gmail.GmailService
import com.ustad.personalassistant.permissions.Capability
import com.ustad.personalassistant.security.SecurityManager

class Part08Orchestrator(private val gate: CapabilityGate, private val security: SecurityManager, private val gmail: GmailService, private val gmailAuth: com.ustad.personalassistant.gmail.GmailAuthManager, private val aiBrain: AiBrain) {
    fun execute(intent: GmailIntent): GmailActionResult {
        if (!security.isActionAuthorized(intent.operation.name)) return GmailActionResult(false, "Gmail action is not authorized.")
        val capability = gate.check(listOf(CapabilityRequirement(Capability.GMAIL)))
        if (capability !is com.ustad.personalassistant.capability.CapabilityGateResult.Allowed) return GmailActionResult(false, "Gmail capability is unavailable. Connect Gmail first.")
        if (!gmailAuth.isConnected()) return GmailActionResult(false, "Gmail is disconnected. Please connect Gmail first.")
        return runCatching {
            when (intent.operation) {
                Part08Operation.COUNT_UNREAD_EMAIL -> gmail.listUnread(50).map { GmailActionResult(true, "${it.size} unread emails.", it) }.getOrElse { GmailActionResult(false, safeError(it)) }
                Part08Operation.READ_EMAIL -> intent.emailId?.let { gmail.read(it).map { m -> GmailActionResult(true, "Email loaded.", listOf(m)) }.getOrElse { GmailActionResult(false, safeError(it)) } } ?: GmailActionResult(false, "Email reference is required.")
                Part08Operation.SEARCH_EMAIL -> gmail.search(safeQuery(intent.query ?: buildQuery(intent))).map { GmailActionResult(true, "Search complete.", it) }.getOrElse { GmailActionResult(false, safeError(it)) }
                Part08Operation.SUMMARIZE_EMAIL -> {
                    val emails = gmail.search(safeQuery(intent.query ?: buildQuery(intent)), 10).getOrElse { return GmailActionResult(false, safeError(it)) }
                    val compact = emails.take(10).joinToString("\n") { "From: ${it.from.orEmpty()} Subject: ${it.subject.orEmpty()} Snippet: ${it.body.orEmpty().take(500)}" }
                    aiBrain.summarize(com.ustad.personalassistant.ai.AiRequest("Summarize only this email data. Do not infer missing details.\n$compact", setOf(com.ustad.personalassistant.ai.AiCapability.SUMMARIZATION, com.ustad.personalassistant.ai.AiCapability.TEXT_GENERATION))).map { GmailActionResult(true, it.text, emails) }.getOrElse { GmailActionResult(false, safeError(it)) }
                }
                Part08Operation.COMPOSE_EMAIL -> if (!intent.authenticatedVoice) GmailActionResult(false, "Owner voice authentication is required.") else if (!intent.confirmed) GmailActionResult(false, "Explicit confirmation is required before sending.") else {
                    val recipient = intent.recipient?.trim().orEmpty(); val body = intent.body?.trim().orEmpty(); if (!validEmail(recipient) || body.isBlank()) GmailActionResult(false, "Recipient and message body are required.") else gmail.draft(com.ustad.personalassistant.gmail.GmailMessage("", null, null, recipient, intent.subject?.take(200), body.take(20_000), false)).flatMap { gmail.send(it, com.ustad.personalassistant.gmail.GmailSendPolicy.AUTHORIZED_AUTO_SEND) }.map { GmailActionResult(true, "Gmail send accepted.", remoteId = it) }.getOrElse { GmailActionResult(false, safeError(it)) }
                }
                Part08Operation.REPLY_EMAIL -> if (!intent.authenticatedVoice) GmailActionResult(false, "Owner voice authentication is required.") else if (!intent.confirmed) GmailActionResult(false, "Explicit confirmation is required before replying.") else {
                    val id = intent.emailId.orEmpty(); val body = intent.body?.trim().orEmpty(); if (id.isBlank() || body.isBlank()) GmailActionResult(false, "Email reference and reply body are required.") else gmail.reply(id, body, com.ustad.personalassistant.gmail.GmailSendPolicy.AUTHORIZED_AUTO_SEND).map { GmailActionResult(true, "Gmail reply accepted.", remoteId = it) }.getOrElse { GmailActionResult(false, safeError(it)) }
                }
                Part08Operation.CANCEL_EMAIL_ACTION -> GmailActionResult(true, "Gmail action cancelled.")
            }
        }.getOrElse { GmailActionResult(false, "Gmail action failed safely.") }
    }
    private fun buildQuery(intent: GmailIntent): String = listOfNotNull(intent.sender?.takeIf { it.isNotBlank() }?.let { "from:$it" }, intent.subject?.takeIf { it.isNotBlank() }?.let { "subject:${it.replace(Regex("[\\r\\n]"), " ").take(100)}" }), intent.timeRange?.takeIf { it.isNotBlank() }).joinToString(" ").ifBlank { "newer_than:7d" }
    private fun safeQuery(value: String): String { val q = value.replace(Regex("[\\r\\n]"), " ").trim(); require(q.isNotBlank() && q.length <= 200); return q }
    private fun validEmail(value: String): Boolean = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(value)
    private fun safeError(error: Throwable): String = error.message?.take(180) ?: "Gmail operation failed."
}
