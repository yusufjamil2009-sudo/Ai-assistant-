package com.yusufjamil.aicallassistant

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * Main Activity - provides the main settings and configuration UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
                    MainScreen(
                        onOpenHistory = { 
                            startActivity(android.content.Intent(this@MainActivity, CallHistoryActivity::class.java)) 
                        },
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

/**
 * Main screen composable.
 */
@Composable
private fun MainScreen(
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
    isDefaultDialer: Boolean,
    onRequestPermissions: () -> Unit,
    onRequestDialerRole: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(AppSettings.name(context)) }
    var language by remember { mutableStateOf(AppSettings.language(context)) }
    var voice by remember { mutableStateOf(AppSettings.voice(context)) }
    
    LaunchedEffect(name, language, voice) { 
        AppSettings.saveProfile(context, name, language, voice) 
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("AI Call Assistant", style = MaterialTheme.typography.headlineSmall)
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
                    "The LLM/STT/TTS provider layer is ready, but Androids standard InCallService API " +
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
                Text("AI ASSISTANT SETTINGS", style = MaterialTheme.typography.titleMedium)
                
                // Profile
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
                
                Text("Profile", style = MaterialTheme.typography.bodyMedium)
                Text("Language", style = MaterialTheme.typography.bodyMedium)
                Text("Voice", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // API Manager
        ApiManagerScreen()
        
        // Call Permissions
        CallPermissionsScreen()

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

        Button(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) { 
            Text("CALL HISTORY") 
        }
        Text("SMS/message reading is not requested or declared by this project.")
    }
}

/**
 * Helper to run on UI thread.
 */
private fun runOnUiThread(action: () -> Unit) {
    android.os.Handler(android.os.Looper.getMainLooper()).post(action)
}
