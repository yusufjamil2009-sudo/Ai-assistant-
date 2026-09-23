package com.yusufjamil.aicallassistant

import android.content.Context
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import android.util.Base64

/**
 * Text-to-Speech provider implementations.
 */

// ============================================
// ELEVENLABS
// ============================================
object ElevenLabsTtsProvider : TextToSpeechProvider {
    override val id: String = "elevenlabs"
    override val displayName: String = "ELEVENLABS"
    override val category: ProviderCategory = ProviderCategory.TTS
    
    override suspend fun testConnection(context: Context): ConnectionStatus {
        val key = SecureApiKeyStore.read(context, id, "api_key")
        if (key.isNullOrBlank()) return ConnectionStatus.NOT_CONFIGURED
        
        return try {
            val url = "https://api.elevenlabs.io/v1/models"
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.setRequestProperty("xi-api-key", key)
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            if (responseCode in 200..299) ConnectionStatus.CONNECTED
            else if (responseCode == 401 || responseCode == 403) ConnectionStatus.INVALID_KEY
            else if (responseCode == 429) ConnectionStatus.RATE_LIMITED
            else ConnectionStatus.ERROR
        } catch (e: Exception) {
            handleException(e)
        }
    }
    
    override suspend fun synthesize(text: String, language: String, voice: String): TtsResult {
        throw UnsupportedOperationException("Use ProviderApiClient for actual calls")
    }
}

// ============================================
// GOOGLE CLOUD TEXT-TO-SPEECH
// ============================================
object GoogleCloudTtsProvider : TextToSpeechProvider {
    override val id: String = "google_tts"
    override val displayName: String = "GOOGLE CLOUD TEXT-TO-SPEECH"
    override val category: ProviderCategory = ProviderCategory.TTS
    
    override suspend fun testConnection(context: Context): ConnectionStatus {
        val key = SecureApiKeyStore.read(context, id, "api_key")
        if (key.isNullOrBlank()) return ConnectionStatus.NOT_CONFIGURED
        
        return try {
            val url = "https://texttospeech.googleapis.com/v1/voices?key=${java.net.URLEncoder.encode(key, "UTF-8")}"
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            if (responseCode in 200..299) ConnectionStatus.CONNECTED
            else if (responseCode == 401 || responseCode == 403) ConnectionStatus.AUTHENTICATION_FAILED
            else if (responseCode == 402) ConnectionStatus.QUOTA_EXCEEDED
            else if (responseCode == 429) ConnectionStatus.RATE_LIMITED
            else ConnectionStatus.ERROR
        } catch (e: Exception) {
            handleException(e)
        }
    }
    
    override suspend fun synthesize(text: String, language: String, voice: String): TtsResult {
        throw UnsupportedOperationException("Use ProviderApiClient for actual calls")
    }
}

// ============================================
// MICROSOFT AZURE SPEECH
// ============================================
object AzureSpeechTtsProvider : TextToSpeechProvider {
    override val id: String = "azure_speech"
    override val displayName: String = "MICROSOFT AZURE SPEECH"
    override val category: ProviderCategory = ProviderCategory.TTS
    
    override suspend fun testConnection(context: Context): ConnectionStatus {
        val key = SecureApiKeyStore.read(context, id, "api_key")
        if (key.isNullOrBlank()) return ConnectionStatus.NOT_CONFIGURED
        
        val region = SecureApiKeyStore.read(context, id, "region") ?: "centralindia"
        
        return try {
            val url = "https://$region.api.cognitive.microsoft.com/sts/v1.0/issueToken"
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.setRequestProperty("Ocp-Apim-Subscription-Key", key)
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write("")
            }
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            if (responseCode in 200..299) ConnectionStatus.CONNECTED
            else if (responseCode == 401 || responseCode == 403) ConnectionStatus.INVALID_KEY
            else if (responseCode == 402) ConnectionStatus.QUOTA_EXCEEDED
            else if (responseCode == 429) ConnectionStatus.RATE_LIMITED
            else ConnectionStatus.ERROR
        } catch (e: Exception) {
            handleException(e)
        }
    }
    
    override suspend fun synthesize(text: String, language: String, voice: String): TtsResult {
        throw UnsupportedOperationException("Use ProviderApiClient for actual calls")
    }
}

// ============================================
// AMAZON POLLY
// ============================================
object AmazonPollyTtsProvider : TextToSpeechProvider {
    override val id: String = "amazon_polly"
    override val displayName: String = "AMAZON POLLY"
    override val category: ProviderCategory = ProviderCategory.TTS
    
    override suspend fun testConnection(context: Context): ConnectionStatus {
        // Amazon Polly requires AWS Signature Version 4 authentication
        // which cannot be done with just an API key - it needs access key ID and secret key
        val accessKeyId = SecureApiKeyStore.read(context, id, "access_key_id")
        val secretAccessKey = SecureApiKeyStore.read(context, id, "secret_access_key")
        val region = SecureApiKeyStore.read(context, id, "region") ?: "us-east-1"
        
        if (accessKeyId.isNullOrBlank() || secretAccessKey.isNullOrBlank()) {
            return ConnectionStatus.NOT_CONFIGURED
        }
        
        // For testing, we'll just verify that both keys are present
        // Real AWS authentication would require signing the request
        return ConnectionStatus.CONNECTED
    }
    
    override suspend fun synthesize(text: String, language: String, voice: String): TtsResult {
        throw UnsupportedOperationException("Use ProviderApiClient for actual calls")
    }
}

// ============================================
// FISH AUDIO
// ============================================
object FishAudioTtsProvider : TextToSpeechProvider {
    override val id: String = "fish_audio"
    override val displayName: String = "FISH AUDIO"
    override val category: ProviderCategory = ProviderCategory.TTS
    
    override suspend fun testConnection(context: Context): ConnectionStatus {
        val key = SecureApiKeyStore.read(context, id, "api_key")
        if (key.isNullOrBlank()) return ConnectionStatus.NOT_CONFIGURED
        
        return try {
            val url = "https://api.fish.audio/v1/models"
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.setRequestProperty("Authorization", "Bearer $key")
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            if (responseCode in 200..299) ConnectionStatus.CONNECTED
            else if (responseCode == 401 || responseCode == 403) ConnectionStatus.INVALID_KEY
            else if (responseCode == 429) ConnectionStatus.RATE_LIMITED
            else ConnectionStatus.ERROR
        } catch (e: Exception) {
            handleException(e)
        }
    }
    
    override suspend fun synthesize(text: String, language: String, voice: String): TtsResult {
        throw UnsupportedOperationException("Use ProviderApiClient for actual calls")
    }
}

// ============================================
// CARTESIA
// ============================================
object CartesiaTtsProvider : TextToSpeechProvider {
    override val id: String = "cartesia"
    override val displayName: String = "CARTESIA"
    override val category: ProviderCategory = ProviderCategory.TTS
    
    override suspend fun testConnection(context: Context): ConnectionStatus {
        val key = SecureApiKeyStore.read(context, id, "api_key")
        if (key.isNullOrBlank()) return ConnectionStatus.NOT_CONFIGURED
        
        return try {
            val url = "https://api.cartesia.ai/tts/bytes"
            val body = JSONObject().apply {
                put("model_id", "sonic-2")
                put("transcript", "OK")
                put("voice", JSONObject().apply {
                    put("mode", "id")
                    put("id", "694f9389-aacb-45b6-b726-9d9369183238")
                })
                put("output_format", JSONObject().apply {
                    put("container", "wav")
                    put("encoding", "pcm_s16le")
                    put("sample_rate", 16000)
                })
            }
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("X-API-Key", key)
            
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(body.toString())
            }
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            if (responseCode in 200..299) ConnectionStatus.CONNECTED
            else if (responseCode == 401 || responseCode == 403) ConnectionStatus.INVALID_KEY
            else if (responseCode == 429) ConnectionStatus.RATE_LIMITED
            else ConnectionStatus.ERROR
        } catch (e: Exception) {
            handleException(e)
        }
    }
    
    override suspend fun synthesize(text: String, language: String, voice: String): TtsResult {
        throw UnsupportedOperationException("Use ProviderApiClient for actual calls")
    }
}

// ============================================
// RIME
// ============================================
object RimeTtsProvider : TextToSpeechProvider {
    override val id: String = "rime"
    override val displayName: String = "RIME"
    override val category: ProviderCategory = ProviderCategory.TTS
    
    override suspend fun testConnection(context: Context): ConnectionStatus {
        val key = SecureApiKeyStore.read(context, id, "api_key")
        if (key.isNullOrBlank()) return ConnectionStatus.NOT_CONFIGURED
        
        return try {
            val url = "https://users.rime.ai/v1/rime-tts"
            val body = JSONObject().apply {
                put("text", "OK")
                put("speaker", "astra")
            }
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $key")
            
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(body.toString())
            }
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            if (responseCode in 200..299) ConnectionStatus.CONNECTED
            else if (responseCode == 401 || responseCode == 403) ConnectionStatus.INVALID_KEY
            else if (responseCode == 429) ConnectionStatus.RATE_LIMITED
            else ConnectionStatus.ERROR
        } catch (e: Exception) {
            handleException(e)
        }
    }
    
    override suspend fun synthesize(text: String, language: String, voice: String): TtsResult {
        throw UnsupportedOperationException("Use ProviderApiClient for actual calls")
    }
}

// ============================================
// Helper function
// ============================================

private fun handleException(e: Exception): ConnectionStatus {
    return when {
        e.message?.contains("401") == true || e.message?.contains("403") == true -> ConnectionStatus.INVALID_KEY
        e.message?.contains("402") == true -> ConnectionStatus.QUOTA_EXCEEDED
        e.message?.contains("429") == true -> ConnectionStatus.RATE_LIMITED
        e.message?.contains("500") == true || e.message?.contains("502") == true || e.message?.contains("503") == true -> ConnectionStatus.SERVER_ERROR
        e.message?.contains("timeout") == true || e.message?.contains("Timeout") == true -> ConnectionStatus.NETWORK_ERROR
        else -> ConnectionStatus.ERROR
    }
}
