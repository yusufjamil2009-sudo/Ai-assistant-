package com.ustad.personalassistant.voice

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

class AndroidLocalSpeechProvider(private val context: Context, private val networkAvailable: () -> Boolean = { true }) : SpeechToTextProvider {
    override val config = SpeechToTextConfig("android_local", "Android / Local", enabled = true, priority = 99, offlineSupport = true, streamingSupport = true)
    private var recognizer: SpeechRecognizer? = null
    private var activeListener: ((SttEvent) -> Unit)? = null
    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)
    override fun healthCheck(): VoiceProviderHealth = if (isAvailable()) VoiceProviderHealth(VoiceProviderStatus.HEALTHY) else VoiceProviderHealth(VoiceProviderStatus.UNAVAILABLE)
    override fun start(language: VoiceLanguage, listener: (SttEvent) -> Unit): Result<Unit> {
        if (!isAvailable()) return Result.failure(VoiceException(VoiceErrorCode.MICROPHONE_UNAVAILABLE, "Speech recognition is not available on this device."))
        if (recognizer != null) stop()
        activeListener = listener
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { sr ->
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit; override fun onBeginningOfSpeech() = Unit; override fun onRmsChanged(rmsdB: Float) = Unit; override fun onBufferReceived(buffer: ByteArray?) = Unit; override fun onEndOfSpeech() = Unit
                override fun onPartialResults(results: Bundle?) = emit(results, false)
                override fun onResults(results: Bundle?) { emit(results, true); release() }
                override fun onError(error: Int) { activeListener?.invoke(SttEvent(SttEventType.ERROR, error = VoiceError(mapError(error), "Speech recognition stopped: ${errorMessage(error)}"))); release() }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            val locale = if (language == VoiceLanguage.ENGLISH) Locale.ENGLISH else Locale("hi", "IN")
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag()); putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.toLanguageTag()); putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true); putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3); putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, !networkAvailable()) }
            sr.startListening(intent)
        }
        return Result.success(Unit)
    }
    private fun emit(bundle: Bundle?, final: Boolean) { val text = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty(); if (text.isNotBlank()) activeListener?.invoke(SttEvent(if (final) SttEventType.FINAL else SttEventType.PARTIAL, SttResult(text, VoiceLanguageDetector().detect(text), null, config.providerId, final))) }
    override fun streamAudio(audio: ByteArray): Result<Unit> = Result.success(Unit)
    override fun stop(): Result<SttResult?> { recognizer?.stopListening(); release(); return Result.success(null) }
    override fun transcribe(audio: ByteArray, language: VoiceLanguage): Result<SttResult> = Result.failure(VoiceException(VoiceErrorCode.STT_PROVIDER_UNAVAILABLE, "Android SpeechRecognizer manages its own audio session."))
    private fun release() { recognizer?.cancel(); recognizer?.destroy(); recognizer = null; activeListener = null }
    private fun mapError(error: Int) = when (error) { SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> VoiceErrorCode.STT_NETWORK_ERROR; SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> VoiceErrorCode.STT_PROVIDER_UNAVAILABLE; SpeechRecognizer.ERROR_SPEECH_TIMEOUT, SpeechRecognizer.ERROR_NO_MATCH -> VoiceErrorCode.STT_TIMEOUT; else -> VoiceErrorCode.STT_INVALID_RESPONSE }
    private fun errorMessage(error: Int) = when (error) { SpeechRecognizer.ERROR_AUDIO -> "Audio capture error"; SpeechRecognizer.ERROR_NETWORK -> "Network error"; SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"; SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"; SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"; else -> "Recognition error" }
}

class AndroidTextToSpeechProvider(private val context: Context) : TextToSpeechProvider, ConfigurableTtsVoice {
    override val config = TextToSpeechConfig("android_tts", "Android Native TTS", enabled = true, priority = 99)
    private var tts: TextToSpeech? = null; private var ready = false; private var audioManager: AudioManager? = null; private var focusRequest: AudioFocusRequest? = null; private var completion: ((Result<Unit>) -> Unit)? = null
    init { initialize() }
    private fun initialize() { tts = TextToSpeech(context) { status -> ready = status == TextToSpeech.SUCCESS; if (ready) tts?.setOnUtteranceProgressListener(progressListener) }; audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    override fun isAvailable(): Boolean = ready && tts?.voices?.isNotEmpty() == true
    override fun healthCheck(): VoiceProviderHealth = if (isAvailable()) VoiceProviderHealth(VoiceProviderStatus.HEALTHY) else VoiceProviderHealth(VoiceProviderStatus.UNAVAILABLE)
    override fun speak(text: String, language: VoiceLanguage, onComplete: (Result<Unit>) -> Unit) { if (!isAvailable()) { onComplete(Result.failure(VoiceException(VoiceErrorCode.NO_LOCAL_VOICE, "No compatible Android voice is available."))); return }; val locale = if (language == VoiceLanguage.ENGLISH) Locale.ENGLISH else Locale("hi", "IN"); val result = tts?.setLanguage(locale) ?: TextToSpeech.ERROR; if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) tts?.setLanguage(Locale.ENGLISH); requestFocus(); completion = onComplete; val id = UUID.randomUUID().toString(); val status = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), id) ?: TextToSpeech.ERROR; if (status == TextToSpeech.ERROR) { releaseFocus(); completion = null; onComplete(Result.failure(VoiceException(VoiceErrorCode.TTS_INVALID_RESPONSE, "Android text-to-speech could not start."))) } }
    override fun setSpeechRate(rate: Float) { tts?.setSpeechRate(rate.coerceIn(0.5f, 2f)) }
    override fun setSpeechPitch(pitch: Float) { tts?.setPitch(pitch.coerceIn(0.5f, 2f)) }
    override fun selectVoice(name: String?): Boolean { val voice = name?.let { n -> tts?.voices?.firstOrNull { it.name == n } } ?: return false; tts?.voice = voice; return true }
    override fun availableVoices(): List<String> = tts?.voices?.mapNotNull { it.name }?.sorted().orEmpty()
    fun availableLocales(): List<Locale> = tts?.availableLanguages?.toList().orEmpty()
    private val progressListener = object : UtteranceProgressListener() { override fun onStart(utteranceId: String?) = Unit; override fun onDone(utteranceId: String?) { releaseFocus(); completion?.invoke(Result.success(Unit)); completion = null }; override fun onError(utteranceId: String?) { releaseFocus(); completion?.invoke(Result.failure(VoiceException(VoiceErrorCode.TTS_INVALID_RESPONSE, "Android text-to-speech failed."))); completion = null } }
    override fun stop(): Result<Unit> { tts?.stop(); completion = null; releaseFocus(); return Result.success(Unit) }
    fun shutdown() { stop(); tts?.shutdown(); tts = null; ready = false }
    private fun requestFocus() { val am = audioManager ?: return; if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).setOnAudioFocusChangeListener { change -> if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) stop() }.build(); am.requestAudioFocus(focusRequest!!) } else requestLegacyFocus(am) }
    @Suppress("DEPRECATION") private fun requestLegacyFocus(am: AudioManager) { am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) }
    private fun releaseFocus() { val am = audioManager ?: return; if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) focusRequest?.let(am::abandonAudioFocusRequest) else releaseLegacyFocus(am) }
    @Suppress("DEPRECATION") private fun releaseLegacyFocus(am: AudioManager) { am.abandonAudioFocus(null) }
}
