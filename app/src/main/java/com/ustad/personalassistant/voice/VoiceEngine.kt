package com.ustad.personalassistant.voice

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.ustad.personalassistant.ai.AiAutomationOrchestrator
import com.ustad.personalassistant.ai.AiRequest
import com.ustad.personalassistant.ai.AiResponse
import com.ustad.personalassistant.capability.CapabilityEngine
import com.ustad.personalassistant.data.SettingsRepository
import com.ustad.personalassistant.domain.CapabilityStatus
import com.ustad.personalassistant.permissions.Capability
import com.ustad.personalassistant.permissions.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface VoiceEngine { fun setVoiceAuthenticated(authenticated: Boolean); fun startListening(activity: Activity, onEvent: (SttEvent) -> Unit = {}); fun resumeListeningAfterPermission(activity: Activity); fun startListeningFromBackground(onEvent: (SttEvent) -> Unit = {}); fun stopListening(); fun cancelListening(); fun speak(text: String, onComplete: (Result<Unit>) -> Unit = {}); fun stopSpeaking(); fun getListeningState(): VoiceSessionState; fun getSpeakingState(): SpeakingState; fun setCallConversationMode(enabled: Boolean); fun close() }
enum class VoiceOperationMode { USER_COMMAND, CALL_CONVERSATION_MODE }

class AndroidVoiceEngine(private val permissionManager: PermissionManager, private val capabilityEngine: CapabilityEngine, private val sttManager: SpeechToTextManager, private val ttsManager: TextToSpeechManager, private val aiOrchestrator: AiAutomationOrchestrator, private val settings: SettingsRepository, private val normalizer: VoiceInputNormalizer = DefaultVoiceInputNormalizer(), private val actionGate: VoiceActionGate = VoiceActionGate()) : VoiceEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Volatile private var voiceAuthenticated = false
    @Volatile private var sessionState = VoiceSessionState.IDLE; @Volatile private var speakingState = SpeakingState.IDLE
    private var activeSttProvider: String? = null; private var callback: (SttEvent) -> Unit = {}; private var mode = VoiceOperationMode.USER_COMMAND; private var operationJob: Job? = null
    override fun startListening(activity: Activity, onEvent: (SttEvent) -> Unit) { if (sessionState == VoiceSessionState.LISTENING || sessionState == VoiceSessionState.STARTING || sessionState == VoiceSessionState.PROCESSING) return; if (speakingState == SpeakingState.SPEAKING) stopSpeaking(); callback = onEvent; sessionState = VoiceSessionState.REQUESTING_PERMISSION; if (permissionManager.verifyPermission(Capability.MICROPHONE) != CapabilityStatus.ON) { if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) permissionManager.requestPermission(activity, Capability.MICROPHONE) else { sessionState = VoiceSessionState.ERROR; callback(SttEvent(SttEventType.ERROR, error = VoiceError(VoiceErrorCode.MIC_PERMISSION_DENIED, "Microphone permission is denied."))) }; return }; if (!capabilityEngine.isAvailable(Capability.MICROPHONE)) { sessionState = VoiceSessionState.ERROR; callback(SttEvent(SttEventType.ERROR, error = VoiceError(VoiceErrorCode.MIC_PERMISSION_REQUIRED, "Microphone permission is required."))); return }; beginListening() }
    override fun resumeListeningAfterPermission(activity: Activity) {
        if (sessionState != VoiceSessionState.REQUESTING_PERMISSION) return
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
            permissionManager.verifyPermission(Capability.MICROPHONE) == CapabilityStatus.ON &&
            capabilityEngine.isAvailable(Capability.MICROPHONE)
        ) {
            beginListening()
        } else {
            sessionState = VoiceSessionState.ERROR
            callback(
                SttEvent(
                    SttEventType.ERROR,
                    error = VoiceError(
                        VoiceErrorCode.MIC_PERMISSION_DENIED,
                        "Microphone permission was not granted."
                    )
                )
            )
        }
    }

    override fun startListeningFromBackground(onEvent: (SttEvent) -> Unit) { if (sessionState != VoiceSessionState.IDLE) return; if (permissionManager.verifyPermission(Capability.MICROPHONE) != CapabilityStatus.ON || !capabilityEngine.isAvailable(Capability.MICROPHONE)) { sessionState = VoiceSessionState.ERROR; onEvent(SttEvent(SttEventType.ERROR, error = VoiceError(VoiceErrorCode.MIC_PERMISSION_REQUIRED, "Microphone permission is required."))); return }; callback = onEvent; beginListening() }
    private fun beginListening() { sessionState = VoiceSessionState.STARTING; operationJob?.cancel(); operationJob = scope.launch { val language = runCatching { VoiceLanguage.valueOf(settings.voiceLanguage.first()) }.getOrDefault(VoiceLanguage.HINGLISH); sttManager.setPreferredProvider(settings.preferredSttProvider.first()); val result = sttManager.start(language) { event -> when (event.type) { SttEventType.PARTIAL -> callback(event); SttEventType.FINAL -> { sessionState = VoiceSessionState.PROCESSING; callback(event); processFinal(event.result?.text, event.result?.language ?: language) }; SttEventType.ERROR -> { sessionState = VoiceSessionState.ERROR; callback(event) } } }; result.onSuccess { activeSttProvider = it; sessionState = VoiceSessionState.LISTENING }.onFailure { sessionState = VoiceSessionState.ERROR; callback(SttEvent(SttEventType.ERROR, error = VoiceError(VoiceErrorCode.STT_PROVIDER_UNAVAILABLE, "Speech recognition is unavailable.", it.message))) } } }
    private fun processFinal(rawText: String?, detectedLanguage: VoiceLanguage) { if (rawText.isNullOrBlank()) { sessionState = VoiceSessionState.IDLE; return }; val origin = if (mode == VoiceOperationMode.CALL_CONVERSATION_MODE) VoiceInputOrigin.CALLER else VoiceInputOrigin.AUTHORIZED_USER; if (!actionGate.mayEnterActionPipeline(origin, true)) { sessionState = VoiceSessionState.IDLE; return }; val normalized = normalizer.normalize(rawText); if (normalized.isBlank()) { sessionState = VoiceSessionState.IDLE; return }; operationJob?.cancel(); operationJob = scope.launch(Dispatchers.Default) { val result = aiOrchestrator.process(AiRequest(normalized), voiceAuthenticated); withContext(Dispatchers.Main.immediate) { result.onSuccess { handleAiResponse(it, detectedLanguage) }.onFailure { error -> sessionState = VoiceSessionState.ERROR; callback(SttEvent(SttEventType.ERROR, error = VoiceError(VoiceErrorCode.VOICE_SESSION_ERROR, "I couldn't process that voice request.", error.message))) } } } }
    private fun handleAiResponse(response: AiResponse, language: VoiceLanguage) { sessionState = VoiceSessionState.IDLE; if (response.text.isBlank()) return; scope.launch { if (settings.autoSpeak.first()) speak(response.text) else callback(SttEvent(SttEventType.FINAL, SttResult(response.text, language, null, "ai_response", true))) } }
    override fun stopListening() { if (sessionState == VoiceSessionState.IDLE) return; sessionState = VoiceSessionState.STOPPING; activeSttProvider?.let(sttManager::stop); activeSttProvider = null; operationJob?.cancel(); operationJob = null; sessionState = VoiceSessionState.IDLE }
    override fun cancelListening() = stopListening()
    override fun speak(text: String, onComplete: (Result<Unit>) -> Unit) { stopSpeaking(); sessionState = VoiceSessionState.SPEAKING; speakingState = SpeakingState.SPEAKING; scope.launch { val language = runCatching { VoiceLanguage.valueOf(settings.voiceLanguage.first()) }.getOrDefault(VoiceLanguage.HINGLISH); ttsManager.setPreferredProvider(settings.preferredTtsProvider.first()); val controls = ttsManager.firstConfigurableProvider(); controls?.setSpeechRate(settings.speechSpeed.first()); controls?.setSpeechPitch(settings.speechPitch.first()); controls?.selectVoice(settings.voiceName.first().ifBlank { null }); ttsManager.speak(text, language) { result -> speakingState = if (result.isSuccess) SpeakingState.IDLE else SpeakingState.ERROR; if (sessionState == VoiceSessionState.SPEAKING) sessionState = VoiceSessionState.IDLE; onComplete(result) } } }
    override fun stopSpeaking() { if (speakingState == SpeakingState.IDLE) return; speakingState = SpeakingState.STOPPING; ttsManager.stop(); speakingState = SpeakingState.IDLE; if (sessionState == VoiceSessionState.SPEAKING) sessionState = VoiceSessionState.IDLE }
    override fun getListeningState() = sessionState; override fun getSpeakingState() = speakingState
    override fun setVoiceAuthenticated(authenticated: Boolean) { voiceAuthenticated = authenticated }
    override fun setCallConversationMode(enabled: Boolean) { voiceAuthenticated = false; mode = if (enabled) VoiceOperationMode.CALL_CONVERSATION_MODE else VoiceOperationMode.USER_COMMAND; aiOrchestrator.setCallConversationMode(enabled); if (enabled) stopListening() }
    override fun close() { stopListening(); stopSpeaking(); scope.cancel() }
}
