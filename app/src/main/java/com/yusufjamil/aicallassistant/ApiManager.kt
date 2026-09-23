package com.yusufjamil.aicallassistant

import android.content.Context
import kotlinx.coroutines.runBlocking

/**
 * API Manager for managing provider credentials and connections.
 * Uses Android Keystore-backed encryption for secure credential storage.
 */
class ApiManager(private val context: Context) {
    
    /**
     * Save a credential field for a provider.
     */
    fun saveCredential(providerId: String, fieldName: String, value: String): Boolean {
        if (value.isBlank()) return false
        SecureApiKeyStore.save(context, providerId, fieldName, value.trim())
        return true
    }
    
    /**
     * Get a credential field for a provider.
     */
    fun getCredential(providerId: String, fieldName: String): String? {
        return SecureApiKeyStore.read(context, providerId, fieldName)
    }
    
    /**
     * Check if a provider has all required credentials configured.
     */
    fun hasAllCredentials(providerId: String): Boolean {
        val config = ProviderCatalog.getById(providerId) ?: return false
        return config.credentialFields.all { field ->
            !field.isSecret || getCredential(providerId, field.name)?.isNotBlank() == true
        }
    }
    
    /**
     * Check if a specific credential field is configured.
     */
    fun hasCredential(providerId: String, fieldName: String): Boolean {
        return getCredential(providerId, fieldName)?.isNotBlank() == true
    }
    
    /**
     * Delete a credential field for a provider.
     */
    fun deleteCredential(providerId: String, fieldName: String) {
        SecureApiKeyStore.delete(context, providerId, fieldName)
    }
    
    /**
     * Delete all credentials for a provider.
     */
    fun deleteAllCredentials(providerId: String) {
        SecureApiKeyStore.deleteAll(context, providerId)
    }
    
    /**
     * Test a providers connection.
     */
    fun testConnection(providerId: String): ConnectionStatus {
        val provider = getProviderById(providerId) ?: return ConnectionStatus.NOT_CONFIGURED
        return runBlocking {
            provider.testConnection(context)
        }
    }
    
    /**
     * Get the connection status for a provider.
     */
    fun getConnectionStatus(providerId: String): ConnectionStatus {
        if (!hasAllCredentials(providerId)) {
            return ConnectionStatus.NOT_CONFIGURED
        }
        return testConnection(providerId)
    }
    
    /**
     * Get a provider by ID.
     */
    private fun getProviderById(providerId: String): Any? {
        return when (providerId) {
            "groq" -> GroqBrainProvider
            "gemini" -> GoogleGeminiBrainProvider
            "sambanova" -> SambaNovaBrainProvider
            "zhipu" -> ZhipuBrainProvider
            "mistral" -> MistralBrainProvider
            "openrouter" -> OpenRouterBrainProvider
            "deepgram" -> DeepgramSttProvider
            "google_stt" -> GoogleCloudSttProvider
            "assemblyai" -> AssemblyAiSttProvider
            "elevenlabs_scribe" -> ElevenLabsScribeSttProvider
            "groq_whisper" -> GroqWhisperSttProvider
            "mistral_voxtral" -> MistralVoxtralSttProvider
            "openai_whisper" -> OpenAiWhisperSttProvider
            "elevenlabs" -> ElevenLabsTtsProvider
            "google_tts" -> GoogleCloudTtsProvider
            "azure_speech" -> AzureSpeechTtsProvider
            "amazon_polly" -> AmazonPollyTtsProvider
            "fish_audio" -> FishAudioTtsProvider
            "cartesia" -> CartesiaTtsProvider
            "rime" -> RimeTtsProvider
            else -> null
        }
    }
    
    /**
     * Get all providers grouped by category.
     */
    fun getProvidersByCategory(category: ProviderCategory): List<ProviderConfig> {
        return ProviderCatalog.getByCategory(category)
    }
    
    /**
     * Get all providers.
     */
    fun getAllProviders(): List<ProviderConfig> {
        return ProviderCatalog.all
    }
    
    /**
     * Get brain providers.
     */
    fun getBrainProviders(): List<ProviderConfig> {
        return ProviderCatalog.brainProviders
    }
    
    /**
     * Get STT providers.
     */
    fun getSttProviders(): List<ProviderConfig> {
        return ProviderCatalog.sttProviders
    }
    
    /**
     * Get TTS providers.
     */
    fun getTtsProviders(): List<ProviderConfig> {
        return ProviderCatalog.ttsProviders
    }
    
    /**
     * Get the primary and backup providers for each category.
     */
    fun getRouting(): Map<ProviderCategory, Pair<String, String>> {
        return mapOf(
            ProviderCategory.BRAIN to Pair(
                AppSettings.primaryBrain(context),
                AppSettings.backupBrain(context)
            ),
            ProviderCategory.STT to Pair(
                AppSettings.primaryStt(context),
                AppSettings.backupStt(context)
            ),
            ProviderCategory.TTS to Pair(
                AppSettings.primaryTts(context),
                AppSettings.backupTts(context)
            )
        )
    }
    
    /**
     * Set the primary and backup providers for a category.
     */
    fun setRouting(category: ProviderCategory, primary: String, backup: String) {
        when (category) {
            ProviderCategory.BRAIN -> AppSettings.saveBrainRouting(context, primary, backup)
            ProviderCategory.STT -> AppSettings.saveSttRouting(context, primary, backup)
            ProviderCategory.TTS -> AppSettings.saveTtsRouting(context, primary, backup)
        }
    }
    
    /**
     * Get the masked credential for display.
     */
    fun getMaskedCredential(providerId: String, fieldName: String): String {
        return getCredential(providerId, fieldName)?.let { maskApiKey(it) } ?: ""
    }
    
    /**
     * Check if a provider is configured (has all required credentials).
     */
    fun isProviderConfigured(providerId: String): Boolean {
        return hasAllCredentials(providerId)
    }
}
