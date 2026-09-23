package com.yusufjamil.aicallassistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import android.util.Base64

/**
 * Speech-to-Text provider implementations.
 */

// ============================================
// DEEPGRAM
// ============================================
object DeepgramSttProvider : SpeechToTextProvider {
    override val id: String = "deepgram"
    override val displayName: String = "DEEPGRAM"
    override val category: ProviderCategory = ProviderCategory.STT
    
    override suspend fun testConnection(context: Context): ConnectionStatus {
        val key = SecureApiKeyStore.read(context, id, "api_key")
        if (key.isNullOrBlank()) return ConnectionStatus.NOT_CONFIGURED
        
        return try {
            val url = "https://api.deepgram.com/v1/projects"
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.setRequestProperty("Authorization", "Token $key")
            
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
    
    override suspend fun transcribe(audio: ByteArray, language: String): SpeechResult {
        throw UnsupportedOperationException("Use ProviderApiClient for actual calls")
    }
}

// ============================================
// GOOGLE CLOUD SPEECH-TO-TEXT
// ============================================
object GoogleCloudSttProvider : SpeechToTextProvider {
    override val id: String = "google_stt"
    override val displayName: String = "GOOGLE CLOUD SPEECH-TO-TEXT"
    override val category: ProviderCategory = ProviderCategory.STT
    
    override suspend fun testConnection(context: Context): ConnectionStatus {
        val key = SecureApiKeyStore.read(context, id, "api_key")
        if (key.isNullOrBlank()) return ConnectionStatus.NOT_CONFIGURED
        
        return try {
            val url = "https://speech.googleapis.com/v1/speech:recognize?key=${java.net.URLEncoder.encode(key, "UTF-8")}"
            val body = JSONObject().apply {
                put("config", JSONObject().apply {
                    put("encoding", "LINEAR16")
                    put("sampleRateHertz", 16000)
                    put("languageCode", "en-US")
                })
                put("audio", JSONObject().apply {
                    put("content", Base64.encodeToString(ByteArray(3200), Base64.NO_WRAP))
                })
            }
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.setRequestProperty("Content-Type", "application/json")
            
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(body.toString())
            }
            
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
    
    override suspend fun transcribe(audio: ByteArray, language: String): SpeechResult {
        throw UnsupportedOperationException("Use ProviderApiClient for actual calls")
    }
}

// ============================================
// ASSEMBLYAI
// ============================================
object AssemblyAiSttProvider : SpeechToTextProvider {
    override val id: String = "assemblyai"
    override val displayName: String = "ASSEMBLYAI"
    override val category: ProviderCategory = ProviderCategory.STT
    
    override suspend fun testConnection(context: Context): ConnectionStatus {
        val key = SecureApiKeyStore.read(context, id, "api_key")
        if (key.isNullOrBlank()) return ConnectionStatus.NOT_CONFIGURED
        
        return try {
            val url = "https://api.assemblyai.com/v2/transcript"
            val body = JSONObject().apply {
                put("audio_url", "https://example.com/audio.wav")
            }
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", key)
            
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
    
    override suspend fun transcribe(audio: ByteArray, language: String): SpeechResult {
        throw UnsupportedOperationException("Use ProviderApiClient for actual calls")
    }
}

// ============================================
// ELEVENLABS SCRIBE
// ============================================
object ElevenLabsScribeSttProvider : SpeechToTextProvider {
    override val id: String = "elevenlabs_scribe"
    override val displayName: String = "ELEVENLABS SCRIBE"
    override val category: ProviderCategory = ProviderCategory.STT
    
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
    
    override suspend fun transcribe(audio: ByteArray, language: String): SpeechResult {
        throw UnsupportedOperationException("Use ProviderApiClient for actual calls")
    }
}

// ============================================
// GROQ WHISPER
// ============================================
object GroqWhisperSttProvider : SpeechToTextProvider {
    override val id: String = "groq_whisper"
    override val displayName: String = "GROQ WHISPER"
    override val category: ProviderCategory = ProviderCategory.STT
    
    override suspend fun testConnection(context: Context): ConnectionStatus {
        val key = SecureApiKeyStore.read(context, id, "api_key")
        if (key.isNullOrBlank()) return ConnectionStatus.NOT_CONFIGURED
        
        return try {
            val url = "https://api.groq.com/openai/v1/models"
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
    
    override suspend fun transcribe(audio: ByteArray, language: String): SpeechResult {
        throw UnsupportedOperationException("Use ProviderApiClient for actual calls")
    }
}

// ============================================
// MISTRAL VOXTRAL
// ============================================
object MistralVoxtralSttProvider : SpeechToTextProvider {
    override val id: String = "mistral_voxtral"
    override val displayName: String = "MISTRAL VOXTRAL"
    override val category: ProviderCategory = ProviderCategory.STT
    
    override suspend fun testConnection(context: Context): ConnectionStatus {
        val key = SecureApiKeyStore.read(context, id, "api_key")
        if (key.isNullOrBlank()) return ConnectionStatus.NOT_CONFIGURED
        
        return try {
            val url = "https://api.mistral.ai/v1/models"
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
    
    override suspend fun transcribe(audio: ByteArray, language: String): SpeechResult {
        throw UnsupportedOperationException("Use ProviderApiClient for actual calls")
    }
}

// ============================================
// OPENAI WHISPER
// ============================================
object OpenAiWhisperSttProvider : SpeechToTextProvider {
    override val id: String = "openai_whisper"
    override val displayName: String = "OPENAI WHISPER / OPENAI SPEECH-TO-TEXT"
    override val category: ProviderCategory = ProviderCategory.STT
    
    override suspend fun testConnection(context: Context): ConnectionStatus {
        val key = SecureApiKeyStore.read(context, id, "api_key")
        if (key.isNullOrBlank()) return ConnectionStatus.NOT_CONFIGURED
        
        return try {
            val url = "https://api.openai.com/v1/models"
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
    
    override suspend fun transcribe(audio: ByteArray, language: String): SpeechResult {
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
