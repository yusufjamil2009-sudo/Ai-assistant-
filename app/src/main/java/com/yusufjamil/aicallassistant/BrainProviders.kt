package com.yusufjamil.aicallassistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Brain/LLM provider implementations.
 */

// ============================================
// GROQ
// ============================================
object GroqBrainProvider : BrainProvider {
    override val id: String = "groq"
    override val displayName: String = "GROQ"
    override val category: ProviderCategory = ProviderCategory.BRAIN
    
    private var model: String = ProviderCatalog.groq.defaultModel ?: "llama-3.3-70b-versatile"
    
    override suspend fun testConnection(context: Context): ConnectionStatus {
        val key = SecureApiKeyStore.read(context, id, "api_key")
        if (key.isNullOrBlank()) return ConnectionStatus.NOT_CONFIGURED
        
        return try {
            val url = "https://api.groq.com/openai/v1/chat/completions"
            val body = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "Reply only OK")))
                put("max_completion_tokens", 8)
            }
            val result = postRequest(url, key, body)
            if (result.contains("choices")) ConnectionStatus.CONNECTED else ConnectionStatus.ERROR
        } catch (e: Exception) {
            when {
                e.message?.contains("401") == true || e.message?.contains("403") == true -> ConnectionStatus.INVALID_KEY
                e.message?.contains("429") == true -> ConnectionStatus.RATE_LIMITED
                e.message?.contains("timeout") == true || e.message?.contains("Timeout") == true -> ConnectionStatus.NETWORK_ERROR
                else -> ConnectionStatus.ERROR
            }
        }
    }
    
    override suspend fun generate(systemPrompt: String, userText: String): BrainResult {
        throw UnsupportedOperationException("Use ProviderApiClient for actual calls")
    }
    
    override fun getSupportedModels(): List<String> = listOf(
        "llama-3.3-70b-versatile",
        "llama-3.2-11b-vision",
        "llama-3.2-90b-vision",
        "llama-3.2-3b",
        "llama-3.2-11b",
        "llama-3.2-90b",
        "mixtral-8x7b"
    )
    
    override fun setModel(model: String) {
        if (getSupportedModels().contains(model)) {
            this.model = model
        }
    }
}

// ============================================
// GOOGLE GEMINI
// ============================================
object GoogleGeminiBrainProvider : BrainProvider {
    override val id: String = "gemini"
    override val displayName: String = "GOOGLE GEMINI"
    override val category: ProviderCategory = ProviderCategory.BRAIN
    
    private var model: String = ProviderCatalog.googleGemini.defaultModel ?: "gemini-2.5-flash"
    
    override suspend fun testConnection(context: Context): ConnectionStatus {
        val key = SecureApiKeyStore.read(context, id, "api_key")
        if (key.isNullOrBlank()) return ConnectionStatus.NOT_CONFIGURED
        
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${java.net.URLEncoder.encode(key, "UTF-8")}"
            val body = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", "Reply only OK")))))
            }
            val result = postRequest(url, key, body, useBearer = false)
            if (result.isNotBlank()) ConnectionStatus.CONNECTED else ConnectionStatus.ERROR
        } catch (e: Exception) {
            when {
                e.message?.contains("401") == true || e.message?.contains("403") == true -> ConnectionStatus.AUTHENTICATION_FAILED
                e.message?.contains("429") == true -> ConnectionStatus.RATE_LIMITED
                e.message?.contains("timeout") == true || e.message?.contains("Timeout") == true -> ConnectionStatus.NETWORK_ERROR
                else -> ConnectionStatus.ERROR
            }
        }
    }
    
    override suspend fun generate(systemPrompt: String, userText: String): BrainResult {
        throw UnsupportedOperationException("Use ProviderApiClient for actual calls")
    }
    
    override fun getSupportedModels(): List<String> = listOf(
        "gemini-2.5-flash",
        "gemini-2.5-pro",
        "gemini-2.0-flash",
        "gemini-2.0-pro",
        "gemini-1.5-flash",
        "gemini-1.5-pro"
    )
    
    override fun setModel(model: String) {
        if (getSupportedModels().contains(model)) {
            this.model = model
        }
    }
}

// ============================================
// SAMBANOVA
// ============================================
object SambaNovaBrainProvider : BrainProvider {
    override val id: String = "sambanova"
    override val displayName: String = "SAMBANOVA"
    override val category: ProviderCategory = ProviderCategory.BRAIN
    
    private var model: String = ProviderCatalog.sambanova.defaultModel ?: "Meta-Llama-3.1-405B-Instruct"
    
    override suspend fun testConnection(context: Context): ConnectionStatus {
        val key = SecureApiKeyStore.read(context, id, "api_key")
        if (key.isNullOrBlank()) return ConnectionStatus.NOT_CONFIGURED
        
        return try {
            val url = "https://api.sambanova.ai/v1/chat/completions"
            val body = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "Reply only OK")))
                put("max_tokens", 8)
            }
            val result = postRequest(url, key, body)
            if (result.contains("choices")) ConnectionStatus.CONNECTED else ConnectionStatus.ERROR
        } catch (e: Exception) {
            handleException(e)
        }
    }
    
    override suspend fun generate(systemPrompt: String, userText: String): BrainResult {
        throw UnsupportedOperationException("Use ProviderApiClient for actual calls")
    }
    
    override fun getSupportedModels(): List<String> = listOf(
        "Meta-Llama-3.1-405B-Instruct",
        "Meta-Llama-3.1-70B-Instruct",
        "Meta-Llama-3-70B-Instruct",
        "Meta-Llama-3-8B-Instruct"
    )
    
    override fun setModel(model: String) {
        if (getSupportedModels().contains(model)) {
            this.model = model
        }
    }
}

// ============================================
// Z.AI / ZHIPU
// ============================================
object ZhipuBrainProvider : BrainProvider {
    override val id: String = "zhipu"
    override val displayName: String = "Z.AI / ZHIPU"
    override val category: ProviderCategory = ProviderCategory.BRAIN
    
    private var model: String = ProviderCatalog.zhipu.defaultModel ?: "glm-4.6"
    
    override suspend fun testConnection(context: Context): ConnectionStatus {
        val key = SecureApiKeyStore.read(context, id, "api_key")
        if (key.isNullOrBlank()) return ConnectionStatus.NOT_CONFIGURED
        
        return try {
            val url = "https://api.z.ai/api/paas/v4/chat/completions"
            val body = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "Reply only OK")))
                put("max_tokens", 8)
            }
            val result = postRequest(url, key, body)
            if (result.contains("choices")) ConnectionStatus.CONNECTED else ConnectionStatus.ERROR
        } catch (e: Exception) {
            handleException(e)
        }
    }
    
    override suspend fun generate(systemPrompt: String, userText: String): BrainResult {
        throw UnsupportedOperationException("Use ProviderApiClient for actual calls")
    }
    
    override fun getSupportedModels(): List<String> = listOf(
        "glm-4.6",
        "glm-4.5",
        "glm-4.0",
        "glm-3-turbo"
    )
    
    override fun setModel(model: String) {
        if (getSupportedModels().contains(model)) {
            this.model = model
        }
    }
}

// ============================================
// MISTRAL AI
// ============================================
object MistralBrainProvider : BrainProvider {
    override val id: String = "mistral"
    override val displayName: String = "MISTRAL AI"
    override val category: ProviderCategory = ProviderCategory.BRAIN
    
    private var model: String = ProviderCatalog.mistral.defaultModel ?: "mistral-small-latest"
    
    override suspend fun testConnection(context: Context): ConnectionStatus {
        val key = SecureApiKeyStore.read(context, id, "api_key")
        if (key.isNullOrBlank()) return ConnectionStatus.NOT_CONFIGURED
        
        return try {
            val url = "https://api.mistral.ai/v1/chat/completions"
            val body = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "Reply only OK")))
                put("max_tokens", 8)
            }
            val result = postRequest(url, key, body)
            if (result.contains("choices")) ConnectionStatus.CONNECTED else ConnectionStatus.ERROR
        } catch (e: Exception) {
            handleException(e)
        }
    }
    
    override suspend fun generate(systemPrompt: String, userText: String): BrainResult {
        throw UnsupportedOperationException("Use ProviderApiClient for actual calls")
    }
    
    override fun getSupportedModels(): List<String> = listOf(
        "mistral-small-latest",
        "mistral-medium-latest",
        "mistral-large-latest",
        "codestral-latest",
        "mathstral-latest"
    )
    
    override fun setModel(model: String) {
        if (getSupportedModels().contains(model)) {
            this.model = model
        }
    }
}

// ============================================
// OPENROUTER
// ============================================
object OpenRouterBrainProvider : BrainProvider {
    override val id: String = "openrouter"
    override val displayName: String = "OPENROUTER"
    override val category: ProviderCategory = ProviderCategory.BRAIN
    
    private var model: String = ProviderCatalog.openrouter.defaultModel ?: "openai/gpt-oss-20b"
    
    override suspend fun testConnection(context: Context): ConnectionStatus {
        val key = SecureApiKeyStore.read(context, id, "api_key")
        if (key.isNullOrBlank()) return ConnectionStatus.NOT_CONFIGURED
        
        return try {
            val url = "https://openrouter.ai/api/v1/chat/completions"
            val body = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "Reply only OK")))
                put("max_tokens", 8)
            }
            val result = postRequest(url, key, body)
            if (result.contains("choices")) ConnectionStatus.CONNECTED else ConnectionStatus.ERROR
        } catch (e: Exception) {
            handleException(e)
        }
    }
    
    override suspend fun generate(systemPrompt: String, userText: String): BrainResult {
        throw UnsupportedOperationException("Use ProviderApiClient for actual calls")
    }
    
    override fun getSupportedModels(): List<String> = listOf(
        "openai/gpt-oss-20b",
        "anthropic/claude-3-haiku",
        "anthropic/claude-3-sonnet",
        "google/gemini-flash-1.5",
        "meta-llama/llama-3.1-70b",
        "mistralai/mistral-large"
    )
    
    override fun setModel(model: String) {
        if (getSupportedModels().contains(model)) {
            this.model = model
        }
    }
}

// ============================================
// Helper functions
// ============================================

private fun postRequest(url: String, apiKey: String, body: JSONObject, useBearer: Boolean = true): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 15000
        connection.readTimeout = 60000
        connection.setRequestProperty("Content-Type", "application/json")
        if (useBearer) {
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
        }
        
        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(body.toString())
        }
        
        val responseCode = connection.responseCode
        val inputStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val result = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
        
        if (responseCode !in 200..299) {
            throw Exception("HTTP $responseCode: $result")
        }
        
        return result
    } finally {
        connection.disconnect()
    }
}

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
