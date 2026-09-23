package com.yusufjamil.aicallassistant

import android.content.Context

class ApiManager(private val context: Context) {
    fun saveKey(providerId: String, apiKey: String) = SecureApiKeyStore.save(context, providerId, apiKey)
    fun hasKey(providerId: String) = !SecureApiKeyStore.read(context, providerId).isNullOrBlank()
    fun deleteKey(providerId: String) = SecureApiKeyStore.delete(context, providerId)
    fun keyConfigured(providerId: String): ProviderConnectionResult {
        val config = ProviderCatalog.all.firstOrNull { it.id == providerId }
            ?: return ProviderConnectionResult(providerId, false, "Unknown provider")
        val present = hasKey(providerId)
        val message = if (present) config.displayName + ": key stored securely; live provider test is pending"
        else config.displayName + ": API key not configured"
        return ProviderConnectionResult(providerId, present, message)
    }
}