package com.ustad.personalassistant.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiBrainTest {
    private fun provider(id: String, priority: Int, response: Result<AiResponse>): AiProvider = object : AiProvider {
        override val providerId = id
        override val displayName = id
        override val enabled = true
        override val priority = priority
        override val model = "test"
        override val endpoint: String? = null
        override val capabilities = setOf(AiCapability.CHAT, AiCapability.TEXT_GENERATION)
        override val timeoutMs = 1_000L
        override val retryCount = 0
        override fun isAvailable() = true
        override fun healthCheck() = ProviderHealth(ProviderHealthStatus.HEALTHY)
        override fun generate(request: AiRequest) = response
    }

    @Test fun prioritySelectionUsesLowestPriorityNumber() {
        val manager = ApiManager(listOf(provider("second", 2, Result.success(AiResponse("ok2"))), provider("first", 1, Result.success(AiResponse("ok1")))))
        assertEquals("first", manager.selectProvider(AiRequest("hi"))?.providerId)
    }

    @Test fun failoverUsesNextProvider() {
        val manager = ApiManager(listOf(provider("bad", 1, Result.failure(AiException(AiErrorCode.PROVIDER_TIMEOUT))), provider("good", 2, Result.success(AiResponse("ok")))))
        assertEquals("good", manager.generate(AiRequest("hi")).getOrNull()?.providerId)
    }

    @Test fun invalidSendPlanIsRejected() {
        val valid = DefaultActionPlanValidator()
        assertFalse(valid.validate(ActionPlan("SEND_MESSAGE", "x", requiresConfirmation = false)))
        assertTrue(valid.validate(ActionPlan("SEND_MESSAGE", "x", requiresConfirmation = true)))
    }

    @Test fun contextIsBounded() {
        val context = InMemoryConversationContextManager(3)
        context.addMessage("1"); context.addMessage("2"); context.addMessage("3"); context.addMessage("4")
        assertEquals(listOf("2", "3", "4"), context.getContext().recentMessages)
    }

    @Test fun offlineDoesNotCallProvider() {
        var calls = 0
        val p = object : AiProvider by provider("p", 1, Result.success(AiResponse("ok"))) {
            override fun generate(request: AiRequest): Result<AiResponse> { calls++; return Result.success(AiResponse("ok")) }
        }
        val manager = ApiManager(listOf(p), networkState = { NetworkState.OFFLINE })
        assertEquals(AiErrorCode.OFFLINE, (manager.generate(AiRequest("hi")).exceptionOrNull() as AiException).code)
        assertEquals(0, calls)
    }
}
