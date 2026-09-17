package com.ustad.personalassistant.voice

import android.content.Context
import com.ustad.personalassistant.security.SecureConfigStore

class VoiceProviderRegistry(context: Context, private val networkAvailable: () -> Boolean) {
    private val secure = SecureConfigStore(context, "voice_provider_credentials")
    private val sttConfigs = DefaultVoiceProviderSlots.stt().toMutableList()
    private val ttsConfigs = DefaultVoiceProviderSlots.tts().toMutableList()

    fun sttProviders(context: Context): List<SpeechToTextProvider> = sttConfigs.map { config ->
        when (config.providerId) {
            "android_local" -> AndroidLocalSpeechProvider(context, networkAvailable)
            else -> ConfiguredHttpSpeechProvider(config) { secure.get("${config.providerId}_api_key") }
        }
    }

    fun ttsProviders(context: Context): List<TextToSpeechProvider> = ttsConfigs.map { config ->
        when (config.providerId) {
            "android_tts" -> AndroidTextToSpeechProvider(context)
            else -> ConfiguredHttpTextToSpeechProvider(config) { secure.get("${config.providerId}_api_key") }
        }
    }

    fun sttConfigs(): List<SpeechToTextConfig> = sttConfigs.toList()
    fun ttsConfigs(): List<TextToSpeechConfig> = ttsConfigs.toList()
    fun saveStt(config: SpeechToTextConfig, apiKey: String? = null) { replaceStt(config); apiKey?.takeIf { it.isNotBlank() }?.let { secure.put("${config.providerId}_api_key", it) } }
    fun saveTts(config: TextToSpeechConfig, apiKey: String? = null) { replaceTts(config); apiKey?.takeIf { it.isNotBlank() }?.let { secure.put("${config.providerId}_api_key", it) } }
    fun removeKey(providerId: String) { secure.remove("${providerId}_api_key") }
    fun hasKey(providerId: String): Boolean = !secure.get("${providerId}_api_key").isNullOrBlank()
    fun maskedKey(providerId: String): String = secure.get("${providerId}_api_key")?.let { if (it.length <= 8) "••••••••" else "${it.take(4)}••••${it.takeLast(4)}" } ?: "Not configured"

    private fun replaceStt(config: SpeechToTextConfig) { val i = sttConfigs.indexOfFirst { it.providerId == config.providerId }; if (i >= 0) sttConfigs[i] = config else sttConfigs += config }
    private fun replaceTts(config: TextToSpeechConfig) { val i = ttsConfigs.indexOfFirst { it.providerId == config.providerId }; if (i >= 0) ttsConfigs[i] = config else ttsConfigs += config }
}

class ConfiguredHttpSpeechProvider(
    override val config: SpeechToTextConfig,
    private val apiKey: () -> String?
) : SpeechToTextProvider {
    override fun isAvailable(): Boolean = config.enabled && !config.endpoint.isNullOrBlank() && !apiKey().isNullOrBlank()
    override fun healthCheck(): VoiceProviderHealth = if (isAvailable()) VoiceProviderHealth(VoiceProviderStatus.HEALTHY) else VoiceProviderHealth(VoiceProviderStatus.NOT_CONFIGURED)
    override fun start(language: VoiceLanguage, listener: (SttEvent) -> Unit): Result<Unit> = Result.failure(VoiceException(VoiceErrorCode.STT_PROVIDER_UNAVAILABLE, "${config.displayName} streaming requires its provider-specific audio session adapter."))
    override fun streamAudio(audio: ByteArray): Result<Unit> = Result.failure(VoiceException(VoiceErrorCode.STT_PROVIDER_UNAVAILABLE, "Streaming adapter is not configured."))
    override fun stop(): Result<SttResult?> = Result.success(null)
    override fun transcribe(audio: ByteArray, language: VoiceLanguage): Result<SttResult> = Result.failure(VoiceException(VoiceErrorCode.STT_PROVIDER_UNAVAILABLE, "${config.displayName} is configured as a future cloud STT adapter."))
}

class ConfiguredHttpTextToSpeechProvider(
    override val config: TextToSpeechConfig,
    private val apiKey: () -> String?
) : TextToSpeechProvider {
    override fun isAvailable(): Boolean = config.enabled && !config.endpoint.isNullOrBlank() && !apiKey().isNullOrBlank()
    override fun healthCheck(): VoiceProviderHealth = if (isAvailable()) VoiceProviderHealth(VoiceProviderStatus.HEALTHY) else VoiceProviderHealth(VoiceProviderStatus.NOT_CONFIGURED)
    override fun speak(text: String, language: VoiceLanguage, onComplete: (Result<Unit>) -> Unit) = onComplete(Result.failure(VoiceException(VoiceErrorCode.TTS_PROVIDER_UNAVAILABLE, "${config.displayName} cloud audio adapter is not configured.")))
    override fun stop(): Result<Unit> = Result.success(Unit)
}
