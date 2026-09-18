package com.ustad.personalassistant.voice

import android.app.Activity
import android.content.Context
import com.ustad.personalassistant.data.SettingsRepository
import com.ustad.personalassistant.wake.AndroidSpeechRecognizerWakeWordEngine
import com.ustad.personalassistant.wake.WakeWordEngine
import com.ustad.personalassistant.wake.WakeWordError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VoiceSessionManager(private val context: Context, private val voiceEngine: VoiceEngine, private val authentication: VoiceAuthenticationEngine, private val settings: SettingsRepository, private val wakeWordEngine: WakeWordEngine = AndroidSpeechRecognizerWakeWordEngine()) {
    private val _stateFlow = MutableStateFlow(VoiceSessionManagerState.IDLE)
    val stateFlow: StateFlow<VoiceSessionManagerState> = _stateFlow.asStateFlow()
    @Volatile var state: VoiceSessionManagerState = VoiceSessionManagerState.IDLE
        private set(value) {
            field = value
            _stateFlow.value = value
        }
    @Volatile var sessionType: VoiceSessionType = VoiceSessionType.NORMAL_ASSISTANT_SESSION
        private set
    private var commandTimeoutMs = 8_000L
    private var timeoutThread: Thread? = null

    fun setSessionType(type: VoiceSessionType) { sessionType = type; voiceEngine.setVoiceAuthenticated(false); voiceEngine.setCallConversationMode(type == VoiceSessionType.CALL_CONVERSATION_SESSION); if (type == VoiceSessionType.CALL_CONVERSATION_SESSION) stopBackgroundWakeListening() }
    fun startBackgroundWakeListening(serviceContext: Context, onError: (WakeWordError) -> Unit = {}) {
        if (sessionType != VoiceSessionType.NORMAL_ASSISTANT_SESSION) return
        if (!runBlockingSettings { settings.wakeWordEnabled }) return
        wakeWordEngine.start(serviceContext, { onWakeDetected() }, { error -> state = VoiceSessionManagerState.ERROR; onError(error) })
    }
    fun stopBackgroundWakeListening() { timeoutThread?.interrupt(); timeoutThread = null; wakeWordEngine.stop(); state = VoiceSessionManagerState.IDLE; voiceEngine.stopSpeaking(); voiceEngine.stopListening() }
    private fun onWakeDetected() {
        if (sessionType != VoiceSessionType.NORMAL_ASSISTANT_SESSION) return
        if (state != VoiceSessionManagerState.IDLE && state != VoiceSessionManagerState.PAUSED) return
        state = VoiceSessionManagerState.WAKE_DETECTED; state = VoiceSessionManagerState.ACKNOWLEDGING
        voiceEngine.speak("हाँ, बोलिए।") { result -> if (result.isFailure) { state = VoiceSessionManagerState.ERROR; return@speak }; authenticateThenListen() }
    }
    private fun authenticateThenListen() {
        if (sessionType != VoiceSessionType.NORMAL_ASSISTANT_SESSION) return
        if (!runBlockingSettings { settings.voiceAuthenticationEnabled }) {
            voiceEngine.setVoiceAuthenticated(false);
            state = VoiceSessionManagerState.COMMAND_LISTENING
            startCommandTimeout()
            voiceEngine.startListeningFromBackground { event ->
                if (event.type == SttEventType.FINAL) {
                    state = VoiceSessionManagerState.RESPONDING
                    cancelTimeout()
                }
                if (event.type == SttEventType.ERROR) state = VoiceSessionManagerState.ERROR
            }
            return
        }
        state = VoiceSessionManagerState.AUTHENTICATING
        val attempt = authentication.authenticate().getOrElse { state = VoiceSessionManagerState.ERROR; return }
        if (!VoiceAuthenticationPolicy.mayEnterControlPipeline(sessionType, attempt.result)) {
            state = if (attempt.result == VoiceAuthenticationResult.LOCKED_OUT) VoiceSessionManagerState.PAUSED else VoiceSessionManagerState.IDLE
            if (attempt.result == VoiceAuthenticationResult.UNAUTHORIZED || attempt.result == VoiceAuthenticationResult.LOCKED_OUT) voiceEngine.speak(if (attempt.result == VoiceAuthenticationResult.LOCKED_OUT) "Voice authentication is temporarily locked." else "Voice authentication failed.")
            return
        }
        voiceEngine.setVoiceAuthenticated(true)
        state = VoiceSessionManagerState.COMMAND_LISTENING; startCommandTimeout()
        voiceEngine.startListeningFromBackground { event -> if (event.type == SttEventType.FINAL) { state = VoiceSessionManagerState.RESPONDING; cancelTimeout() }; if (event.type == SttEventType.ERROR) state = VoiceSessionManagerState.ERROR }
    }
    fun startManualCommandListening(activity: Activity, onEvent: (SttEvent) -> Unit = {}) {
        if (sessionType != VoiceSessionType.NORMAL_ASSISTANT_SESSION) return
        voiceEngine.setVoiceAuthenticated(false)
        state = VoiceSessionManagerState.COMMAND_LISTENING
        voiceEngine.startListening(activity) { event -> onEvent(event); if (event.type == SttEventType.FINAL) { state = VoiceSessionManagerState.RESPONDING; cancelTimeout() }; if (event.type == SttEventType.ERROR) state = VoiceSessionManagerState.ERROR }
    }
    private fun startCommandTimeout() { cancelTimeout(); timeoutThread = Thread { try { Thread.sleep(commandTimeoutMs) } catch (_: InterruptedException) { return@Thread }; if (state == VoiceSessionManagerState.COMMAND_LISTENING) { voiceEngine.stopListening(); voiceEngine.speak("जी?"); state = VoiceSessionManagerState.IDLE } }.also { it.isDaemon = true; it.start() } }
    fun setCommandTimeoutMs(value: Long) { commandTimeoutMs = value.coerceIn(3_000L, 20_000L) }
    fun cancel() { cancelTimeout(); voiceEngine.setVoiceAuthenticated(false); voiceEngine.cancelListening(); voiceEngine.stopSpeaking(); state = VoiceSessionManagerState.IDLE }
    fun pause() { cancelTimeout(); wakeWordEngine.pause(); voiceEngine.stopListening(); state = VoiceSessionManagerState.PAUSED }
    fun resume() { if (state == VoiceSessionManagerState.PAUSED) { state = VoiceSessionManagerState.IDLE; wakeWordEngine.resume() } }
    private fun cancelTimeout() { timeoutThread?.interrupt(); timeoutThread = null }
    private fun <T> runBlockingSettings(block: suspend () -> kotlinx.coroutines.flow.Flow<T>): T = kotlinx.coroutines.runBlocking { block().first() }
}

enum class VoiceSessionType { NORMAL_ASSISTANT_SESSION, CALL_CONVERSATION_SESSION }
enum class VoiceSessionManagerState { IDLE, WAKE_DETECTED, ACKNOWLEDGING, AUTHENTICATING, COMMAND_LISTENING, PROCESSING, RESPONDING, PAUSED, ERROR }
