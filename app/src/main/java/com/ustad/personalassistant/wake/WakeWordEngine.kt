package com.ustad.personalassistant.wake

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

interface WakeWordEngine {
    val state: WakeWordState
    val wakePhrase: String
    fun isAvailable(context: Context): Boolean
    fun start(context: Context, onWake: (WakeWordEvent) -> Unit, onError: (WakeWordError) -> Unit = {})
    fun stop()
    fun pause()
    fun resume()
}

class WakeWordStateMachine(private val phrase: String = "Hello Assistant") {
    var state: WakeWordState = WakeWordState.IDLE
        private set
    fun start(): Boolean = if (state == WakeWordState.IDLE || state == WakeWordState.PAUSED || state == WakeWordState.ERROR) { state = WakeWordState.LISTENING_FOR_WAKE; true } else false
    fun wakeDetected(): Boolean = if (state == WakeWordState.LISTENING_FOR_WAKE) { state = WakeWordState.WAKE_DETECTED; true } else false
    fun commandListening(): Boolean = if (state == WakeWordState.WAKE_DETECTED) { state = WakeWordState.LISTENING_FOR_COMMAND; true } else false
    fun processing(): Boolean = if (state == WakeWordState.LISTENING_FOR_COMMAND) { state = WakeWordState.PROCESSING; true } else false
    fun speaking(): Boolean = if (state == WakeWordState.PROCESSING) { state = WakeWordState.SPEAKING; true } else false
    fun idle(): Boolean { state = WakeWordState.IDLE; return true }
    fun pause(): Boolean { state = WakeWordState.PAUSED; return true }
    fun error(): Boolean { state = WakeWordState.ERROR; return true }
}

class AndroidSpeechRecognizerWakeWordEngine(private val configuredPhrase: String = "Hello Assistant") : WakeWordEngine {
    private var recognizer: SpeechRecognizer? = null
    private var context: Context? = null
    private var wakeCallback: (WakeWordEvent) -> Unit = {}
    private var errorCallback: (WakeWordError) -> Unit = {}
    private val machine = WakeWordStateMachine(configuredPhrase)
    override val state: WakeWordState get() = machine.state
    override val wakePhrase: String get() = configuredPhrase

    override fun isAvailable(context: Context): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    override fun start(context: Context, onWake: (WakeWordEvent) -> Unit, onError: (WakeWordError) -> Unit) {
        if (!isAvailable(context)) { machine.error(); onError(WakeWordError(WakeWordErrorCode.UNAVAILABLE, "Wake-word detection is unavailable on this device.")); return }
        this.context = context.applicationContext
        wakeCallback = onWake
        errorCallback = onError
        if (!machine.start()) return
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this.context).apply { setRecognitionListener(listener) }
        }
        beginRecognition()
    }

    private fun beginRecognition() {
        if (machine.state != WakeWordState.LISTENING_FOR_WAKE) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        runCatching { recognizer?.startListening(intent) }.onFailure {
            machine.error()
            errorCallback(WakeWordError(WakeWordErrorCode.START_FAILED, "Wake-word listening could not start.", it.message))
        }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() { if (machine.state == WakeWordState.LISTENING_FOR_WAKE) beginRecognition() }
        override fun onError(error: Int) { if (machine.state == WakeWordState.LISTENING_FOR_WAKE) beginRecognition() }
        override fun onResults(results: Bundle?) {
            if (machine.state != WakeWordState.LISTENING_FOR_WAKE) return
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            val normalizedPhrase = normalize(configuredPhrase)
            val match = matches.firstOrNull { normalize(it).contains(normalizedPhrase) }
            if (match != null && machine.wakeDetected()) {
                recognizer?.cancel()
                wakeCallback(WakeWordEvent(configuredPhrase, null))
            } else {
                beginRecognition()
            }
        }
        override fun onPartialResults(partialResults: Bundle?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun normalize(value: String): String = value.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()
    override fun stop() { recognizer?.cancel(); machine.idle() }
    override fun pause() { recognizer?.cancel(); machine.pause() }
    override fun resume() { if (machine.state == WakeWordState.PAUSED) { machine.start(); beginRecognition() } }
}
