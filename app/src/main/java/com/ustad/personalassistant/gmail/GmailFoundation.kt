package com.ustad.personalassistant.gmail

import com.ustad.personalassistant.security.SecureConfigStore

enum class GmailSendPolicy { DRAFT_ONLY, CONFIRM_BEFORE_SEND, AUTHORIZED_AUTO_SEND }
data class GmailMessage(val id: String, val threadId: String?, val from: String?, val to: String?, val subject: String?, val body: String?, val unread: Boolean)
enum class GmailOperation { LIST, SEARCH, READ, SUMMARIZE, DRAFT, SEND, REPLY, MARK_READ, MARK_UNREAD }
interface GmailRepository {
    fun listEmails(maxResults: Int = 20): Result<List<GmailMessage>>
    fun searchEmails(query: String, maxResults: Int = 20): Result<List<GmailMessage>>
    fun readEmail(id: String): Result<GmailMessage>
    fun draftEmail(message: GmailMessage): Result<String>
    fun sendEmail(draftId: String): Result<String>
    fun replyToEmail(id: String, body: String): Result<String>
    fun markRead(id: String, read: Boolean): Result<Unit>
}
interface GmailAuthManager { fun isConnected(): Boolean; fun beginAuthorization(): Result<Unit>; fun refreshOrReauthenticate(): Result<Unit>; fun disconnect() }
class SecureGmailAuthStateStore(private val store: SecureConfigStore) {
    fun saveConnected(connected: Boolean) { store.put("gmail_connected", connected.toString()) }
    fun isConnected() = store.get("gmail_connected") == "true"
}
class UnconfiguredGmailAuthManager(private val state: SecureGmailAuthStateStore) : GmailAuthManager {
    override fun isConnected() = state.isConnected()
    override fun beginAuthorization() = Result.failure<Unit>(IllegalStateException("Gmail OAuth configuration required"))
    override fun refreshOrReauthenticate() = Result.failure<Unit>(IllegalStateException("Gmail OAuth configuration required"))
    override fun disconnect() { state.saveConnected(false) }
}
class UnconfiguredGmailRepository : GmailRepository {
    private fun <T> unavailable() = Result.failure<T>(IllegalStateException("Gmail API not configured"))
    override fun listEmails(maxResults: Int) = unavailable<List<GmailMessage>>()
    override fun searchEmails(query: String, maxResults: Int) = unavailable<List<GmailMessage>>()
    override fun readEmail(id: String) = unavailable<GmailMessage>()
    override fun draftEmail(message: GmailMessage) = unavailable<String>()
    override fun sendEmail(draftId: String) = unavailable<String>()
    override fun replyToEmail(id: String, body: String) = unavailable<String>()
    override fun markRead(id: String, read: Boolean) = unavailable<Unit>()
}
