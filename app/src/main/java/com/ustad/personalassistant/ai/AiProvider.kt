package com.ustad.personalassistant.ai

interface AiProvider {
    val providerId: String
    val displayName: String
    val enabled: Boolean
    val priority: Int
    val model: String
    val endpoint: String?
    val capabilities: Set<AiCapability>
    val timeoutMs: Long
    val retryCount: Int
    fun isAvailable(): Boolean
    fun healthCheck(): ProviderHealth
    fun generate(request: AiRequest): Result<AiResponse>
    fun stream(request: AiRequest, onEvent: (AiStreamEvent) -> Unit): Result<AiResponse> = generate(request).onSuccess { onEvent(AiStreamEvent(AiStreamEventType.COMPLETE, response = it)) }
}

enum class ProviderHealthStatus { HEALTHY, DEGRADED, UNHEALTHY, COOLDOWN, DISABLED }
data class ProviderHealth(val status: ProviderHealthStatus = ProviderHealthStatus.UNHEALTHY, val latencyMs: Long? = null, val lastSuccess: Long? = null, val lastFailure: Long? = null, val consecutiveFailures: Int = 0, val cooldownUntil: Long? = null)
data class ProviderConfig(val providerId: String, val displayName: String, val enabled: Boolean = true, val priority: Int = 100, val model: String = "", val endpoint: String? = null, val capabilities: Set<AiCapability> = setOf(AiCapability.CHAT, AiCapability.TEXT_GENERATION), val timeoutMs: Long = 30_000L, val retryCount: Int = 1)

class ConfiguredProvider(private val config: ProviderConfig, private val generator: (AiRequest, ProviderConfig) -> Result<AiResponse>) : AiProvider {
    override val providerId get() = config.providerId
    override val displayName get() = config.displayName
    override val enabled get() = config.enabled
    override val priority get() = config.priority
    override val model get() = config.model
    override val endpoint get() = config.endpoint
    override val capabilities get() = config.capabilities
    override val timeoutMs get() = config.timeoutMs
    override val retryCount get() = config.retryCount.coerceIn(0, 3)
    override fun isAvailable() = enabled
    override fun healthCheck() = ProviderHealth(if (enabled) ProviderHealthStatus.HEALTHY else ProviderHealthStatus.DISABLED)
    override fun generate(request: AiRequest) = if (request.requiredCapabilities.all(capabilities::contains)) generator(request, config) else Result.failure(UnsupportedOperationException(AiErrorCode.PROVIDER_UNSUPPORTED_CAPABILITY.name))
}

object DefaultProviderSlots {
    fun configs(): List<ProviderConfig> = listOf("gemini" to "Gemini", "openrouter" to "OpenRouter", "groq" to "Groq", "mistral" to "Mistral", "sambanova" to "SambaNova", "zhipu" to "Zhipu").mapIndexed { index, (id, name) -> ProviderConfig(id, name, enabled = false, priority = index + 1) }
}
