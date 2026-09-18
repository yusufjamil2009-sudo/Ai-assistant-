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
    private data class Slot(val id: String, val name: String, val model: String, val endpoint: String)

    private val slots = listOf(
        Slot("gemini", "Gemini", "gemini-2.5-flash", "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"),
        Slot("openrouter", "OpenRouter", "openai/gpt-oss-20b:free", "https://openrouter.ai/api/v1/chat/completions"),
        Slot("groq", "Groq", "llama-3.3-70b-versatile", "https://api.groq.com/openai/v1/chat/completions"),
        Slot("mistral", "Mistral", "mistral-small-latest", "https://api.mistral.ai/v1/chat/completions"),
        Slot("sambanova", "SambaNova", "Meta-Llama-3.3-70B-Instruct", "https://api.sambanova.ai/v1/chat/completions"),
        Slot("zhipu", "Zhipu", "glm-4.5-flash", "https://open.bigmodel.cn/api/paas/v4/chat/completions")
    )

    fun configs(): List<ProviderConfig> = slots.mapIndexed { index, slot ->
        ProviderConfig(
            providerId = slot.id,
            displayName = slot.name,
            enabled = false,
            priority = index + 1,
            model = slot.model,
            endpoint = slot.endpoint,
            capabilities = setOf(AiCapability.CHAT, AiCapability.TEXT_GENERATION)
        )
    }
}
