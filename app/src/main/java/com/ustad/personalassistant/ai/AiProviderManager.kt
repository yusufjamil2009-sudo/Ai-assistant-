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

    fun allConfigs(): List<ProviderConfig> = configs.values.sortedBy { it.priority }
    fun get(providerId: String): ProviderConfig? = configs[providerId]
    fun routingPolicy(): RoutingPolicy = runCatching { RoutingPolicy.valueOf(prefs.getString("routing_policy", RoutingPolicy.PRIVACY_FIRST.name)!!) }.getOrDefault(RoutingPolicy.PRIVACY_FIRST)
    fun setRoutingPolicy(policy: RoutingPolicy) { prefs.edit().putString("routing_policy", policy.name).apply() }

    fun save(config: ProviderConfig, apiKey: String? = null) {
        require(config.providerId.matches(Regex("[a-z0-9._-]{2,40}")))
        configs[config.providerId] = config.copy(priority = config.priority.coerceIn(1, 999), retryCount = config.retryCount.coerceIn(0, 3), timeoutMs = config.timeoutMs.coerceIn(1_000L, 120_000L))
        if (apiKey != null) {
            if (apiKey.isBlank()) credentialStore.removeApiKey(config.providerId) else credentialStore.saveApiKey(config.providerId, apiKey.trim())
        }
        persist()
    }

    fun remove(providerId: String) { configs.remove(providerId); credentialStore.removeApiKey(providerId); persist() }
    fun maskedKey(providerId: String): String = credentialStore.masked(providerId)
    fun hasKey(providerId: String): Boolean = credentialStore.hasApiKey(providerId)
    fun providers(): List<AiProvider> = allConfigs().map { config -> HttpAiProvider(config, apiKey = { secrets.get("api_key_${config.providerId}") }) }
    fun buildApiManager(networkState: () -> NetworkState = { NetworkState.ONLINE }): ApiManager = ApiManager(providers(), networkState)

    private fun load() {
        val raw = prefs.getString("configs", null)
        if (raw.isNullOrBlank()) {
            DefaultProviderSlots.configs().forEach { configs[it.providerId] = it }
            persist()
            return
        }
        runCatching {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val caps = o.optJSONArray("capabilities")?.let { a -> buildSet { for (j in 0 until a.length()) runCatching { add(AiCapability.valueOf(a.getString(j))) } } } ?: setOf(AiCapability.CHAT, AiCapability.TEXT_GENERATION)
                val config = ProviderConfig(o.getString("providerId"), o.optString("displayName"), o.optBoolean("enabled"), o.optInt("priority", 100), o.optString("model"), o.optString("endpoint").ifBlank { null }, caps, o.optLong("timeoutMs", 30_000L), o.optInt("retryCount", 1))
                configs[config.providerId] = config
            }
        }.onFailure { DefaultProviderSlots.configs().forEach { configs[it.providerId] = it }; persist() }
    }

    private fun persist() {
        val array = JSONArray()
        configs.values.forEach { c -> array.put(JSONObject().apply {
            put("providerId", c.providerId); put("displayName", c.displayName); put("enabled", c.enabled); put("priority", c.priority); put("model", c.model); put("endpoint", c.endpoint ?: ""); put("timeoutMs", c.timeoutMs); put("retryCount", c.retryCount); put("capabilities", JSONArray(c.capabilities.map { it.name }))
        }) }
        prefs.edit().putString("configs", array.toString()).apply()
    }
}
