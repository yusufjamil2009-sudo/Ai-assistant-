package com.ustad.personalassistant.ai

import com.ustad.personalassistant.security.SecureConfigStore

class AiCredentialStore(private val store: SecureConfigStore) {
    fun saveApiKey(providerId: String, apiKey: String) { require(apiKey.isNotBlank()); store.put("api_key_$providerId", apiKey) }
    fun hasApiKey(providerId: String): Boolean = !store.get("api_key_$providerId").isNullOrBlank()
    fun removeApiKey(providerId: String) { store.remove("api_key_$providerId") }
    fun masked(providerId: String): String = if (hasApiKey(providerId)) "••••••••••" else "Not configured"
}
