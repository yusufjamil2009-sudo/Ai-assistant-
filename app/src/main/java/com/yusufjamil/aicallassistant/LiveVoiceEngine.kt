package com.yusufjamil.aicallassistant

import android.content.Context
import android.telecom.Call

/**
 * Part 4 voice orchestration boundary.
 *
 * Important Android limitation:
 * InCallService gives the default phone app control of a cellular call, but a
 * normal AudioRecord/microphone capture is not a supported transparent bridge
 * for the remote cellular call audio. Therefore this class intentionally does
 * not pretend to stream SIM-call audio through the phone microphone.
 *
 * Part 4 establishes the STT -> LLM -> TTS orchestration contract. Part 5
 * supplies real provider clients and keys. A device-supported call-audio path
 * must be verified before enabling automatic caller-audio interception.
 */
object LiveVoiceEngine {

    @Volatile
    var state: VoiceEngineState = VoiceEngineState.IDLE
        private set

    @Volatile
    var lastTranscript: String = ""
        private set

    @Volatile
    var lastResponse: String = ""
        private set

    fun startForCurrentCall(context: Context) {
        val call = CallSession.currentCall ?: return
        start(context, call)
    }

    fun start(context: Context, call: Call) {
        if (state == VoiceEngineState.RUNNING) return

        state = VoiceEngineState.WAITING_FOR_AUDIO_BRIDGE
        lastTranscript = ""
        lastResponse = ""

        // Provider initialization is intentionally deferred to Part 5.
        // No SMS/message-reading path exists here.
    }

    fun submitCallerTextForPipeline(text: String) {
        if (text.isBlank()) return
        lastTranscript = text.trim()
        state = VoiceEngineState.STT_RECEIVED

        // Part 5 will connect this to the selected LLM provider.
        // The response is not fabricated here.
    }

    fun submitAssistantResponse(text: String) {
        if (text.isBlank()) return
        lastResponse = text.trim()
        state = VoiceEngineState.TTS_READY
    }

    fun stop() {
        state = VoiceEngineState.IDLE
    }
}

enum class VoiceEngineState {
    IDLE,
    WAITING_FOR_AUDIO_BRIDGE,
    STT_RECEIVED,
    LLM_READY,
    TTS_READY,
    RUNNING,
    ERROR
}
