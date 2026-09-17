package com.ustad.personalassistant.part08

import com.ustad.personalassistant.ai.AiBrain
import com.ustad.personalassistant.capability.CapabilityGate
import com.ustad.personalassistant.capability.CapabilityGateResult
import com.ustad.personalassistant.capability.CapabilityRequirement
import com.ustad.personalassistant.gmail.GmailMessage
import com.ustad.personalassistant.gmail.GmailSendPolicy
import com.ustad.personalassistant.gmail.GmailService
import com.ustad.personalassistant.permissions.Capability
import com.ustad.personalassistant.security.SecurityManager

class Part08Orchestrator(private val gate: CapabilityGate, private val security: SecurityManager, private val gmail: GmailService, private val gmailAuth: com.ustad.personalassistant.gmail.GmailAuthManager, private val aiBrain: AiBrain) {
    fun execute(intent: GmailIntent): GmailActionResult {
        if (!security.isActionAuthorized(intent.operation.name)) return GmailActionResult(false, "Gmail action is not authorized.")
        if (gate.check(listOf(CapabilityRequirement(Capability.GMAIL))) !is CapabilityGateResult.Allowed) return GmailActionResult(false, "Gmail capability is unavailable. Connect Gmail first.")
        if (!gmailAuth.isConnected()) return GmailActionResult(false, "Gmail is disconnected. Please connect Gmail first.")
        return try {
            when (intent.operation) {
                Part08Operation.COUNT_UNREAD_EMAIL -> gmail.listUnread(50).fold({ GmailActionResult(true, "${it.size} unread emails.", it) }, { failure(it) })
                Part08Operation.READ_EMAIL -> intent.emailId?.let { id -> gmail.read(id).fold({ GmailActionResult(true, "Email loaded.", listOf(it)) }, { failure(it) }) } ?: GmailActionResult(false, "Email reference is required.")
                Part08Operation.SEARCH_EMAIL -> gmail.search(safeQuery(intent.query ?: buildQuery(intent)), 20).fold({ GmailActionResult(true, "Search complete.", it) }, { failure(it) })
                Part08Operation.SUMMARIZE_EMAIL -> summarize(intent)
                Part08Operation.COMPOSE_EMAIL -> compose(intent)
                Part08Operation.REPLY_EMAIL -> reply(intent)
                Part08Operation.CANCEL_EMAIL_ACTION -> GmailActionResult(true, "Gmail action cancelled.")
            }
        } catch (t: Throwable) { failure(t) }
    }
    private fun summarize(intent: GmailIntent): GmailActionResult { val emails = gmail.search(safeQuery(intent.query ?: buildQuery(intent)), 10).getOrElse { return failure(it) }; val compact = emails.take(10).joinToString("\n") { "From: ${it.from.orEmpty()} Subject: ${it.subject.orEmpty()} Snippet: ${it.body.orEmpty().take(500)}" }; return aiBrain.summarize(com.ustad.personalassistant.ai.AiRequest("Summarize only this email data. Do not infer missing details.\n$compact", setOf(com.ustad.personalassistant.ai.AiCapability.SUMMARIZATION, com.ustad.personalassistant.ai.AiCapability.TEXT_GENERATION))).fold({ GmailActionResult(true, it.text, emails) }, { failure(it) }) }
    private fun compose(intent: GmailIntent): GmailActionResult { if (!intent.authenticatedVoice) return GmailActionResult(false, "Owner voice authentication is required."); if (!intent.confirmed) return GmailActionResult(false, "Explicit confirmation is required before sending."); val recipient = intent.recipient?.trim().orEmpty(); val body = intent.body?.trim().orEmpty(); if (!validEmail(recipient) || body.isBlank()) return GmailActionResult(false, "Recipient and message body are required."); val draft = gmail.draft(GmailMessage("", null, null, recipient, intent.subject?.take(200), body.take(20_000), false)).getOrElse { return failure(it) }; return gmail.send(draft, GmailSendPolicy.AUTHORIZED_AUTO_SEND).fold({ GmailActionResult(true, "Gmail send accepted.", remoteId = it) }, { failure(it) }) }
    private fun reply(intent: GmailIntent): GmailActionResult { if (!intent.authenticatedVoice) return GmailActionResult(false, "Owner voice authentication is required."); if (!intent.confirmed) return GmailActionResult(false, "Explicit confirmation is required before replying."); val id = intent.emailId.orEmpty(); val body = intent.body?.trim().orEmpty(); if (id.isBlank() || body.isBlank()) return GmailActionResult(false, "Email reference and reply body are required."); return gmail.reply(id, body, GmailSendPolicy.AUTHORIZED_AUTO_SEND).fold({ GmailActionResult(true, "Gmail reply accepted.", remoteId = it) }, { failure(it) }) }
    private fun buildQuery(intent: GmailIntent): String = listOfNotNull(intent.sender?.takeIf(String::isNotBlank)?.let { "from:$it" }, intent.subject?.takeIf(String::isNotBlank)?.let { "subject:${it.replace(Regex("[\\r\\n]"), " ").take(100)}" }, intent.timeRange?.takeIf(String::isNotBlank)).joinToString(" ").ifBlank { "newer_than:7d" }
    private fun safeQuery(value: String): String { val q = value.replace(Regex("[\\r\\n]"), " ").trim(); require(q.isNotBlank() && q.length <= 200); return q }
    private fun validEmail(value: String): Boolean = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(value)
    private fun failure(error: Throwable) = GmailActionResult(false, error.message?.take(180) ?: "Gmail operation failed.")
}
