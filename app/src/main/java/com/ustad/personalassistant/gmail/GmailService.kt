package com.ustad.personalassistant.gmail

interface GmailService {
    fun listUnread(maxResults: Int = 20): Result<List<GmailMessage>>
    fun search(query: String, maxResults: Int = 20): Result<List<GmailMessage>>
    fun read(id: String): Result<GmailMessage>
    fun draft(message: GmailMessage): Result<String>
    fun send(draftId: String, policy: GmailSendPolicy = GmailSendPolicy.CONFIRM_BEFORE_SEND): Result<String>
    fun reply(id: String, body: String, policy: GmailSendPolicy = GmailSendPolicy.CONFIRM_BEFORE_SEND): Result<String>
}
class DefaultGmailService(private val repository: GmailRepository, private val auth: GmailAuthManager) : GmailService {
    override fun listUnread(maxResults: Int) = repository.listEmails(maxResults).map { it.filter(GmailMessage::unread) }
    override fun search(query: String, maxResults: Int) = repository.searchEmails(query, maxResults)
    override fun read(id: String) = repository.readEmail(id)
    override fun draft(message: GmailMessage) = repository.draftEmail(message)
    override fun send(draftId: String, policy: GmailSendPolicy) = if (!auth.isConnected()) Result.failure(IllegalStateException("Gmail authentication required")) else if (policy != GmailSendPolicy.AUTHORIZED_AUTO_SEND) Result.failure(IllegalStateException("Confirmation required before send")) else repository.sendEmail(draftId)
    override fun reply(id: String, body: String, policy: GmailSendPolicy) = if (!auth.isConnected()) Result.failure(IllegalStateException("Gmail authentication required")) else if (policy != GmailSendPolicy.AUTHORIZED_AUTO_SEND) Result.failure(IllegalStateException("Confirmation required before reply")) else repository.replyToEmail(id, body)
}
