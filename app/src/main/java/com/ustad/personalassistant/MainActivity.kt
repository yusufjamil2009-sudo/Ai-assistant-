package com.ustad.personalassistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.*
import com.ustad.personalassistant.domain.AppState
import com.ustad.personalassistant.domain.CapabilityStatus
import com.ustad.personalassistant.messaging.MessagingPlatform
import com.ustad.personalassistant.permissions.Capability
import com.ustad.personalassistant.ui.AiProviderManagerScreen
import com.ustad.personalassistant.ui.MainViewModel
import com.ustad.personalassistant.ui.VoiceSettingsScreen
import com.ustad.personalassistant.voice.SttEvent
import com.ustad.personalassistant.voice.SttEventType
import com.ustad.personalassistant.voice.VoiceEngine
import com.ustad.personalassistant.voice.VoiceAuthEngineState
import com.ustad.personalassistant.voice.VoiceSessionState
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel> { val app = application as UstadApplication; MainViewModel.Factory(app.appStateRepository, app.permissionManager) }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); enableEdgeToEdge(); setContent { UstadApp(viewModel) }; handleGmailRedirect(intent) }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); handleGmailRedirect(intent) }
    private fun handleGmailRedirect(intent: Intent?) { val data = intent?.data ?: return; if (data.scheme == "ustad-gmail" && data.host == "oauth2redirect") (application as UstadApplication).gmailAuthManager.handleRedirect(data) }
}

@Composable
private fun UstadApp(viewModel: MainViewModel) {
    val navController = rememberNavController(); val state by viewModel.state.collectAsState(); val context = LocalContext.current; val activity = context as? MainActivity; val app = context.applicationContext as UstadApplication
    val backgroundEnabled by app.settingsRepository.backgroundAssistantEnabled.collectAsState(initial = false)
    DisposableEffect(activity, backgroundEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
                activity?.let {
                    app.voiceEngine.resumeListeningAfterPermission(it)
                    if (backgroundEnabled &&
                        app.permissionManager.verifyPermission(Capability.MICROPHONE) == CapabilityStatus.ON
                    ) {
                        app.backgroundAssistantManager.setEnabled(true)
                    }
                }
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }
    MaterialTheme { Scaffold(bottomBar = { NavigationBar { NavigationBarItem(selected = false, onClick = { navController.navigate("home") }, icon = { Icon(Icons.Outlined.Home, null) }, label = { Text("Home") }); NavigationBarItem(selected = false, onClick = { navController.navigate("permissions") }, icon = { Icon(Icons.Outlined.Lock, null) }, label = { Text("Permissions") }); NavigationBarItem(selected = false, onClick = { navController.navigate("settings") }, icon = { Icon(Icons.Outlined.Settings, null) }, label = { Text("Settings") }) } }) { padding -> NavHost(navController, startDestination = "home", modifier = Modifier.padding(padding)) { composable("home") { HomeScreen(state, app, activity) }; composable("permissions") { PermissionCenterScreen(state, viewModel, activity) }; composable("settings") { SettingsScreen(app, onAiProviders = { navController.navigate("ai-providers") }, onVoice = { navController.navigate("voice-settings") }, onMessaging = { navController.navigate("messaging") }) }; composable("ai-providers") { AiProviderManagerScreen(app.aiProviderManager) }; composable("voice-settings") { VoiceSettingsScreen(app.settingsRepository, app.voiceProviderRegistry, app.sttManager, app.ttsManager) }; composable("messaging") { MessagingStatusScreen(app, state, viewModel, activity) } } } }
}

@Composable
private fun HomeScreen(state: AppState, app: UstadApplication, activity: MainActivity?) {
    var transcript by remember { mutableStateOf("") }
    var voiceState by remember { mutableStateOf(VoiceSessionState.IDLE) }
    val backgroundState by app.backgroundAssistantManager.state.collectAsState()
    val backgroundEnabled by app.settingsRepository.backgroundAssistantEnabled.collectAsState(initial = false)
    val sessionState by app.voiceSessionManager.stateFlow.collectAsState()
    val voiceActivated = sessionState == com.ustad.personalassistant.voice.VoiceSessionManagerState.WAKE_DETECTED ||
        sessionState == com.ustad.personalassistant.voice.VoiceSessionManagerState.ACKNOWLEDGING ||
        sessionState == com.ustad.personalassistant.voice.VoiceSessionManagerState.AUTHENTICATING ||
        sessionState == com.ustad.personalassistant.voice.VoiceSessionManagerState.COMMAND_LISTENING ||
        sessionState == com.ustad.personalassistant.voice.VoiceSessionManagerState.PROCESSING ||
        sessionState == com.ustad.personalassistant.voice.VoiceSessionManagerState.RESPONDING
    val onVoiceEvent: (SttEvent) -> Unit = { event ->
        if (event.type == SttEventType.PARTIAL || event.type == SttEventType.FINAL) transcript = event.result?.text.orEmpty()
        voiceState = app.voiceEngine.getListeningState()
    }
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF7FAFD)).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("USTAD", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF071A33))
        Text("Personal AI Assistant", style = MaterialTheme.typography.titleMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Assistant status", style = MaterialTheme.typography.titleLarge)
                StatusRow("Assistant", if (state.assistantEnabled) CapabilityStatus.ON else CapabilityStatus.OFF)
                Text("Background voice control can stay active through the foreground assistant service.")
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(if (backgroundEnabled && backgroundState == com.ustad.personalassistant.background.BackgroundAssistantState.ACTIVE) "VOICE ASSISTANT ACTIVE" else "VOICE ASSISTANT OFF", style = MaterialTheme.typography.titleLarge)
                Button(
                    enabled = activity != null,
                    onClick = {
                        val a = activity ?: return@Button
                        if (backgroundEnabled) {
                            app.backgroundAssistantManager.setEnabled(false)
                        } else {
                            runBlocking { app.settingsRepository.setBackgroundAssistantEnabled(true) }
                            if (app.permissionManager.verifyPermission(Capability.MICROPHONE) == CapabilityStatus.ON) {
                                app.backgroundAssistantManager.setEnabled(true)
                            } else {
                                app.permissionManager.requestPermission(a, Capability.MICROPHONE)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                ) {
                    Icon(Icons.Outlined.Mic, null)
                    Spacer(Modifier.width(10.dp))
                    Text(if (backgroundEnabled) "TURN VOICE OFF" else "TURN VOICE ON")
                }
                Text("Say “Hello Assistant” to activate the assistant.")
                Text("Background state: ${backgroundState.name}", style = MaterialTheme.typography.bodySmall)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Voice Engine", style = MaterialTheme.typography.titleLarge)
                Text("State: ${app.voiceEngine.getListeningState().name}")
                if (transcript.isNotBlank()) Text("Transcript: $transcript")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = activity != null, onClick = { activity?.let { app.voiceEngine.startListening(it, onVoiceEvent) } }) {
                        Icon(Icons.Outlined.Mic, null); Text("LISTEN")
                    }
                    Button(onClick = { app.voiceEngine.stopListening(); voiceState = VoiceSessionState.IDLE }) { Text("STOP") }
                }
                Text("Voice status: ${voiceState.name}")
            }
        }
        if (voiceActivated) {
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Mic, contentDescription = "Voice activated")
                    Spacer(Modifier.width(10.dp))
                    Text("VOICE ACTIVATED • LISTENING", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Microphone access is visible to Android through its privacy indicator and the persistent foreground-service notification.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun PermissionCenterScreen(state: AppState, viewModel: MainViewModel, activity: MainActivity?) { val items = listOf(Capability.MICROPHONE to ("🎙️ Microphone" to state.microphonePermission), Capability.VOICE_AUTHENTICATION to ("🗣️ Voice Authentication" to state.voiceAuthentication), Capability.NOTIFICATION_ACCESS to ("🔔 Notification Access" to state.notificationAccess), Capability.APP_CONTROL to ("♿ App Control" to state.accessibilityAccess), Capability.PHONE_CALLS to ("📞 Phone Calls" to state.phoneCapability), Capability.CONTACTS to ("👤 Contacts" to state.contactsPermission), Capability.OPEN_APPS to ("📱 Open Apps" to state.openAppsCapability), Capability.PHOTOS_FILES to ("📁 Photos & Files" to state.filesCapability), Capability.LOCATION to ("📍 Location" to state.locationPermission), Capability.CAMERA to ("📷 Camera" to state.cameraPermission), Capability.BACKGROUND_ASSISTANT to ("🎧 Background Assistant" to state.backgroundAssistantStatus), Capability.GMAIL to ("🔵 Gmail" to state.gmailConnection), Capability.GOOGLE_ACCOUNT to ("🟢 Google Account" to state.googleAccountConnection), Capability.SMS to ("💬 SMS" to state.smsCapability)); Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Permission Center", style = MaterialTheme.typography.headlineMedium); Text("Statuses are derived from Android/system state. Future connections remain unconfigured until their OAuth implementation is added."); items.forEach { (capability, data) -> CapabilityCard(capability, data.first, data.second, viewModel, activity) } } }
@Composable private fun CapabilityCard(capability: Capability, title: String, status: CapabilityStatus, viewModel: MainViewModel, activity: MainActivity?) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(viewModel.explain(capability), style = MaterialTheme.typography.bodySmall) }; StatusRow("Status", status) }; if (status == CapabilityStatus.OFF || status == CapabilityStatus.ACTION_REQUIRED || status == CapabilityStatus.CONNECT || status == CapabilityStatus.NOT_ENROLLED) Button(enabled = activity != null, onClick = { activity?.let { viewModel.request(it, capability) } }) { Text(when (status) { CapabilityStatus.CONNECT -> "CONNECT"; CapabilityStatus.ACTION_REQUIRED -> "OPEN SETTINGS"; CapabilityStatus.NOT_ENROLLED -> "SET UP"; else -> "ENABLE" }) } } } }
@Composable private fun StatusRow(label: String, status: CapabilityStatus) { val statusText = when (status) { CapabilityStatus.UNKNOWN -> "UNKNOWN"; CapabilityStatus.ON -> "ON"; CapabilityStatus.OFF -> "OFF"; CapabilityStatus.CONNECT -> "CONNECT"; CapabilityStatus.CONNECTED -> "CONNECTED"; CapabilityStatus.NOT_AVAILABLE -> "NOT AVAILABLE"; CapabilityStatus.ACTION_REQUIRED -> "ACTION REQUIRED"; CapabilityStatus.NOT_ENROLLED -> "NOT ENROLLED" }; Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) { if (status == CapabilityStatus.ON || status == CapabilityStatus.CONNECTED) Icon(Icons.Outlined.CheckCircle, null); Text("$label: $statusText", style = MaterialTheme.typography.labelMedium) } }

@Composable
private fun SettingsScreen(app: UstadApplication, onAiProviders: () -> Unit, onVoice: () -> Unit, onMessaging: () -> Unit) {
    val backgroundEnabled by app.settingsRepository.backgroundAssistantEnabled.collectAsState(initial = false); val wakeEnabled by app.settingsRepository.wakeWordEnabled.collectAsState(initial = true); val authEnabled by app.settingsRepository.voiceAuthenticationEnabled.collectAsState(initial = false); val backgroundState by app.backgroundAssistantManager.state.collectAsState(); val enrolled = app.voiceAuthenticationEngine.isEnrolled(); val callEnabled by app.settingsRepository.callAssistantEnabled.collectAsState(initial = false); val callTimeout by app.settingsRepository.callAssistantTimeoutSeconds.collectAsState(initial = 25); val callGreeting by app.settingsRepository.callAssistantGreeting.collectAsState(initial = true); val callSummary by app.settingsRepository.callAssistantPostCallSummary.collectAsState(initial = true); var authMessage by remember { mutableStateOf("") }; var gmailMessage by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium); Text("Centralized settings preserve the existing capability, security and automation boundaries."); HorizontalDivider()
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Gmail", style = MaterialTheme.typography.titleMedium); Text(if (app.gmailAuthManager.isConnected()) "CONNECTED" else "DISCONNECTED"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { gmailMessage = app.gmailAuthManager.beginAuthorization().fold({ "Browser authorization started" }, { it.message ?: "OAuth could not start" }) }) { Text("CONNECT GMAIL") }; Button(onClick = { app.gmailAuthManager.disconnect(); gmailMessage = "Gmail disconnected" }) { Text("DISCONNECT") } }; if (gmailMessage.isNotBlank()) Text(gmailMessage) } }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Phone Calls", style = MaterialTheme.typography.titleMedium); Text("Telecom: ${if (app.callAssistantEngine.capabilityStatus().telecomAvailable) "AVAILABLE" else "UNAVAILABLE"}; Auto-answer: ${if (app.callAssistantEngine.capabilityStatus().autoAnswerAvailable) "AVAILABLE" else "UNAVAILABLE"}"); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(if (callEnabled) "ON" else "OFF"); Switch(checked = callEnabled, onCheckedChange = { runBlocking { app.settingsRepository.setCallAssistantEnabled(it) } }) }; Text("Unanswered-call timeout: ${callTimeout}s"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { runBlocking { app.settingsRepository.setCallAssistantTimeoutSeconds((callTimeout + 5).coerceAtMost(120)) } }) { Text("+5s") }; Button(onClick = { runBlocking { app.settingsRepository.setCallAssistantTimeoutSeconds((callTimeout - 5).coerceAtLeast(5)) } }) { Text("-5s") } }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Greeting enabled"); Switch(checked = callGreeting, onCheckedChange = { runBlocking { app.settingsRepository.setCallAssistantGreeting(it) } }) }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Post-call summary"); Switch(checked = callSummary, onCheckedChange = { runBlocking { app.settingsRepository.setCallAssistantPostCallSummary(it) } }) }; Text("Caller audio is never treated as owner authorization; public Android Telecom does not provide a general cellular call-audio injection path for this app.") } }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Background Assistant", style = MaterialTheme.typography.titleMedium); Text("Visible foreground-service assistant; microphone use stops when disabled."); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(if (backgroundEnabled) "ACTIVE" else "OFF"); Switch(checked = backgroundEnabled, onCheckedChange = { enabled -> if (enabled) { val result = app.backgroundAssistantManager.setEnabled(true); if (result.isSuccess) runBlocking { app.settingsRepository.setBackgroundAssistantEnabled(true) } } else { app.backgroundAssistantManager.setEnabled(false) } }) }; Text("State: ${backgroundState.name}") } }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Wake Word", style = MaterialTheme.typography.titleMedium); Text("Hello Assistant"); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(if (wakeEnabled) "ON" else "OFF"); Switch(checked = wakeEnabled, onCheckedChange = { runBlocking { app.settingsRepository.setWakeWordEnabled(it) } }) } } }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Voice Authentication", style = MaterialTheme.typography.titleMedium); Text(if (enrolled) "Authentication Status: Enrolled" else "Authentication Status: Not Enrolled"); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(if (authEnabled) "ON" else "OFF"); Switch(enabled = enrolled, checked = authEnabled, onCheckedChange = { value -> if (app.voiceAuthenticationEngine.status() != VoiceAuthEngineState.LOCKED_OUT) runBlocking { app.settingsRepository.setVoiceAuthenticationEnabled(value) } }); }; Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { authMessage = "Enrollment started"; app.voiceAuthenticationEngine.enroll { authMessage = it.name }; runBlocking { app.settingsRepository.setVoiceAuthenticationEnabled(false) } }) { Text(if (enrolled) "RE-ENROLL" else "SET UP") }; if (enrolled) Button(onClick = { app.voiceAuthenticationEngine.resetEnrollment(); runBlocking { app.settingsRepository.setVoiceAuthenticationEnabled(false) }; authMessage = "Enrollment reset" }) { Text("RESET") } }; if (authMessage.isNotBlank()) Text(authMessage) } }
        Button(onClick = onAiProviders, modifier = Modifier.fillMaxWidth()) { Text("AI PROVIDER MANAGER") }; Button(onClick = onVoice, modifier = Modifier.fillMaxWidth()) { Text("VOICE SETTINGS") }; Button(onClick = onMessaging, modifier = Modifier.fillMaxWidth()) { Text("MESSAGING STATUS") }
        listOf("Assistant", "Voice", "AI Providers", "Speech-to-Text", "Text-to-Speech", "Permissions", "Security", "Protected Apps", "Notifications", "Messaging", "Calls", "Gmail", "Google Account", "Data", "Diagnostics", "About").forEach { Card(Modifier.fillMaxWidth()) { Text(it, Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium) } }; Spacer(Modifier.height(12.dp))
    }
}

@Composable private fun MessagingStatusScreen(app: UstadApplication, state: AppState, viewModel: MainViewModel, activity: MainActivity?) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Messaging", style = MaterialTheme.typography.headlineMedium); Text("Only capabilities reported by Android and supported provider mechanisms are shown. Outbound messages require an authenticated voice session and explicit confirmation."); Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("WhatsApp", style = MaterialTheme.typography.titleLarge); StatusRow("Notification Access", state.notificationAccess); StatusRow("App Control", state.accessibilityAccess); StatusRow("Installed", if (app.messagingEngine.provider(MessagingPlatform.WHATSAPP)?.isAvailable() == true) CapabilityStatus.ON else CapabilityStatus.NOT_AVAILABLE) } }; Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Messenger", style = MaterialTheme.typography.titleLarge); StatusRow("Notification Access", state.notificationAccess); StatusRow("App Control", state.accessibilityAccess); StatusRow("Installed", if (app.messagingEngine.provider(MessagingPlatform.MESSENGER)?.isAvailable() == true) CapabilityStatus.ON else CapabilityStatus.NOT_AVAILABLE) } }; Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("SMS", style = MaterialTheme.typography.titleLarge); StatusRow("SMS Capability", state.smsCapability); if (state.smsCapability == CapabilityStatus.OFF || state.smsCapability == CapabilityStatus.ACTION_REQUIRED) Button(enabled = activity != null, onClick = { activity?.let { viewModel.request(it, Capability.SMS) } }) { Text("ENABLE SMS") } } }; Text("Private message bodies are not stored by the messaging engine by default; sensitive security content is redacted and not forwarded to the AI summarizer.", style = MaterialTheme.typography.bodySmall) } }
