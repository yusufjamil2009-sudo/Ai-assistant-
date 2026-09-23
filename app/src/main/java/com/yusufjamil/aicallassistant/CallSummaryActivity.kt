package com.yusufjamil.aicallassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

class CallSummaryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        val s = if (id > 0) CallHistoryStore(this).find(id) else null
        setContent {
            MaterialTheme {
                SummaryCard(s, ::finish)
            }
        }
    }

    companion object {
        const val EXTRA_ID = "summary_id"
    }
}

@Composable
private fun SummaryCard(s: CallSummary?, close: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AI Call Summary", style = MaterialTheme.typography.headlineSmall)
                    IconButton(onClick = close) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                if (s == null) {
                    Text("Summary unavailable")
                } else {
                    Text(
                        s.callerName ?: "Unknown caller",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(s.callerNumber ?: "Number unavailable")
                    Text(if (s.savedContact) "Saved contact" else "Unknown caller")
                    Text("Purpose: " + s.purpose)
                    Text("Category: " + s.category)
                    Text("Duration: " + s.durationSeconds + "s")
                    Text("Date/time: " + DateFormat.getDateTimeInstance().format(Date(s.startedAt)))
                    Text("Caller said: " + s.callerSaid.ifBlank { "No transcript available" })
                    Text("AI said: " + s.assistantSaid.ifBlank { "No AI response recorded" })
                    if (s.importantPoints.isNotEmpty()) {
                        Text("Important points:")
                        s.importantPoints.forEach { Text("\u2022 " + it) }
                    }
                }
                Button(onClick = close, Modifier.fillMaxWidth()) {
                    Text("CLOSE")
                }
            }
        }
    }
}
