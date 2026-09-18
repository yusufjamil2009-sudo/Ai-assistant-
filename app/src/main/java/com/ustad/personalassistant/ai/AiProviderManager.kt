package com.ustad.personalassistant.ai

import android.content.Context
import com.ustad.personalassistant.security.SecureConfigStore
import org.json.JSONArray
import org.json.JSONObject

class AiProviderManager(context: Context) {
    private val prefs = context.getSharedPreferences("ustad_ai_providers", Context.MODE_PRIVATE)
    private val secrets = SecureConfigStore(context, "ai_provider_keys")
    private val configs = linkedMapOf<String, ProviderConfig>()
    private val credentialStore = AiCredentialStore(secrets)

    init { load() }
    fun allConfigs(): List<ProviderConfig> = configs.values.sortedWith(compareBy<ProviderConfig> { it.priority }.thenBy { it.providerId })
    fun get(providerId: String): ProviderConfig? = configs[providerId]
    fun routingPolicy(): RoutingPolicy = runCatching { RoutingPolicy.valueOf(prefs.getString("routing_policy", RoutingPolicy.PRIVACY_FIRST.name)!!) }.getOrDefault(RoutingPolicy.PRIVACY_FIRST)
    fun setRoutingPolicy(policy: RoutingPolicy) { prefs.edit().putString("routing_policy", policy.name).apply() }

    fun save(config: ProviderConfig, apiKey: String? = null) {
        require(config.providerId.matches(Regex("[a-z0-9._-]{2,40}")))
        configs[config.providerId] = config.copy(
            displayName = config.displayName.ifBlank { config.providerId },
            model = config.model.trim(),
            endpoint = config.endpoint?.trim()?.ifBlank { null },
            priority = config.priority.coerceIn(1, 999),
            retryCount = config.retryCount.coerceIn(0, 3),
            timeoutMs = config.timeoutMs.coerceIn(1_000L, 120_000L)
        )
        if (apiKey != null) credentialStore.saveApiKey(config.providerId, apiKey)
        persist()
    }
    fun removeApiKey(providerId: String) { credentialStore.removeApiKey(providerId) }
    fun remove(providerId: String) { configs.remove(providerId); credentialStore.removeApiKey(providerId); persist() }
    fun maskedKey(providerId: String): String = credentialStore.masked(providerId)
    fun hasKey(providerId: String): Boolean = credentialStore.hasApiKey(providerId)
    fun providers(): List<AiProvider> = allConfigs().map { config -> HttpAiProvider(config, apiKey = { secrets.get("api_key_${config.providerId}") }) }

    /** Performs a real HTTPS request; an override is tested without being persisted. */
    fun testProvider(providerId: String, apiKeyOverride: String? = null): ProviderTestResult {
        val config = configs[providerId] ?: return ProviderTestResult(providerId, false, "Provider configuration not found.")
        val key = apiKeyOverride?.trim()?.takeIf { it.isNotBlank() } ?: secrets.get("api_key_$providerId")
        if (key.isNullOrBlank()) return ProviderTestResult(providerId, false, "API key is not configured.")
        if (config.endpoint.isNullOrBlank()) return ProviderTestResult(providerId, false, "Provider endpoint is not configured.")
        val testConfig = config.copy(enabled = true, timeoutMs = config.timeoutMs.coerceIn(5_000L, 30_000L), retryCount = 0)
        val result = HttpAiProvider(testConfig, apiKey = { key }).generate(
            AiRequest("Reply with OK only.", setOf(AiCapability.CHAT, AiCapability.TEXT_GENERATION))
        )
        return result.fold(
            { response -> ProviderTestResult(providerId, true, "Live API test passed.", response.providerId, response.model, response.usage) },
            { error -> ProviderTestResult(providerId, false, providerTestMessage(error as? AiException, error.message)) }
        )
    }

    private fun providerTestMessage(error: AiException?, fallback: String?): String = when (error?.code) {
        AiErrorCode.PROVIDER_AUTH_ERROR -> "API key rejected or not authorized by the provider."
        AiErrorCode.PROVIDER_RATE_LIMITED -> "API key was recognized, but the provider rate-limited this request."
        AiErrorCode.PROVIDER_TIMEOUT -> "Provider did not respond before the test timeout."
        AiErrorCode.PROVIDER_NETWORK_ERROR -> "Network/TLS connection failed while testing the provider."
        AiErrorCode.PROVIDER_SERVER_ERROR -> "Provider server returned an error."
        AiErrorCode.PROVIDER_INVALID_RESPONSE -> "Provider responded, but its response format was invalid."
        AiErrorCode.PROVIDER_UNSUPPORTED_CAPABILITY -> "Provider does not support the required test capability."
        else -> fallback ?: "Live API test failed."
    }

    fun buildApiManager(networkState: () -> NetworkState = { NetworkState.ONLINE }): ApiManager = ApiManager({ providers() }, networkState)

    private fun load() {
        val raw = prefs.getString("configs", null)
        if (raw.isNullOrBlank()) { DefaultProviderSlots.configs().forEach { configs[it.providerId] = it }; persist(); return }
        runCatching {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val caps = o.optJSONArray("capabilities")?.let { a -> buildSet { for (j in 0 until a.length()) runCatching { add(AiCapability.valueOf(a.getString(j))) } } } ?: setOf(AiCapability.CHAT, AiCapability.TEXT_GENERATION)
                val config = ProviderConfig(o.getString("providerId"), o.optString("displayName"), o.optBoolean("enabled"), o.optInt("priority", 100), o.optString("model"), o.optString("endpoint").ifBlank { null }, caps, o.optLong("timeoutMs", 30_000L), o.optInt("retryCount", 1))
                configs[config.providerId] = config
            }
            val missing = DefaultProviderSlots.configs().filter { defaults -> configs[defaults.providerId] == null }
            missing.forEach { configs[it.providerId] = it }
            if (missing.isNotEmpty()) persist()
        }.onFailure { configs.clear(); DefaultProviderSlots.configs().forEach { configs[it.providerId] = it }; persist() }
    }
    private fun persist() {
        val array = JSONArray()
        allConfigs().forEach { c -> array.put(JSONObject().apply { put("providerId", c.providerId); put("displayName", c.displayName); put("enabled", c.enabled); put("priority", c.priority); put("model", c.model); put("endpoint", c.endpoint ?: ""); put("timeoutMs", c.timeoutMs); put("retryCount", c.retryCount); put("capabilities", JSONArray(c.capabilities.map { it.name })) }) }
        prefs.edit().putString("configs", array.toString()).apply()
    }
}


data class ProviderTestResult(
    val providerId: String,
    val success: Boolean,
    val message: String,
    val returnedProviderId: String? = null,
    val model: String? = null,
    val usage: AiUsage? = null
)
