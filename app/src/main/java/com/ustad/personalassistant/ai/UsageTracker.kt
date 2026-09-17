package com.ustad.personalassistant.ai

data class ProviderUsage(val requests: Long = 0, val successes: Long = 0, val failures: Long = 0, val totalLatencyMs: Long = 0, val inputTokens: Long = 0, val outputTokens: Long = 0, val estimatedCost: Double = 0.0) {
    val averageLatencyMs: Long get() = if (requests == 0L) 0L else totalLatencyMs / requests
}

interface UsageTracker {
    fun record(providerId: String, response: AiResponse?, latencyMs: Long, success: Boolean)
    fun snapshot(): Map<String, ProviderUsage>
    fun clear()
}

class InMemoryUsageTracker : UsageTracker {
    private val data = mutableMapOf<String, ProviderUsage>()
    override fun record(providerId: String, response: AiResponse?, latencyMs: Long, success: Boolean) {
        synchronized(data) {
            val old = data[providerId] ?: ProviderUsage()
            val u = response?.usage
            data[providerId] = old.copy(
                requests = old.requests + 1,
                successes = old.successes + if (success) 1 else 0,
                failures = old.failures + if (success) 0 else 1,
                totalLatencyMs = old.totalLatencyMs + latencyMs.coerceAtLeast(0),
                inputTokens = old.inputTokens + (u?.inputTokens ?: 0),
                outputTokens = old.outputTokens + (u?.outputTokens ?: 0),
                estimatedCost = old.estimatedCost + (u?.estimatedCost ?: 0.0)
            )
        }
    }
    override fun snapshot(): Map<String, ProviderUsage> = synchronized(data) { data.toMap() }
    override fun clear() = synchronized(data) { data.clear() }
}
