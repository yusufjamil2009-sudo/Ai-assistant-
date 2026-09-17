package com.ustad.personalassistant.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** HTTP adapter. Provider-specific mapping stays here; ApiManager remains provider-agnostic. */
class HttpAiProvider(
    private val config: ProviderConfig,
    private val apiKey: () -> String?,
    private val requestBuilder: (AiRequest, ProviderConfig, String) -> String = ::defaultRequest,
    private val responseParser: (String, ProviderConfig) -> AiResponse = ::defaultResponse
) : AiProvider {
    override val providerId get() = config.providerId
    override val displayName get() = config.displayName
    override val enabled get() = config.enabled
    override val priority get() = config.priority
    override val model get() = config.model
    override val endpoint get() = config.endpoint
    override val capabilities get() = config.capabilities
    override val timeoutMs get() = config.timeoutMs.coerceIn(1_000L, 120_000L)
    override val retryCount get() = config.retryCount.coerceIn(0, 3)
    private val isGemini get() = config.providerId.equals("gemini", true)

    override fun isAvailable(): Boolean = enabled && !config.endpoint.isNullOrBlank() && !apiKey().isNullOrBlank()
    override fun healthCheck(): ProviderHealth = if (isAvailable()) ProviderHealth(ProviderHealthStatus.HEALTHY) else ProviderHealth(ProviderHealthStatus.DISABLED)

    override fun generate(request: AiRequest): Result<AiResponse> {
        if (!isAvailable()) return Result.failure(AiException(AiErrorCode.PROVIDER_AUTH_ERROR))
        if (!request.requiredCapabilities.all(capabilities::contains)) return Result.failure(AiException(AiErrorCode.PROVIDER_UNSUPPORTED_CAPABILITY))
        val key = apiKey() ?: return Result.failure(AiException(AiErrorCode.PROVIDER_AUTH_ERROR))
        return runCatching {
            val endpoint = if (isGemini) appendQueryKey(config.endpoint!!, key) else config.endpoint!!
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; connectTimeout = timeoutMs.toInt(); readTimeout = timeoutMs.toInt(); doOutput = true
                setRequestProperty("Content-Type", "application/json")
                if (!isGemini) setRequestProperty("Authorization", "Bearer $key")
            }
            connection.outputStream.use { it.write(requestBuilder(request, config, key).toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val raw = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()
            if (code !in 200..299) throw AiException(classifyHttp(code))
            responseParser(raw, config).also { validate(it) }
        }.recoverCatching { throwable -> throw if (throwable is AiException) throwable else AiException(AiErrorCode.PROVIDER_NETWORK_ERROR) }
    }

    private fun validate(response: AiResponse) { if (response.text.isBlank() && response.actionPlan == null && response.entities.isEmpty()) throw AiException(AiErrorCode.PROVIDER_INVALID_RESPONSE) }
    private fun classifyHttp(code: Int) = when (code) { 401, 403 -> AiErrorCode.PROVIDER_AUTH_ERROR; 408, 504 -> AiErrorCode.PROVIDER_TIMEOUT; 429 -> AiErrorCode.PROVIDER_RATE_LIMITED; in 500..599 -> AiErrorCode.PROVIDER_SERVER_ERROR; else -> AiErrorCode.PROVIDER_INVALID_RESPONSE }
}

private fun appendQueryKey(endpoint: String, key: String): String = endpoint + if (endpoint.contains('?')) "&key=${java.net.URLEncoder.encode(key, "UTF-8")}" else "?key=${java.net.URLEncoder.encode(key, "UTF-8")}"

private fun defaultRequest(request: AiRequest, config: ProviderConfig, key: String): String {
    if (config.providerId.equals("gemini", true)) return JSONObject().put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", request.text))))).toString()
    return JSONObject().apply { put("model", config.model); put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", request.text))) }.toString()
}

private fun defaultResponse(raw: String, config: ProviderConfig): AiResponse {
    val root = JSONObject(raw)
    val text = if (config.providerId.equals("gemini", true)) {
        root.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text").orEmpty()
    } else {
        root.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty().ifBlank { root.optString("text") }
    }
    val usageObj = root.optJSONObject("usage")
    val usage = usageObj?.let { AiUsage(it.optLong("prompt_tokens").takeIf { n -> n > 0 }, it.optLong("completion_tokens").takeIf { n -> n > 0 }, it.optLong("total_tokens").takeIf { n -> n > 0 }) }
    return AiResponse(text = text, providerId = config.providerId, model = config.model, usage = usage)
}
