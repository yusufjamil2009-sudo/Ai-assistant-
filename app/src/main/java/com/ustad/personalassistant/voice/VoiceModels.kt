package com.ustad.personalassistant.voice

import com.ustad.personalassistant.permissions.Capability

enum class VoiceSessionState { IDLE, REQUESTING_PERMISSION, STARTING, LISTENING, PROCESSING, SPEAKING, STOPPING, ERROR }
enum class SpeakingState { IDLE, SPEAKING, STOPPING, ERROR }
enum class VoiceLanguage { HINDI, HINGLISH, ENGLISH, MIXED, UNKNOWN }
enum class SttEventType { PARTIAL, FINAL, ERROR }
enum class VoiceErrorCode { MIC_PERMISSION_REQUIRED, MIC_PERMISSION_DENIED, MICROPHONE_UNAVAILABLE, STT_PROVIDER_UNAVAILABLE, STT_TIMEOUT, STT_NETWORK_ERROR, STT_RATE_LIMITED, STT_AUTH_ERROR, STT_INVALID_RESPONSE, STT_ALL_PROVIDERS_FAILED, TTS_PROVIDER_UNAVAILABLE, TTS_TIMEOUT, TTS_NETWORK_ERROR, TTS_AUTH_ERROR, TTS_INVALID_RESPONSE, TTS_ALL_PROVIDERS_FAILED, NO_LOCAL_VOICE, AUDIO_FOCUS_INTERRUPTED, VOICE_SESSION_ERROR }
data class SttResult(val text: String, val language: VoiceLanguage = VoiceLanguage.UNKNOWN, val confidence: Double? = null, val providerId: String? = null, val isFinal: Boolean = true)
data class SttEvent(val type: SttEventType, val result: SttResult? = null, val error: VoiceError? = null)
data class VoiceError(val code: VoiceErrorCode, val userMessage: String, val causeMessage: String? = null)
data class VoiceProviderHealth(val status: VoiceProviderStatus, val consecutiveFailures: Int = 0, val cooldownUntil: Long? = null, val lastSuccess: Long? = null, val lastFailure: Long? = null)
enum class VoiceProviderStatus { HEALTHY, DEGRADED, COOLDOWN, DISABLED, NOT_CONFIGURED, UNAVAILABLE }
data class SpeechToTextConfig(val providerId: String, val displayName: String, val enabled: Boolean = false, val priority: Int = 100, val supportedLanguages: Set<VoiceLanguage> = setOf(VoiceLanguage.HINDI, VoiceLanguage.HINGLISH, VoiceLanguage.ENGLISH, VoiceLanguage.MIXED), val streamingSupport: Boolean = false, val offlineSupport: Boolean = false, val endpoint: String? = null, val model: String? = null, val timeoutMs: Long = 30_000L, val retryCount: Int = 1)
data class TextToSpeechConfig(val providerId: String, val displayName: String, val enabled: Boolean = false, val priority: Int = 100, val supportedLanguages: Set<VoiceLanguage> = setOf(VoiceLanguage.HINDI, VoiceLanguage.HINGLISH, VoiceLanguage.ENGLISH, VoiceLanguage.MIXED), val streamingSupport: Boolean = false, val endpoint: String? = null, val model: String? = null, val voiceId: String? = null, val timeoutMs: Long = 30_000L)
interface SpeechToTextProvider { val config: SpeechToTextConfig; fun start(language: VoiceLanguage, listener: (SttEvent) -> Unit): Result<Unit>; fun streamAudio(audio: ByteArray): Result<Unit>; fun stop(): Result<SttResult?>; fun transcribe(audio: ByteArray, language: VoiceLanguage): Result<SttResult>; fun isAvailable(): Boolean; fun healthCheck(): VoiceProviderHealth }
interface TextToSpeechProvider { val config: TextToSpeechConfig; fun speak(text: String, language: VoiceLanguage, onComplete: (Result<Unit>) -> Unit); fun stop(): Result<Unit>; fun isAvailable(): Boolean; fun healthCheck(): VoiceProviderHealth }
interface ConfigurableTtsVoice { fun setSpeechRate(rate: Float); fun setSpeechPitch(pitch: Float); fun selectVoice(name: String?): Boolean; fun availableVoices(): List<String> }
interface VoiceAuthenticationManager { enum class Result { AUTHORIZED, UNAUTHORIZED, NOT_ENROLLED, UNAVAILABLE }; fun status(): Result }
class UnavailableVoiceAuthenticationManager : VoiceAuthenticationManager { override fun status() = VoiceAuthenticationManager.Result.UNAVAILABLE }
interface VoiceInputNormalizer { fun normalize(text: String): String }
class DefaultVoiceInputNormalizer : VoiceInputNormalizer { override fun normalize(text: String): String = text.replace(Regex("\\s+"), " ").replace(Regex("[.!?]{2,}"), ".").trim() }
object VoiceCapabilities { val microphone = Capability.MICROPHONE }
object DefaultVoiceProviderSlots {
    fun stt(): List<SpeechToTextConfig> = listOf(SpeechToTextConfig("deepgram", "Deepgram", priority = 1, streamingSupport = true), SpeechToTextConfig("assemblyai", "AssemblyAI", priority = 2, streamingSupport = true), SpeechToTextConfig("android_local", "Android / Local", enabled = true, priority = 99, offlineSupport = true, streamingSupport = true))
    fun tts(): List<TextToSpeechConfig> = listOf(TextToSpeechConfig("elevenlabs", "ElevenLabs", priority = 1), TextToSpeechConfig("android_tts", "Android Native TTS", enabled = true, priority = 99))
}
