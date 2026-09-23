package com.yusufjamil.aicallassistant

import android.Manifest
import android.app.role.RoleManager
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
import androidx.compose.runtime.LaunchedEffect
import java.util.concurrent.Executors
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    private val dialerRoleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Scaffold(
                    topBar = { TopAppBar(title = { Text("AI Call Assistant") }) }
                ) { padding ->
                    PartTwoSetupScreen(
                        onOpenHistory = { startActivity(android.content.Intent(this@MainActivity, CallHistoryActivity::class.java)) },
                        modifier = Modifier.padding(padding),
                        isDefaultDialer = isDefaultDialer(),
                        onRequestPermissions = ::requestRequiredPermissions,
                        onRequestDialerRole = ::requestDialerRole
                    )
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        val requested = listOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )
        val missing = requested.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
    }

    private fun isDefaultDialer(): Boolean {
        val roleManager = getSystemService(RoleManager::class.java) ?: return false
        return roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
    }

    private fun requestDialerRole() {
        val roleManager = getSystemService(RoleManager::class.java) ?: return
        if (!roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
            dialerRoleLauncher.launch(
                roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            )
        }
    }
}

@Composable
private fun PartTwoSetupScreen(
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
    isDefaultDialer: Boolean,
    onRequestPermissions: () -> Unit,
    onRequestDialerRole: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var name by remember { mutableStateOf(AppSettings.name(context)) }
    var language by remember { mutableStateOf(AppSettings.language(context)) }
    var voice by remember { mutableStateOf(AppSettings.voice(context)) }
    LaunchedEffect(name, language, voice) { AppSettings.saveProfile(context, name, language, voice) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Part 2 — Incoming Call Engine", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Native Telecom incoming-call controls are enabled. A ringing call waits 20 seconds before " +
                "automatic answer, and JOIN CALL / MUTE / END CALL remain available from the call controls."
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("AI voice status", style = MaterialTheme.typography.titleMedium)
                Text(
                    "The LLM/STT/TTS provider layer is ready, but Android's standard InCallService API " +
                        "does not expose a generic PCM stream of a normal SIM call to third-party apps. " +
                        "The app never pretends that microphone capture is remote caller audio."
                )
                Text("Voice engine: ${LiveVoiceEngine.state}")
            }
        }

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

        Button(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) {
            Text("Grant Required Permissions")
        }

        Button(onClick = onRequestDialerRole, modifier = Modifier.fillMaxWidth()) {
            Text(if (isDefaultDialer) "Default Phone App: Enabled" else "Set as Default Phone App")
        }

        Text(
            if (isDefaultDialer) {
                "The app currently holds the Android default dialer role."
            } else {
                "Android requires the selected default phone app to provide the full InCallService experience."
            }
        )

        ApiManagerScreen()

        Button(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) { Text("CALL HISTORY") }
        Text("SMS/message reading is not requested or declared by this project.")
    }
}

@Composable
private fun ApiManagerScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = remember { ApiManager(context) }
    var selected by remember { mutableStateOf(ProviderCatalog.all.first()) }
    var key by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Not Connected") }
    var azureRegion by remember { mutableStateOf(AppSettings.azureRegion(context)) }
    var primary by remember { mutableStateOf(AppSettings.primary(context)) }
    var backup by remember { mutableStateOf(AppSettings.backup(context)) }
    var testing by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("API Manager", style = MaterialTheme.typography.titleLarge)
            Text("Secure API keys • Brain / STT / TTS")
            OutlinedTextField(value = selected.displayName, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(), label = { Text("Selected provider") })
            OutlinedTextField(value = key, onValueChange = { key = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Paste API key") }, singleLine = true)
            Button(onClick = { if (manager.saveKey(selected.id, key)) { key = ""; status = "Key saved securely" } else status = "Enter an API key first" }, modifier = Modifier.fillMaxWidth()) { Text("SAVE KEY") }
            Button(onClick = { status = manager.keyConfigured(selected.id).message }, modifier = Modifier.fillMaxWidth()) { Text("CHECK KEY") }
            Button(enabled = !testing, onClick = { testing = true; status = "Testing..."; Executors.newSingleThreadExecutor().execute { val result = ProviderApiClient.test(context, selected.id); runOnUiThread { status = result.status + ": " + result.detail; testing = false } } }, modifier = Modifier.fillMaxWidth()) { Text("TEST API") }
            Text("Status: $status")
            if (selected.id == "azure_speech") {
                OutlinedTextField(value = azureRegion, onValueChange = { azureRegion = it; AppSettings.saveAzureRegion(context, it) }, modifier = Modifier.fillMaxWidth(), label = { Text("Azure region") }, singleLine = true)
            }
            Button(onClick = { manager.deleteKey(selected.id); key = ""; status = "API key removed" }, modifier = Modifier.fillMaxWidth()) { Text("REMOVE KEY") }
            Text("Primary: $primary")
            Text("Backup: $backup")
            Button(onClick = { primary = selected.id; AppSettings.saveRouting(context, primary, backup) }, modifier = Modifier.fillMaxWidth()) { Text("SET AS PRIMARY") }
            Button(onClick = { backup = selected.id; AppSettings.saveRouting(context, primary, backup) }, modifier = Modifier.fillMaxWidth()) { Text("SET AS BACKUP") }
            ProviderCatalog.all.forEach { p ->
                Button(onClick = { selected = p; status = if (manager.hasKey(p.id)) "Key stored" else "Not Connected" }, modifier = Modifier.fillMaxWidth()) {
                    Text(p.displayName + " • " + p.category)
                }
            }
        }
    }
}