package com.ustad.personalassistant.ai

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.random.Random

class ApiManager(
    private val providerSource: () -> List<AiProvider>,
    private val networkState: () -> NetworkState = { NetworkState.ONLINE },
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val usageTracker: UsageTracker = InMemoryUsageTracker()
) {
    constructor(
        providers: List<AiProvider>,
        networkState: () -> NetworkState = { NetworkState.ONLINE },
        clock: () -> Long = { System.currentTimeMillis() },
        usageTracker: UsageTracker = InMemoryUsageTracker()
    ) : this({ providers }, networkState, clock, usageTracker)

    private val health = mutableMapOf<String, ProviderHealth>()
    private val cooldownMs = 30_000L

    fun selectProvider(request: AiRequest): AiProvider? = candidates(request).firstOrNull()
    fun usage(): Map<String, ProviderUsage> = usageTracker.snapshot()
    fun health(providerId: String): ProviderHealth = health[providerId]
        ?: providerSource().firstOrNull { it.providerId == providerId }?.healthCheck()
        ?: ProviderHealth()

    fun generate(request: AiRequest): Result<AiResponse> = generateInternal(request)
    fun generateAsync(scope: CoroutineScope, request: AiRequest, onResult: (Result<AiResponse>) -> Unit): Job = scope.launch(Dispatchers.IO) { onResult(generateInternal(request)) }

    fun stream(request: AiRequest, onEvent: (AiStreamEvent) -> Unit): Result<AiResponse> {
        if (networkState() == NetworkState.OFFLINE) return Result.failure(AiException(AiErrorCode.OFFLINE))
        val list = candidates(request)
        if (list.isEmpty()) return Result.failure(AiException(AiErrorCode.NO_PROVIDER_AVAILABLE))
        onEvent(AiStreamEvent(AiStreamEventType.START))
        var last: AiException? = null
        for (provider in list) {
            val started = clock()
            val result = provider.stream(request) { event -> if (event.type != AiStreamEventType.START) onEvent(event) }
            val elapsed = (clock() - started).coerceAtLeast(0L)
            usageTracker.record(provider.providerId, result.getOrNull(), elapsed, result.isSuccess)
            if (result.isSuccess) {
                health[provider.providerId] = ProviderHealth(ProviderHealthStatus.HEALTHY, elapsed, clock(), health[provider.providerId]?.lastFailure, 0, null)
                val response = result.getOrThrow().copy(
                    providerId = result.getOrThrow().providerId ?: provider.providerId,
                    model = result.getOrThrow().model ?: provider.model
                )
                onEvent(AiStreamEvent(AiStreamEventType.COMPLETE, response = response))
                return Result.success(response)
            }
            val code = classify(result.exceptionOrNull())
            last = AiException(code)
            markFailure(provider.providerId, code, elapsed)
        }
        onEvent(AiStreamEvent(AiStreamEventType.ERROR, error = AiError(last?.code ?: AiErrorCode.ALL_PROVIDERS_FAILED, "All configured providers failed")))
        return Result.failure(last ?: AiException(AiErrorCode.ALL_PROVIDERS_FAILED))
    }

    private fun generateInternal(request: AiRequest): Result<AiResponse> {
        if (networkState() == NetworkState.OFFLINE) return Result.failure(AiException(AiErrorCode.OFFLINE))
        val list = candidates(request)
        if (list.isEmpty()) return Result.failure(AiException(AiErrorCode.NO_PROVIDER_AVAILABLE))
        var last: AiException? = null
        for (provider in list) {
            repeat(provider.retryCount + 1) { attempt ->
                val started = clock()
                val result = runProviderWithTimeout(provider, request)
                val elapsed = (clock() - started).coerceAtLeast(0L)
                usageTracker.record(provider.providerId, result.getOrNull(), elapsed, result.isSuccess)
                if (result.isSuccess) {
                    health[provider.providerId] = ProviderHealth(ProviderHealthStatus.HEALTHY, elapsed, clock(), health[provider.providerId]?.lastFailure, 0, null)
                    val response = result.getOrThrow()
                    val normalized = response.copy(providerId = response.providerId ?: provider.providerId, model = response.model ?: provider.model)
                    if (normalized.actionPlan?.let { DefaultActionPlanValidator().validate(it) } == false) return Result.failure(AiException(AiErrorCode.INVALID_AI_RESPONSE))
                    return Result.success(normalized)
                }
                val code = classify(result.exceptionOrNull())
                last = AiException(code)
                markFailure(provider.providerId, code, elapsed, attempt == provider.retryCount)
                if (attempt < provider.retryCount) Thread.sleep(backoffMs(attempt))
            }
        }
        return Result.failure(last ?: AiException(AiErrorCode.ALL_PROVIDERS_FAILED))
    }

    private fun markFailure(providerId: String, code: AiErrorCode, elapsed: Long, forceCooldown: Boolean = true) {
        val failures = (health[providerId]?.consecutiveFailures ?: 0) + 1
        val cooldown = if (code == AiErrorCode.PROVIDER_RATE_LIMITED || forceCooldown) clock() + cooldownMs else null
        health[providerId] = ProviderHealth(
            if (cooldown != null) ProviderHealthStatus.COOLDOWN else ProviderHealthStatus.DEGRADED,
            elapsed,
            health[providerId]?.lastSuccess,
            clock(),
            failures,
            cooldown
        )
    }

    private fun runProviderWithTimeout(provider: AiProvider, request: AiRequest): Result<AiResponse> = runCatching {
        val timeout = provider.timeoutMs.coerceIn(1_000L, 120_000L)
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
        try { executor.submit<Result<AiResponse>> { provider.generate(request) }.get(timeout, java.util.concurrent.TimeUnit.MILLISECONDS) }
        finally { executor.shutdownNow() }
    }.fold({ it }, { throwable -> Result.failure(if (throwable is java.util.concurrent.TimeoutException) AiException(AiErrorCode.PROVIDER_TIMEOUT) else throwable) })

    private fun candidates(request: AiRequest): List<AiProvider> {
        val currentProviders = providerSource()
        return currentProviders
            .filter { it.enabled && it.isAvailable() && request.requiredCapabilities.all(it.capabilities::contains) }
            .filter { health[it.providerId]?.let { h -> h.status == ProviderHealthStatus.COOLDOWN && (h.cooldownUntil ?: 0L) > clock() } != true }
            .sortedBy { it.priority }
    }

    private fun backoffMs(attempt: Int): Long {
        val base = (150L * (1L shl attempt.coerceIn(0, 3))).coerceAtMost(1_200L)
        return (base + Random.nextLong(0, (base / 3).coerceAtLeast(1))).coerceAtMost(1_600L)
    }

    private fun classify(t: Throwable?): AiErrorCode = when {
        t is AiException -> t.code
        t?.message?.uppercase() == AiErrorCode.PROVIDER_TIMEOUT.name -> AiErrorCode.PROVIDER_TIMEOUT
        t?.message?.uppercase() == AiErrorCode.PROVIDER_RATE_LIMITED.name -> AiErrorCode.PROVIDER_RATE_LIMITED
        t?.message?.uppercase() == AiErrorCode.PROVIDER_AUTH_ERROR.name -> AiErrorCode.PROVIDER_AUTH_ERROR
        t?.message?.uppercase() == AiErrorCode.PROVIDER_NETWORK_ERROR.name -> AiErrorCode.PROVIDER_NETWORK_ERROR
        t?.message?.uppercase() == AiErrorCode.PROVIDER_UNSUPPORTED_CAPABILITY.name -> AiErrorCode.PROVIDER_UNSUPPORTED_CAPABILITY
        else -> AiErrorCode.PROVIDER_SERVER_ERROR
    }
}

enum class NetworkState { ONLINE, OFFLINE, UNSTABLE }
class AiException(val code: AiErrorCode) : RuntimeException(code.name)
