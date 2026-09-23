package com.yusufjamil.aicallassistant

import android.content.Context

class ApiManager(private val context: Context) {
    fun saveKey(providerId: String, apiKey: String): Boolean {
        if (apiKey.isBlank()) return false
        SecureApiKeyStore.save(context, providerId, apiKey.trim())
        return hasKey(providerId)
    }

    fun hasKey(providerId: String) = !SecureApiKeyStore.read(context, providerId).isNullOrBlank()

    fun deleteKey(providerId: String) = SecureApiKeyStore.delete(context, providerId)

    fun keyConfigured(providerId: String): ProviderConnectionResult {
        val config = ProviderCatalog.all.firstOrNull { it.id == providerId }
            ?: return ProviderConnectionResult(providerId, false, "Unknown provider")
        return if (hasKey(providerId)) {
            ProviderConnectionResult(providerId, true, config.displayName + ": API key stored securely")
        } else {
            ProviderConnectionResult(providerId, false, config.displayName + ": API key not configured")
        }
    }
}
