package com.ustad.personalassistant.ai

class ApiManager(private val providers: List<AiProvider>, private val networkState: () -> NetworkState = { NetworkState.ONLINE }, private val clock: () -> Long = { System.currentTimeMillis() }) {
    private val health = mutableMapOf<String, ProviderHealth>()
    private val cooldownMs = 30_000L

    fun selectProvider(request: AiRequest): AiProvider? = candidates(request).firstOrNull()

    fun generate(request: AiRequest): Result<AiResponse> {
        if (networkState() == NetworkState.OFFLINE) return Result.failure(AiException(AiErrorCode.OFFLINE))
        val list = candidates(request)
        if (list.isEmpty()) return Result.failure(AiException(AiErrorCode.NO_PROVIDER_AVAILABLE))
        var last: AiException? = null
        for (provider in list) {
            repeat(provider.retryCount + 1) { attempt ->
                val started = clock()
                val result = provider.generate(request)
                val elapsed = (clock() - started).coerceAtLeast(0L)
                if (result.isSuccess) {
                    health[provider.providerId] = ProviderHealth(ProviderHealthStatus.HEALTHY, elapsed, clock(), health[provider.providerId]?.lastFailure, 0, null)
                    return result.map { it.copy(providerId = it.providerId ?: provider.providerId, model = it.model ?: provider.model) }
                }
                val code = classify(result.exceptionOrNull())
                last = AiException(code)
                val failures = (health[provider.providerId]?.consecutiveFailures ?: 0) + 1
                val cooldown = if (code == AiErrorCode.PROVIDER_RATE_LIMITED || attempt == provider.retryCount) clock() + cooldownMs else null
                health[provider.providerId] = ProviderHealth(if (cooldown != null) ProviderHealthStatus.COOLDOWN else ProviderHealthStatus.DEGRADED, elapsed, health[provider.providerId]?.lastSuccess, clock(), failures, cooldown)
                if (attempt < provider.retryCount) Thread.sleep(backoffMs(attempt))
            }
        }
        return Result.failure(last ?: AiException(AiErrorCode.ALL_PROVIDERS_FAILED))
    }

    fun health(providerId: String): ProviderHealth = health[providerId] ?: providers.firstOrNull { it.providerId == providerId }?.healthCheck() ?: ProviderHealth()
    private fun candidates(request: AiRequest): List<AiProvider> = providers.filter { it.enabled && it.isAvailable() && request.requiredCapabilities.all(it.capabilities::contains) }
        .filter { health[it.providerId]?.let { h -> h.status == ProviderHealthStatus.COOLDOWN && (h.cooldownUntil ?: 0L) > clock() } != true }
        .sortedBy { it.priority }
    private fun backoffMs(attempt: Int): Long = (150L * (1L shl attempt.coerceIn(0, 3))).coerceAtMost(1_200L)
    private fun classify(t: Throwable?): AiErrorCode = when (t?.message?.uppercase()) {
        AiErrorCode.PROVIDER_TIMEOUT.name -> AiErrorCode.PROVIDER_TIMEOUT
        AiErrorCode.PROVIDER_RATE_LIMITED.name -> AiErrorCode.PROVIDER_RATE_LIMITED
        AiErrorCode.PROVIDER_AUTH_ERROR.name -> AiErrorCode.PROVIDER_AUTH_ERROR
        AiErrorCode.PROVIDER_NETWORK_ERROR.name -> AiErrorCode.PROVIDER_NETWORK_ERROR
        AiErrorCode.PROVIDER_UNSUPPORTED_CAPABILITY.name -> AiErrorCode.PROVIDER_UNSUPPORTED_CAPABILITY
        else -> AiErrorCode.PROVIDER_SERVER_ERROR
    }
}

enum class NetworkState { ONLINE, OFFLINE, UNSTABLE }
class AiException(val code: AiErrorCode) : RuntimeException(code.name)
