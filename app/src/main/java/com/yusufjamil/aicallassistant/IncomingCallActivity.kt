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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class IncomingCallActivity : ComponentActivity() {

    private var call: Call? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        call = CallSession.currentCall

        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Incoming call", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                CallSession.callerNumber ?: "Unknown caller",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                "If you do not answer manually, the call-control engine will attempt to answer after 20 seconds."
                            )

                            Button(
                                onClick = {
                                    call?.let { if (it.state == Call.STATE_RINGING) it.answer(0) }
                                    finish()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Answer")
                            }

                            Button(
                                onClick = {
                                    call?.let { if (it.state == Call.STATE_RINGING) it.reject(false, null) }
                                    finish()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Decline")
                            }
                        }
                    }
                }
            }
        }
    }
}
