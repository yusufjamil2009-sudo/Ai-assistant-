package com.ustad.personalassistant.voice

import android.content.Context
import com.ustad.personalassistant.security.SecureConfigStore

class VoiceProviderRegistry(context: Context, private val networkAvailable: () -> Boolean) {
    private val appContext = context.applicationContext
    private val secure = SecureConfigStore(appContext, "voice_provider_credentials")
    private val sttConfigs = DefaultVoiceProviderSlots.stt().toMutableList()
    private val ttsConfigs = DefaultVoiceProviderSlots.tts().toMutableList()
    private val localStt = AndroidLocalSpeechProvider(appContext, networkAvailable)
    private val localTts = AndroidTextToSpeechProvider(appContext)

    fun sttProviders(context: Context): List<SpeechToTextProvider> = sttConfigs.map { config -> if (config.providerId == "android_local") localStt else ConfiguredHttpSpeechProvider(config) { secure.get("${config.providerId}_api_key") } }
    fun ttsProviders(context: Context): List<TextToSpeechProvider> = ttsConfigs.map { config -> if (config.providerId == "android_tts") localTts else ConfiguredHttpTextToSpeechProvider(config) { secure.get("${config.providerId}_api_key") } }
    fun sttConfigs(): List<SpeechToTextConfig> = sttConfigs.toList()
    fun ttsConfigs(): List<TextToSpeechConfig> = ttsConfigs.toList()
    fun saveStt(config: SpeechToTextConfig, apiKey: String? = null) { replaceStt(config); apiKey?.takeIf(String::isNotBlank)?.let { secure.put("${config.providerId}_api_key", it) } }
    fun saveTts(config: TextToSpeechConfig, apiKey: String? = null) { replaceTts(config); apiKey?.takeIf(String::isNotBlank)?.let { secure.put("${config.providerId}_api_key", it) } }
    fun removeKey(providerId: String) { secure.remove("${providerId}_api_key") }
    fun hasKey(providerId: String): Boolean = !secure.get("${providerId}_api_key").isNullOrBlank()
    fun maskedKey(providerId: String): String = secure.get("${providerId}_api_key")?.let { if (it.length <= 8) "••••••••" else "${it.take(4)}••••${it.takeLast(4)}" } ?: "Not configured"
    fun shutdown() { localStt.stop(); localTts.shutdown() }
    private fun replaceStt(config: SpeechToTextConfig) { val i = sttConfigs.indexOfFirst { it.providerId == config.providerId }; if (i >= 0) sttConfigs[i] = config else sttConfigs += config }
    private fun replaceTts(config: TextToSpeechConfig) { val i = ttsConfigs.indexOfFirst { it.providerId == config.providerId }; if (i >= 0) ttsConfigs[i] = config else ttsConfigs += config }
}

class ConfiguredHttpSpeechProvider(override val config: SpeechToTextConfig, private val apiKey: () -> String?) : SpeechToTextProvider {
    override fun isAvailable(): Boolean = config.enabled && !config.endpoint.isNullOrBlank() && !apiKey().isNullOrBlank()
    override fun healthCheck(): VoiceProviderHealth = if (isAvailable()) VoiceProviderHealth(VoiceProviderStatus.HEALTHY) else VoiceProviderHealth(VoiceProviderStatus.NOT_CONFIGURED)
    override fun start(language: VoiceLanguage, listener: (SttEvent) -> Unit): Result<Unit> = Result.failure(VoiceException(VoiceErrorCode.STT_PROVIDER_UNAVAILABLE, "${config.displayName} streaming needs a provider-specific streaming adapter."))
    override fun streamAudio(audio: ByteArray): Result<Unit> = Result.failure(VoiceException(VoiceErrorCode.STT_PROVIDER_UNAVAILABLE, "Streaming adapter is not configured."))
    override fun stop(): Result<SttResult?> = Result.success(null)
    override fun transcribe(audio: ByteArray, language: VoiceLanguage): Result<SttResult> = Result.failure(VoiceException(VoiceErrorCode.STT_PROVIDER_UNAVAILABLE, "${config.displayName} cloud STT adapter is not configured."))
}

class ConfiguredHttpTextToSpeechProvider(override val config: TextToSpeechConfig, private val apiKey: () -> String?) : TextToSpeechProvider {
    override fun isAvailable(): Boolean = config.enabled && !config.endpoint.isNullOrBlank() && !apiKey().isNullOrBlank()
    override fun healthCheck(): VoiceProviderHealth = if (isAvailable()) VoiceProviderHealth(VoiceProviderStatus.HEALTHY) else VoiceProviderHealth(VoiceProviderStatus.NOT_CONFIGURED)
    override fun speak(text: String, language: VoiceLanguage, onComplete: (Result<Unit>) -> Unit) = onComplete(Result.failure(VoiceException(VoiceErrorCode.TTS_PROVIDER_UNAVAILABLE, "${config.displayName} cloud audio adapter is not configured.")))
    override fun stop(): Result<Unit> = Result.success(Unit)
}
