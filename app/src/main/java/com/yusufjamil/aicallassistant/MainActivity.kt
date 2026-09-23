package com.yusufjamil.aicallassistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            // Permission state is read by Android when the screen is reopened.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("AI Call Assistant") })
                    }
                ) { padding ->
                    PartOneSetupScreen(
                        modifier = Modifier.padding(padding),
                        onRequestPermissions = ::requestPartOnePermissions
                    )
                }
            }
        }
    }

    private fun requestPartOnePermissions() {
        val requested = listOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )

        val missing = requested.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }
}

@Composable
private fun PartOneSetupScreen(
    modifier: Modifier = Modifier,
    onRequestPermissions: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("English") }
    var voice by remember { mutableStateOf("Female") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Part 1 — Assistant setup",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "This is the native foundation for the AI call-attendant project. " +
                "Actual call answering and live AI voice handling are deliberately implemented in later parts."
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Assistant profile", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Your name") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = language,
                    onValueChange = { language = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Language") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = voice,
                    onValueChange = { voice = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Voice: Male or Female") },
                    singleLine = true
                )
            }
        }

        Button(
            onClick = onRequestPermissions,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Required Permissions")
        }

        Text("SMS/message reading is not requested or declared by this project.")
    }
}
