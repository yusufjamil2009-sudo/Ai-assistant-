package com.yusufjamil.aicallassistant
import android.content.Context
import android.telecom.Call

/**
 * Live Voice Engine - manages the AI conversation pipeline.
 * Uses the CallAudioTransport abstraction for audio handling.
 */
object LiveVoiceEngine {
    @Volatile var state: VoiceEngineState = VoiceEngineState.IDLE; private set
    @Volatile var lastTranscript: String = ""; private set
    @Volatile var lastResponse: String = ""; private set
    private var activeCall: Call? = null
    
    /**
     * Start the voice engine for the current call.
     */
    fun startForCurrentCall(context: Context) {
        CallSession.currentCall?.let { start(context, it) }
    }
    
    /**
     * Start the voice engine for a specific call.
     */
    @Synchronized fun start(context: Context, call: Call) {
        if (activeCall === call && state != VoiceEngineState.IDLE) return
        activeCall = call
        lastTranscript = ""
        lastResponse = ""
        
        // Check if call audio transport is available
        state = if (CallAudioTransportProvider.isAvailable()) {
            VoiceEngineState.RUNNING
        } else {
            VoiceEngineState.WAITING_FOR_AUDIO_BRIDGE
        }
        
        // Start the conversation loop if audio is available
        if (state == VoiceEngineState.RUNNING) {
            startConversationLoop(context)
        }
    }
    
    /**
     * Submit caller text for the pipeline (for manual testing).
     */
    fun submitCallerTextForPipeline(text: String) {
        if (text.isBlank()) return
        lastTranscript = text.trim()
        state = VoiceEngineState.STT_RECEIVED
    }
    
    /**
     * Process caller text through the full pipeline.
     */
    fun processCallerText(context: Context, text: String): String {
        if (text.isBlank()) return ""
        submitCallerTextForPipeline(text)
        
        val name = AppSettings.name(context).ifBlank { "the owner" }
        val language = AppSettings.language(context).ifBlank { "English" }
        val prompt = "You are a concise phone AI assistant for " + name + ". Reply in " + language + ". " +
                "Be polite, ask one useful clarification when needed, never claim to have completed an action you cannot actually perform, " +
                "and keep the reply short. Caller said: " + text.take(6000)
        
        val response = ProviderApiClient.chatWithFallback(context, prompt)
        if (response.isBlank()) {
            state = VoiceEngineState.ERROR
            return ""
        }
        state = VoiceEngineState.LLM_READY
        submitAssistantResponse(response)
        return response
    }
    
    /**
     * Generate the AI greeting.
     */
    fun greeting(context: Context): String {
        val name = AppSettings.name(context).ifBlank { "the owner" }
        val language = AppSettings.language(context).lowercase()
        val female = AppSettings.voice(context).equals("Female", true)
        
        return if (language.startsWith("hindi") || language.startsWith("hinglish"))
            "Hello, main " + name + " ka AI Assistant " + if (female) "baat kar rahi hoon." else "baat kar raha hoon."
        else
            "Hello, I am " + name + "\'s AI Assistant."
    }
    
    /**
     * Submit the assistant response.
     */
    fun submitAssistantResponse(text: String) {
        if (text.isBlank()) return
        lastResponse = text.trim()
        state = VoiceEngineState.TTS_READY
    }
    
    /**
     * Stop the voice engine.
     */
    @Synchronized fun stop() {
        activeCall = null
        state = VoiceEngineState.IDLE
    }
    
    /**
     * Start the conversation loop.
     * This is a simplified version that acknowledges Androids limitations.
     */
    private fun startConversationLoop(context: Context) {
        // On standard Android, we cannot access the remote SIM call audio PCM stream
        // The voice engine state reflects this limitation
        state = VoiceEngineState.WAITING_FOR_AUDIO_BRIDGE
    }
}

/**
 * Interface for call audio bridge provider.
 * This is a legacy interface for backward compatibility.
 */
interface CallAudioBridgeProvider {
    fun isReady(): Boolean
    fun readCallerAudio(): ByteArray?
    fun writeAssistantAudio(pcm16: ByteArray): Boolean
    
    companion object {
        @Volatile private var provider: CallAudioBridgeProvider? = null
        fun install(value: CallAudioBridgeProvider?) { provider = value }
        fun isAvailable(): Boolean = provider?.isReady() == true
        fun readCallerAudio(): ByteArray? = provider?.readCallerAudio()
        fun writeAssistantAudio(pcm16: ByteArray): Boolean = provider?.writeAssistantAudio(pcm16) == true
    }
}

/**
 * Voice engine state enum.
 */
enum class VoiceEngineState {
    IDLE,
    WAITING_FOR_AUDIO_BRIDGE,
    STT_RECEIVED,
    LLM_READY,
    TTS_READY,
    RUNNING,
    ERROR
}
