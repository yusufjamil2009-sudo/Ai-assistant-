package com.ustad.personalassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ustad.personalassistant.domain.AppState
import com.ustad.personalassistant.domain.CapabilityStatus
import com.ustad.personalassistant.permissions.Capability
import com.ustad.personalassistant.ui.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel> {
        val app = application as UstadApplication
        MainViewModel.Factory(app.appStateRepository, app.permissionManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { UstadApp(viewModel) }
    }
}

@Composable
private fun UstadApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? MainActivity

    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    MaterialTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = false, onClick = { navController.navigate("home") }, icon = { Icon(Icons.Outlined.Home, null) }, label = { Text("Home") })
                    NavigationBarItem(selected = false, onClick = { navController.navigate("permissions") }, icon = { Icon(Icons.Outlined.Lock, null) }, label = { Text("Permissions") })
                    NavigationBarItem(selected = false, onClick = { navController.navigate("settings") }, icon = { Icon(Icons.Outlined.Settings, null) }, label = { Text("Settings") })
                }
            }
        ) { padding ->
            NavHost(navController, startDestination = "home", modifier = Modifier.padding(padding)) {
                composable("home") { HomeScreen(state) }
                composable("permissions") { PermissionCenterScreen(state, viewModel, activity) }
                composable("settings") { SettingsScreen() }
            }
        }
    }
}

@Composable
private fun HomeScreen(state: AppState) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF7FAFD)).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("USTAD", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF071A33))
        Text("Personal AI Assistant", style = MaterialTheme.typography.titleMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Assistant status", style = MaterialTheme.typography.titleLarge)
                StatusRow("Assistant", if (state.assistantEnabled) CapabilityStatus.ON else CapabilityStatus.OFF)
                Text("Permission and capability controls are centralized and refreshed from Android system state.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Security baseline", style = MaterialTheme.typography.titleLarge)
                Text("No lock-screen bypass, silent permission grants, financial-app automation or covert microphone loop is implemented.")
            }
        }
    }
}

@Composable
private fun PermissionCenterScreen(state: AppState, viewModel: MainViewModel, activity: MainActivity?) {
    val items = listOf(
        Capability.MICROPHONE to ("🎙️ Microphone" to state.microphonePermission),
        Capability.VOICE_AUTHENTICATION to ("🗣️ Voice Authentication" to state.voiceAuthentication),
        Capability.NOTIFICATION_ACCESS to ("🔔 Notification Access" to state.notificationAccess),
        Capability.APP_CONTROL to ("♿ App Control" to state.accessibilityAccess),
        Capability.PHONE_CALLS to ("📞 Phone Calls" to state.phoneCapability),
        Capability.CONTACTS to ("👤 Contacts" to state.contactsPermission),
        Capability.OPEN_APPS to ("📱 Open Apps" to state.openAppsCapability),
        Capability.PHOTOS_FILES to ("📁 Photos & Files" to state.filesCapability),
        Capability.LOCATION to ("📍 Location" to state.locationPermission),
        Capability.CAMERA to ("📷 Camera" to state.cameraPermission),
        Capability.BACKGROUND_ASSISTANT to ("🎧 Background Assistant" to state.backgroundAssistantStatus),
        Capability.GMAIL to ("🔵 Gmail" to state.gmailConnection),
        Capability.GOOGLE_ACCOUNT to ("🟢 Google Account" to state.googleAccountConnection)
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Permission Center", style = MaterialTheme.typography.headlineMedium)
        Text("Statuses are derived from Android/system state. Future connections remain unconfigured until their OAuth implementation is added.")
        items.forEach { (capability, data) -> CapabilityCard(capability, data.first, data.second, viewModel, activity) }
    }
}

@Composable
private fun CapabilityCard(capability: Capability, title: String, status: CapabilityStatus, viewModel: MainViewModel, activity: MainActivity?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(viewModel.explain(capability), style = MaterialTheme.typography.bodySmall)
                }
                StatusRow("Status", status)
            }
            if (status == CapabilityStatus.OFF || status == CapabilityStatus.ACTION_REQUIRED || status == CapabilityStatus.CONNECT || status == CapabilityStatus.NOT_ENROLLED) {
                Button(enabled = activity != null, onClick = { activity?.let { viewModel.request(it, capability) } }) {
                    Text(
                        when (status) {
                            CapabilityStatus.CONNECT -> "CONNECT"
                            CapabilityStatus.ACTION_REQUIRED -> "OPEN SETTINGS"
                            CapabilityStatus.NOT_ENROLLED -> "SET UP"
                            else -> "ENABLE"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, status: CapabilityStatus) {
    val statusText = when (status) {
        CapabilityStatus.UNKNOWN -> "UNKNOWN"
        CapabilityStatus.ON -> "ON"
        CapabilityStatus.OFF -> "OFF"
        CapabilityStatus.CONNECT -> "CONNECT"
        CapabilityStatus.CONNECTED -> "CONNECTED"
        CapabilityStatus.NOT_AVAILABLE -> "NOT AVAILABLE"
        CapabilityStatus.ACTION_REQUIRED -> "ACTION REQUIRED"
        CapabilityStatus.NOT_ENROLLED -> "NOT ENROLLED"
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        if (status == CapabilityStatus.ON || status == CapabilityStatus.CONNECTED) Icon(Icons.Outlined.CheckCircle, null)
        Text(statusText, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SettingsScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text("Foundation settings are intentionally minimal while Part 02 establishes the centralized capability engine.")
        HorizontalDivider()
        listOf("Assistant", "Voice", "AI Providers", "Speech-to-Text", "Text-to-Speech", "Permissions", "Security", "Protected Apps", "Notifications", "Calls", "Gmail", "Google Account", "Data", "Diagnostics", "About").forEach {
            Card(Modifier.fillMaxWidth()) { Text(it, Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium) }
        }
        Spacer(Modifier.height(12.dp))
    }
}
