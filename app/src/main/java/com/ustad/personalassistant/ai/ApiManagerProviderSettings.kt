package com.ustad.personalassistant.ai

/** User-facing provider configuration catalog. Credentials are never embedded here. */
data class ApiProviderSetting(
    val providerId: String,
    val displayName: String,
    val enabled: Boolean = true,
    val priority: Int = 100,
    val model: String? = null,
    val endpoint: String? = null,
    val hasApiKey: Boolean = false
)

object DefaultAiProviderCatalog {
    /**
     * All cloud providers are configurable. Lower priority number = tried earlier.
     * The router may automatically fail over to the next eligible provider after
     * authentication, rate-limit, timeout, server, or network failures.
     */
    val providers = listOf(
        ApiProviderSetting("gemini", "Google Gemini", priority = 1, model = "gemini-2.5-flash"),
        ApiProviderSetting("openrouter", "OpenRouter", priority = 2, model = "openai/gpt-oss-20b:free"),
        ApiProviderSetting("groq", "Groq", priority = 3, model = "llama-3.3-70b-versatile"),
        ApiProviderSetting("mistral", "Mistral", priority = 4, model = "mistral-small-latest"),
        ApiProviderSetting("sambanova", "SambaNova", priority = 5, model = "Meta-Llama-3.3-70B-Instruct"),
        ApiProviderSetting("zhipu", "Zhipu AI", priority = 6, model = "glm-4.5-flash")
    )
}
