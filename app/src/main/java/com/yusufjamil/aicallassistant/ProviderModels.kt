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

    fun save(context: Context, providerId: String, value: String) {
        if (value.isBlank()) return
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key(), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(providerId, Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
                Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    fun read(context: Context, providerId: String): String? {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(providerId, null) ?: return null
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

    fun delete(context: Context, providerId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(providerId).apply()
    }
}

data class ProviderConnectionResult(
    val providerId: String,
    val success: Boolean,
    val message: String
)

data class ProviderConfig(
    val id: String,
    val category: ProviderCategory,
    val displayName: String,
    val supportsConnectionTest: Boolean = true
)

enum class ProviderCategory { BRAIN, STT, TTS }

object ProviderCatalog {
    val all = listOf(
        ProviderConfig("groq", ProviderCategory.BRAIN, "Groq"),
        ProviderConfig("gemini", ProviderCategory.BRAIN, "Gemini"),
        ProviderConfig("sambanova", ProviderCategory.BRAIN, "SambaNova"),
        ProviderConfig("zhipu", ProviderCategory.BRAIN, "Zhipu"),
        ProviderConfig("mistral", ProviderCategory.BRAIN, "Mistral"),
        ProviderConfig("openrouter", ProviderCategory.BRAIN, "OpenRouter"),
        ProviderConfig("deepgram", ProviderCategory.STT, "Deepgram"),
        ProviderConfig("google_stt", ProviderCategory.STT, "Google Cloud Speech-to-Text"),
        ProviderConfig("assemblyai", ProviderCategory.STT, "AssemblyAI"),
        ProviderConfig("elevenlabs_scribe", ProviderCategory.STT, "ElevenLabs Scribe"),
        ProviderConfig("groq_whisper", ProviderCategory.STT, "Groq Whisper"),
        ProviderConfig("mistral_voxtral", ProviderCategory.STT, "Mistral Voxtral"),
        ProviderConfig("openai_whisper", ProviderCategory.STT, "OpenAI Whisper/API"),
        ProviderConfig("elevenlabs", ProviderCategory.TTS, "ElevenLabs"),
        ProviderConfig("google_tts", ProviderCategory.TTS, "Google Cloud TTS"),
        ProviderConfig("azure_speech", ProviderCategory.TTS, "Microsoft Azure Speech"),
        ProviderConfig("amazon_polly", ProviderCategory.TTS, "Amazon Polly"),
        ProviderConfig("fish_audio", ProviderCategory.TTS, "Fish Audio"),
        ProviderConfig("cartesia", ProviderCategory.TTS, "Cartesia"),
        ProviderConfig("rime", ProviderCategory.TTS, "Rime")
    )
}
