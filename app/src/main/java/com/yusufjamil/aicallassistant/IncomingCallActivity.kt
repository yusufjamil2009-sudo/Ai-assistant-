package com.yusufjamil.aicallassistant

import android.os.Bundle
import android.telecom.Call
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Incoming Call Activity - displays call controls and status.
 * This is the main UI for handling incoming calls.
 */
class IncomingCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CallControlScreen(
                    callerName = CallSession.callerName ?: CallSession.callerNumber ?: "Unknown caller",
                    callerNumber = CallSession.callerNumber ?: "Unknown",
                    status = CallSession.status,
                    userJoined = CallSession.userJoined,
                    isMuted = CallSession.isMuted,
                    audioStatus = LiveVoiceEngine.state,
                    onAnswer = {
                        AiCallServiceHolder.service?.answerNow()
                        finish()
                    },
                    onJoin = {
                        AiCallServiceHolder.service?.joinCall()
                    },
                    onListen = {
                        CallActionReceiver.dispatchListen()
                    },
                    onMute = {
                        val service = AiCallServiceHolder.service
                        service?.toggleMute()
                    },
                    onEnd = {
                        AiCallServiceHolder.service?.endCall()
                        finish()
                    },
                    onDecline = {
                        AiCallServiceHolder.service?.endCall()
                        finish()
                    }
                )
            }
        }
    }
}

/**
 * Call control screen composable.
 * Displays caller info, status, and call control buttons.
 */
@Composable
private fun CallControlScreen(
    callerName: String,
    callerNumber: String,
    status: String,
    userJoined: Boolean,
    isMuted: Boolean,
    audioStatus: VoiceEngineState,
    onAnswer: () -> Unit,
    onJoin: () -> Unit,
    onListen: () -> Unit,
    onMute: () -> Unit,
    onEnd: () -> Unit,
    onDecline: () -> Unit
) {
    // Map voice engine state to display text
    val audioStatusText = when (audioStatus) {
        VoiceEngineState.IDLE -> "Audio: Idle"
        VoiceEngineState.WAITING_FOR_AUDIO_BRIDGE -> "Caller audio access is unavailable on this Android device."
        VoiceEngineState.STT_RECEIVED -> "Processing caller speech..."
        VoiceEngineState.LLM_READY -> "Aether is thinking..."
        VoiceEngineState.TTS_READY -> "Aether is speaking..."
        VoiceEngineState.RUNNING -> "Listening for caller..."
        VoiceEngineState.ERROR -> "Call audio error"
    }

    // Map call status to display text
    val statusText = when (status) {
        "RINGING" -> "Ringing - Auto-answer in 20 seconds"
        "AI HANDLING" -> "AI handling call"
        "CONNECTED" -> "Call connected"
        "USER JOINED" -> "You joined the call"
        "LISTENING" -> "Listening..."
        else -> status
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("AI Call Assistant", style = MaterialTheme.typography.headlineSmall)
                Text(callerName, style = MaterialTheme.typography.titleLarge)
                Text(callerNumber, style = MaterialTheme.typography.bodyMedium)
                Text("Status: $statusText", style = MaterialTheme.typography.bodyMedium)
                Text(audioStatusText, style = MaterialTheme.typography.bodyMedium)

                if (status == "RINGING") {
                    Button(onClick = onAnswer, modifier = Modifier.fillMaxWidth()) {
                        Text("ANSWER")
                    }
                    Button(onClick = onDecline, modifier = Modifier.fillMaxWidth()) {
                        Text("DECLINE")
                    }
                } else {
                    Button(onClick = onJoin, modifier = Modifier.fillMaxWidth()) {
                        Text(if (userJoined) "JOINED" else "JOIN CALL")
                    }
                    Button(onClick = onListen, modifier = Modifier.fillMaxWidth()) {
                        Text("LISTEN")
                    }
                    Button(onClick = onMute, modifier = Modifier.fillMaxWidth()) {
                        Text(if (isMuted) "UNMUTE" else "MUTE")
                    }
                    Button(onClick = onEnd, modifier = Modifier.fillMaxWidth()) {
                        Text("END CALL")
                    }
                }
            }
        }
    }
}
