package com.yusufjamil.aicallassistant

import android.content.Context

/**
 * Common interface for Speech-to-Text providers.
 */
interface SpeechToTextProvider {
    val id: String
    val displayName: String
    val category: ProviderCategory
    
    /**
     * Test the providers API connection.
     */
    suspend fun testConnection(context: Context): ConnectionStatus
    
    /**
     * Transcribe audio to text.
     */
    suspend fun transcribe(
        audio: ByteArray,
        language: String
    ): SpeechResult
}

/**
 * Common interface for Brain/LLM providers.
 */
interface BrainProvider {
    val id: String
    val displayName: String
    val category: ProviderCategory
    
    /**
     * Test the providers API connection.
     */
    suspend fun testConnection(context: Context): ConnectionStatus
    
    /**
     * Generate a response from the AI.
     */
    suspend fun generate(
        systemPrompt: String,
        userText: String
    ): BrainResult
    
    /**
     * Get the list of supported models.
     */
    fun getSupportedModels(): List<String>
    
    /**
     * Set the model to use.
     */
    fun setModel(model: String)
}

/**
 * Common interface for Text-to-Speech providers.
 */
interface TextToSpeechProvider {
    val id: String
    val displayName: String
    val category: ProviderCategory
    
    /**
     * Test the providers API connection.
     */
    suspend fun testConnection(context: Context): ConnectionStatus
    
    /**
     * Synthesize text to speech.
     */
    suspend fun synthesize(
        text: String,
        language: String,
        voice: String
    ): TtsResult
}

/**
 * Provider configuration with credential fields.
 */
data class ProviderConfig(
    val id: String,
    val category: ProviderCategory,
    val displayName: String,
    val supportsConnectionTest: Boolean = true,
    val credentialFields: List<CredentialField> = emptyList(),
    val defaultModel: String? = null
)

/**
 * Definition of a credential field for a provider.
 */
data class CredentialField(
    val name: String,
    val label: String,
    val isSecret: Boolean = true,
    val placeholder: String = "",
    val validationRegex: String? = null
)

/**
 * Credentials for a specific provider.
 */
data class ProviderCredentials(
    val providerId: String,
    val fields: Map<String, String> = emptyMap()
)
