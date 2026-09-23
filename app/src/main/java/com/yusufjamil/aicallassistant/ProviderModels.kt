package com.yusufjamil.aicallassistant

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Secure storage for API keys using Android Keystore-backed encryption.
 */
object SecureApiKeyStore {
    private const val PREFS = "secure_api_keys"
    private const val KEY_ALIAS = "ai_call_assistant_api_keys_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun key(): SecretKey {
        val ks = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        generator.init(android.security.keystore.KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .build())
        return generator.generateKey()
    }

    fun save(context: Context, providerId: String, fieldName: String, value: String) {
        if (value.isBlank()) return
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key(), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        val keyName = "${providerId}_$fieldName"
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(keyName, Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
                Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    fun read(context: Context, providerId: String, fieldName: String): String? {
        val keyName = "${providerId}_$fieldName"
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(keyName, null) ?: return null
        return try {
            val parts = stored.split(":")
            if (parts.size != 2) return null
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
            Cipher.getInstance(TRANSFORMATION).run {
                init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
                String(doFinal(encrypted), StandardCharsets.UTF_8)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun delete(context: Context, providerId: String, fieldName: String) {
        val keyName = "${providerId}_$fieldName"
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(keyName).apply()
    }

    fun deleteAll(context: Context, providerId: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val allKeys = prefs.all.keys.filter { it.startsWith("$providerId_") }
        prefs.edit().apply {
            allKeys.forEach { remove(it) }
        }.apply()
    }
}

/**
 * Result of checking if a provider has credentials configured.
 */
data class ProviderConnectionResult(
    val providerId: String,
    val success: Boolean,
    val message: String,
    val status: ConnectionStatus = if (success) ConnectionStatus.CONNECTED else ConnectionStatus.NOT_CONFIGURED
)

/**
 * Provider categories.
 */
enum class ProviderCategory { BRAIN, STT, TTS }

/**
 * Catalog of all supported providers with their configurations.
 */
object ProviderCatalog {
    
    // Brain / LLM Providers (6)
    val groq = ProviderConfig(
        id = "groq",
        category = ProviderCategory.BRAIN,
        displayName = "GROQ",
        credentialFields = listOf(
            CredentialField("api_key", "API Key", true, "Enter your Groq API key")
        ),
        defaultModel = "llama-3.3-70b-versatile"
    )
    
    val googleGemini = ProviderConfig(
        id = "gemini",
        category = ProviderCategory.BRAIN,
        displayName = "GOOGLE GEMINI",
        credentialFields = listOf(
            CredentialField("api_key", "API Key", true, "Enter your Google Gemini API key")
        ),
        defaultModel = "gemini-2.5-flash"
    )
    
    val sambanova = ProviderConfig(
        id = "sambanova",
        category = ProviderCategory.BRAIN,
        displayName = "SAMBANOVA",
        credentialFields = listOf(
            CredentialField("api_key", "API Key", true, "Enter your SambaNova API key")
        ),
        defaultModel = "Meta-Llama-3.1-405B-Instruct"
    )
    
    val zhipu = ProviderConfig(
        id = "zhipu",
        category = ProviderCategory.BRAIN,
        displayName = "Z.AI / ZHIPU",
        credentialFields = listOf(
            CredentialField("api_key", "API Key", true, "Enter your Z.AI / Zhipu API key")
        ),
        defaultModel = "glm-4.6"
    )
    
    val mistral = ProviderConfig(
        id = "mistral",
        category = ProviderCategory.BRAIN,
        displayName = "MISTRAL AI",
        credentialFields = listOf(
            CredentialField("api_key", "API Key", true, "Enter your Mistral AI API key")
        ),
        defaultModel = "mistral-small-latest"
    )
    
    val openrouter = ProviderConfig(
        id = "openrouter",
        category = ProviderCategory.BRAIN,
        displayName = "OPENROUTER",
        credentialFields = listOf(
            CredentialField("api_key", "API Key", true, "Enter your OpenRouter API key")
        ),
        defaultModel = "openai/gpt-oss-20b"
    )
    
    // STT / Speech-to-Text Providers (7)
    val deepgram = ProviderConfig(
        id = "deepgram",
        category = ProviderCategory.STT,
        displayName = "DEEPGRAM",
        credentialFields = listOf(
            CredentialField("api_key", "API Key", true, "Enter your Deepgram API key")
        )
    )
    
    val googleCloudStt = ProviderConfig(
        id = "google_stt",
        category = ProviderCategory.STT,
        displayName = "GOOGLE CLOUD SPEECH-TO-TEXT",
        credentialFields = listOf(
            CredentialField("api_key", "API Key", true, "Enter your Google Cloud STT API key")
        )
    )
    
    val assemblyai = ProviderConfig(
        id = "assemblyai",
        category = ProviderCategory.STT,
        displayName = "ASSEMBLYAI",
        credentialFields = listOf(
            CredentialField("api_key", "API Key", true, "Enter your AssemblyAI API key")
        )
    )
    
    val elevenlabsScribe = ProviderConfig(
        id = "elevenlabs_scribe",
        category = ProviderCategory.STT,
        displayName = "ELEVENLABS SCRIBE",
        credentialFields = listOf(
            CredentialField("api_key", "API Key", true, "Enter your ElevenLabs API key", "xi-api-key")
        )
    )
    
    val groqWhisper = ProviderConfig(
        id = "groq_whisper",
        category = ProviderCategory.STT,
        displayName = "GROQ WHISPER",
        credentialFields = listOf(
            CredentialField("api_key", "API Key", true, "Enter your Groq API key")
        )
    )
    
    val mistralVoxtral = ProviderConfig(
        id = "mistral_voxtral",
        category = ProviderCategory.STT,
        displayName = "MISTRAL VOXTRAL",
        credentialFields = listOf(
            CredentialField("api_key", "API Key", true, "Enter your Mistral API key")
        )
    )
    
    val openaiWhisper = ProviderConfig(
        id = "openai_whisper",
        category = ProviderCategory.STT,
        displayName = "OPENAI WHISPER / OPENAI SPEECH-TO-TEXT",
        credentialFields = listOf(
            CredentialField("api_key", "API Key", true, "Enter your OpenAI API key")
        )
    )
    
    // TTS / Text-to-Speech Providers (7)
    val elevenlabs = ProviderConfig(
        id = "elevenlabs",
        category = ProviderCategory.TTS,
        displayName = "ELEVENLABS",
        credentialFields = listOf(
            CredentialField("api_key", "API Key", true, "Enter your ElevenLabs API key", "xi-api-key")
        )
    )
    
    val googleCloudTts = ProviderConfig(
        id = "google_tts",
        category = ProviderCategory.TTS,
        displayName = "GOOGLE CLOUD TEXT-TO-SPEECH",
        credentialFields = listOf(
            CredentialField("api_key", "API Key", true, "Enter your Google Cloud TTS API key")
        )
    )
    
    val azureSpeech = ProviderConfig(
        id = "azure_speech",
        category = ProviderCategory.TTS,
        displayName = "MICROSOFT AZURE SPEECH",
        credentialFields = listOf(
            CredentialField("api_key", "Azure Speech API Key", true, "Enter your Azure Speech API key"),
            CredentialField("region", "Azure Region", false, "centralindia", "^[a-z0-9-]+$")
        )
    )
    
    val amazonPolly = ProviderConfig(
        id = "amazon_polly",
        category = ProviderCategory.TTS,
        displayName = "AMAZON POLLY",
        credentialFields = listOf(
            CredentialField("access_key_id", "AWS Access Key ID", true, "Enter your AWS Access Key ID"),
            CredentialField("secret_access_key", "AWS Secret Access Key", true, "Enter your AWS Secret Access Key"),
            CredentialField("region", "AWS Region", false, "us-east-1", "^[a-z0-9-]+$")
        )
    )
    
    val fishAudio = ProviderConfig(
        id = "fish_audio",
        category = ProviderCategory.TTS,
        displayName = "FISH AUDIO",
        credentialFields = listOf(
            CredentialField("api_key", "API Key", true, "Enter your Fish Audio API key")
        )
    )
    
    val cartesia = ProviderConfig(
        id = "cartesia",
        category = ProviderCategory.TTS,
        displayName = "CARTESIA",
        credentialFields = listOf(
            CredentialField("api_key", "API Key", true, "Enter your Cartesia API key")
        )
    )
    
    val rime = ProviderConfig(
        id = "rime",
        category = ProviderCategory.TTS,
        displayName = "RIME",
        credentialFields = listOf(
            CredentialField("api_key", "API Key", true, "Enter your Rime API key")
        )
    )
    
    // All providers grouped by category
    val brainProviders: List<ProviderConfig> = listOf(
        groq, googleGemini, sambanova, zhipu, mistral, openrouter
    )
    
    val sttProviders: List<ProviderConfig> = listOf(
        deepgram, googleCloudStt, assemblyai, elevenlabsScribe, groqWhisper, mistralVoxtral, openaiWhisper
    )
    
    val ttsProviders: List<ProviderConfig> = listOf(
        elevenlabs, googleCloudTts, azureSpeech, amazonPolly, fishAudio, cartesia, rime
    )
    
    // All providers in one list
    val all: List<ProviderConfig> = brainProviders + sttProviders + ttsProviders
    
    /**
     * Get a provider by its ID.
     */
    fun getById(id: String): ProviderConfig? = all.firstOrNull { it.id == id }
    
    /**
     * Get providers by category.
     */
    fun getByCategory(category: ProviderCategory): List<ProviderConfig> = when (category) {
        ProviderCategory.BRAIN -> brainProviders
        ProviderCategory.STT -> sttProviders
        ProviderCategory.TTS -> ttsProviders
    }
}

/**
 * Helper to mask API keys for display.
 */
fun maskApiKey(key: String): String {
    if (key.length <= 4) return "••••"
    return "•".repeat(key.length - 4) + key.takeLast(4)
}
