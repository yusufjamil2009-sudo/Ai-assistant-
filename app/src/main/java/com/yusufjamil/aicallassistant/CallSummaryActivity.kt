package com.yusufjamil.aicallassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

class CallSummaryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        val summary = if (id > 0) CallHistoryStore(this).find(id) else null
        setContent { MaterialTheme { SummaryCard(summary, ::finish) } }
    }
    companion object { const val EXTRA_ID = "summary_id" }
}

@Composable
private fun SummaryCard(summary: CallSummary?, onClose: () -> Unit) {
    Card(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("AI Call Summary", style = MaterialTheme.typography.headlineSmall)
            if (summary == null) {
                Text("Summary unavailable")
            } else {
                Text(summary.callerName ?: "Unknown caller", style = MaterialTheme.typography.titleLarge)
                Text(summary.callerNumber ?: "Number unavailable")
                Text(if (summary.savedContact) "Saved contact" else "Unknown caller")
                Text("Purpose: ${summary.purpose}")
                Text("Category: ${summary.category}")
                Text("Duration: ${summary.durationSeconds}s")
                Text("Date/time: ${DateFormat.getDateTimeInstance().format(Date(summary.startedAt))}")
                Text("Caller said: " + summary.callerSaid.ifBlank { "No transcript available" })
                Text("AI said: " + summary.assistantSaid.ifBlank { "No AI response recorded" })
                if (summary.importantPoints.isNotEmpty()) {
                    Text("Important points:")
                    summary.importantPoints.forEach { Text("• $it") }
                }
            }
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("CLOSE") }
        }
    }
}