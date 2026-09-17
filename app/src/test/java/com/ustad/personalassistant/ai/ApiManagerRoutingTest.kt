package com.ustad.personalassistant.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiManagerRoutingTest {
    private fun provider(
        id: String,
        priority: Int,
        response: Result<AiResponse>,
        streamResponse: Result<AiResponse> = response,
        enabled: Boolean = true
    ): AiProvider = object : AiProvider {
        override val providerId = id
        override val displayName = id
        override val enabled = enabled
        override val priority = priority
        override val model = "model-$id"
        override val endpoint: String? = "https://example.test/$id"
        override val capabilities = setOf(AiCapability.CHAT, AiCapability.TEXT_GENERATION)
        override val timeoutMs = 1_000L
        override val retryCount = 0
        override fun isAvailable() = enabled
        override fun healthCheck() = ProviderHealth(if (enabled) ProviderHealthStatus.HEALTHY else ProviderHealthStatus.DISABLED)
        override fun generate(request: AiRequest) = response
        override fun stream(request: AiRequest, onEvent: (AiStreamEvent) -> Unit): Result<AiResponse> {
            if (streamResponse.isSuccess) onEvent(AiStreamEvent(AiStreamEventType.TOKEN, text = "ok"))
            return streamResponse
        }
    }

    @Test fun defaultCatalogContainsAllRequestedProvidersWithModelsAndEndpoints() {
        val ids = DefaultProviderSlots.configs().map { it.providerId }
        assertEquals(listOf("gemini", "openrouter", "groq", "mistral", "sambanova", "zhipu"), ids)
        assertTrue(DefaultProviderSlots.configs().all { it.model.isNotBlank() && !it.endpoint.isNullOrBlank() })
    }

    @Test fun priorityAndEnabledStateControlSelection() {
        val manager = ApiManager(listOf(provider("later", 2, Result.success(AiResponse("later"))), provider("first", 1, Result.success(AiResponse("first"))), provider("off", 0, Result.success(AiResponse("off")), enabled = false)))
        assertEquals("first", manager.selectProvider(AiRequest("hi"))?.providerId)
    }

    @Test fun generateFailsOverAfterProviderError() {
        val manager = ApiManager(listOf(provider("bad", 1, Result.failure(AiException(AiErrorCode.PROVIDER_RATE_LIMITED))), provider("good", 2, Result.success(AiResponse("ok")))))
        val result = manager.generate(AiRequest("hi"))
        assertTrue(result.isSuccess)
        assertEquals("good", result.getOrNull()?.providerId)
        assertEquals(ProviderHealthStatus.COOLDOWN, manager.health("bad").status)
    }

    @Test fun streamFailsOverAfterFirstProviderError() {
        val events = mutableListOf<AiStreamEventType>()
        val manager = ApiManager(listOf(
            provider("bad", 1, Result.failure(AiException(AiErrorCode.PROVIDER_NETWORK_ERROR)), streamResponse = Result.failure(AiException(AiErrorCode.PROVIDER_NETWORK_ERROR))),
            provider("good", 2, Result.success(AiResponse("ok")))
        ))
        val result = manager.stream(AiRequest("hi")) { events += it.type }
        assertTrue(result.isSuccess)
        assertEquals("good", result.getOrNull()?.providerId)
        assertTrue(events.contains(AiStreamEventType.COMPLETE))
    }

    @Test fun dynamicProviderSourceReflectsUpdatedConfiguration() {
        var current = listOf(provider("first", 1, Result.success(AiResponse("one"))))
        val manager = ApiManager({ current })
        assertEquals("first", manager.selectProvider(AiRequest("hi"))?.providerId)
        current = listOf(provider("second", 1, Result.success(AiResponse("two"))))
        assertEquals("second", manager.selectProvider(AiRequest("hi"))?.providerId)
    }

    @Test fun failedProviderIsSkippedDuringCooldown() {
        var calls = 0
        val bad = object : AiProvider by provider("bad", 1, Result.failure(AiException(AiErrorCode.PROVIDER_TIMEOUT))) {
            override fun generate(request: AiRequest): Result<AiResponse> { calls++; return Result.failure(AiException(AiErrorCode.PROVIDER_TIMEOUT)) }
        }
        val good = provider("good", 2, Result.success(AiResponse("ok")))
        val manager = ApiManager(listOf(bad, good))
        assertEquals("good", manager.generate(AiRequest("hi")).getOrNull()?.providerId)
        assertEquals("good", manager.generate(AiRequest("hi")).getOrNull()?.providerId)
        assertEquals(1, calls)
        assertFalse(manager.health("bad").status == ProviderHealthStatus.HEALTHY)
    }
}
