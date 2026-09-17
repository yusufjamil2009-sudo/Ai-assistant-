package com.ustad.personalassistant.voice

import kotlin.math.min

class VoiceLanguageDetector {
    fun detect(text: String): VoiceLanguage {
        val value = text.trim()
        if (value.isBlank()) return VoiceLanguage.UNKNOWN
        val hasDevanagari = value.any { it in '\u0900'..'\u097F' }
        val englishWords = Regex("\\b(the|is|are|open|send|read|tell|please|email|message|tomorrow|morning)\\b", RegexOption.IGNORE_CASE).findAll(value).count()
        val hindiWords = Regex("\\b(ko|khol(o|na)?|meri|mere|batao|bhejo|kal|subah|hai|haan|mujhe|par)\\b", RegexOption.IGNORE_CASE).findAll(value).count()
        return when { hasDevanagari && englishWords > 0 -> VoiceLanguage.MIXED; hasDevanagari -> VoiceLanguage.HINDI; hindiWords > 0 && englishWords > 0 -> VoiceLanguage.HINGLISH; hindiWords > 0 -> VoiceLanguage.HINGLISH; englishWords > 0 -> VoiceLanguage.ENGLISH; else -> VoiceLanguage.UNKNOWN }
    }
}
class SpeechToTextManager(private val providers: () -> List<SpeechToTextProvider>, private val networkAvailable: () -> Boolean, private val cooldownMs: Long = 15_000L) {
    private val failures = mutableMapOf<String, Int>(); private val cooldownUntil = mutableMapOf<String, Long>(); private val lastSuccess = mutableMapOf<String, Long>(); @Volatile private var preferredProviderId = "AUTO"
    fun setPreferredProvider(providerId: String) { preferredProviderId = providerId.ifBlank { "AUTO" } }
    fun start(language: VoiceLanguage, listener: (SttEvent) -> Unit): Result<String> { val selected = candidates(language).firstOrNull() ?: return Result.failure(VoiceException(VoiceErrorCode.STT_PROVIDER_UNAVAILABLE, "No speech recognition provider is available.")); return selected.start(language) { event -> if (event.type == SttEventType.FINAL) { failures[selected.config.providerId] = 0; lastSuccess[selected.config.providerId] = System.currentTimeMillis() }; if (event.type == SttEventType.ERROR) recordFailure(selected.config.providerId); listener(event) }.map { selected.config.providerId } }
    fun transcribe(audio: ByteArray, language: VoiceLanguage): Result<SttResult> { if (audio.isEmpty()) return Result.failure(VoiceException(VoiceErrorCode.STT_INVALID_RESPONSE, "No audio was captured.")); var last: Throwable? = null; for (provider in candidates(language)) { repeat(provider.config.retryCount.coerceIn(0, 3) + 1) { attempt -> if (attempt > 0) Thread.sleep(min(750L * (1L shl (attempt - 1)), 3_000L)); val result = provider.transcribe(audio, language); if (result.isSuccess) { failures[provider.config.providerId] = 0; lastSuccess[provider.config.providerId] = System.currentTimeMillis(); return result }; last = result.exceptionOrNull() }; recordFailure(provider.config.providerId) }; return Result.failure(last as? VoiceException ?: VoiceException(VoiceErrorCode.STT_ALL_PROVIDERS_FAILED, "All speech recognition providers failed.")) }
    fun streamAudio(audio: ByteArray): Result<Unit> { if (audio.isEmpty()) return Result.success(Unit); val provider = candidates(VoiceLanguage.MIXED).firstOrNull { it.config.streamingSupport } ?: return Result.failure(VoiceException(VoiceErrorCode.STT_PROVIDER_UNAVAILABLE, "Streaming speech recognition is unavailable.")); return provider.streamAudio(audio).onFailure { recordFailure(provider.config.providerId) } }
    fun stop(providerId: String): Result<SttResult?> = providers().firstOrNull { it.config.providerId == providerId }?.stop() ?: Result.failure(VoiceException(VoiceErrorCode.STT_PROVIDER_UNAVAILABLE, "Speech provider is unavailable."))
    fun health(): Map<String, VoiceProviderHealth> = providers().associate { p -> p.config.providerId to p.healthCheck().copy(consecutiveFailures = failures[p.config.providerId] ?: 0, cooldownUntil = cooldownUntil[p.config.providerId], lastSuccess = lastSuccess[p.config.providerId]) }
    private fun candidates(language: VoiceLanguage): List<SpeechToTextProvider> = providers().filter { it.config.enabled && it.isAvailable() && language in it.config.supportedLanguages && notCooling(it.config.providerId) }.filter { networkAvailable() || it.config.offlineSupport }.sortedWith(compareBy<SpeechToTextProvider> { if (preferredProviderId != "AUTO" && it.config.providerId == preferredProviderId) 0 else 1 }.thenBy { it.config.priority })
    private fun notCooling(id: String) = (cooldownUntil[id] ?: 0L) <= System.currentTimeMillis()
    private fun recordFailure(id: String) { val count = (failures[id] ?: 0) + 1; failures[id] = count; cooldownUntil[id] = System.currentTimeMillis() + cooldownMs * min(count, 4) }
}
class TextToSpeechManager(private val providers: () -> List<TextToSpeechProvider>, private val networkAvailable: () -> Boolean) {
    @Volatile private var preferredProviderId = "AUTO"
    fun setPreferredProvider(providerId: String) { preferredProviderId = providerId.ifBlank { "AUTO" } }
    fun firstConfigurableProvider(): ConfigurableTtsVoice? = providers().firstOrNull { it is ConfigurableTtsVoice } as? ConfigurableTtsVoice
    fun speak(text: String, language: VoiceLanguage, onComplete: (Result<Unit>) -> Unit) { if (text.isBlank()) { onComplete(Result.success(Unit)); return }; val candidates = providers().filter { it.config.enabled && it.isAvailable() && language in it.config.supportedLanguages && (networkAvailable() || it.config.providerId == "android_tts") }.sortedWith(compareBy<TextToSpeechProvider> { if (preferredProviderId != "AUTO" && it.config.providerId == preferredProviderId) 0 else 1 }.thenBy { it.config.priority }); if (candidates.isEmpty()) { onComplete(Result.failure(VoiceException(VoiceErrorCode.TTS_ALL_PROVIDERS_FAILED, "No text-to-speech provider is available."))); return }; speakFrom(candidates, 0, text, language, onComplete) }
    private fun speakFrom(list: List<TextToSpeechProvider>, index: Int, text: String, language: VoiceLanguage, done: (Result<Unit>) -> Unit) { if (index >= list.size) { done(Result.failure(VoiceException(VoiceErrorCode.TTS_ALL_PROVIDERS_FAILED, "All text-to-speech providers failed."))); return }; list[index].speak(text, language) { result -> if (result.isSuccess) done(result) else speakFrom(list, index + 1, text, language, done) } }
    fun stop(): Result<Unit> = providers().firstOrNull { it.isAvailable() }?.stop() ?: Result.success(Unit)
    fun health(): Map<String, VoiceProviderHealth> = providers().associate { it.config.providerId to it.healthCheck() }
}
class VoiceException(val code: VoiceErrorCode, override val message: String, val causeText: String? = null) : Exception(message)
