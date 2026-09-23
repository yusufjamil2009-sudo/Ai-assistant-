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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class IncomingCallActivity : ComponentActivity() {

        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CallControlScreen(
                    caller = CallSession.callerNumber ?: "Unknown caller",
                    status = CallSession.status,
                    userJoined = CallSession.userJoined,
                    isMuted = CallSession.isMuted,
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

@Composable
private fun CallControlScreen(
    caller: String,
    status: String,
    userJoined: Boolean,
    isMuted: Boolean,
    onAnswer: () -> Unit,
    onJoin: () -> Unit,
    onListen: () -> Unit,
    onMute: () -> Unit,
    onEnd: () -> Unit,
    onDecline: () -> Unit
) {
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
                Text(caller, style = MaterialTheme.typography.titleLarge)
                Text("Status: $status")

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
