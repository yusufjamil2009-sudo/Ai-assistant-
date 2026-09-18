package com.ustad.personalassistant.finalagent

/** In-memory bounded request registry used to prevent stale/duplicate execution. */
class ActionRequestRegistry(private val maxEntries: Int = 64) {
    private val states = LinkedHashMap<String, State>()
    private val contexts = LinkedHashMap<String, PendingExecutionContext>()

    data class PendingExecutionContext(val authenticated: Boolean, val voiceAuthenticated: Boolean, val deviceUnlocked: Boolean, val sessionType: AssistantSessionType, val targetApp: String?)

    enum class State { PENDING_CONFIRMATION, EXECUTING, COMPLETED, CANCELLED }

    @Synchronized
    fun putPending(requestId: String, context: PendingExecutionContext? = null): Boolean {
        if (requestId.isBlank() || states.containsKey(requestId)) return false
        states[requestId] = State.PENDING_CONFIRMATION
        if (context != null) contexts[requestId] = context
        trim()
        return true
    }

    @Synchronized
    fun beginExecution(requestId: String): Boolean {
        if (states[requestId] != State.PENDING_CONFIRMATION) return false
        states[requestId] = State.EXECUTING
        return true
    }

    @Synchronized
    fun complete(requestId: String) {
        if (states.containsKey(requestId)) states[requestId] = State.COMPLETED
    }

    @Synchronized
    fun pendingContext(requestId: String): PendingExecutionContext? = contexts[requestId]

    @Synchronized
    fun cancel(requestId: String): Boolean {
        val current = states[requestId] ?: return false
        if (current == State.COMPLETED) return false
        states[requestId] = State.CANCELLED
        contexts.remove(requestId)
        return true
    }

    @Synchronized
    fun state(requestId: String): State? = states[requestId]

    private fun trim() {
        while (states.size > maxEntries.coerceAtLeast(1)) { val oldest = states.entries.first().key; states.remove(oldest); contexts.remove(oldest) }
    }
}
