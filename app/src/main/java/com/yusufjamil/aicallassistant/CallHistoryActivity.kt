package com.yusufjamil.aicallassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

class CallHistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                HistoryScreen(
                    CallHistoryStore(this).all()
                ) { id ->
                    startActivity(
                        android.content.Intent(this, CallSummaryActivity::class.java)
                            .putExtra(CallSummaryActivity.EXTRA_ID, id)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    items: List<CallSummary>,
    open: (Long) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Call History", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        if (items.isEmpty()) {
            Text("No calls recorded yet.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items) { s ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .clickable { open(s.id) }
                    ) {
                        Column(
                            Modifier.padding(16.dp)
                        ) {
                            Text(
                                s.callerName ?: "Unknown caller",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(s.callerNumber ?: "Number unavailable")
                            Text(s.category + " \u2022 " + s.durationSeconds + "s")
                            Text(DateFormat.getDateTimeInstance().format(Date(s.startedAt)))
                        }
                    }
                }
            }
        }
    }
}
