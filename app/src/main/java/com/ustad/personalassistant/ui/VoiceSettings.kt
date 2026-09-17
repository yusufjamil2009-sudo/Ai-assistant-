package com.ustad.personalassistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ustad.personalassistant.data.SettingsRepository
import com.ustad.personalassistant.voice.SpeechToTextConfig
import com.ustad.personalassistant.voice.SpeechToTextManager
import com.ustad.personalassistant.voice.TextToSpeechConfig
import com.ustad.personalassistant.voice.TextToSpeechManager
import com.ustad.personalassistant.voice.VoiceProviderRegistry
import kotlinx.coroutines.launch

@Composable
fun VoiceSettingsScreen(settings: SettingsRepository, registry: VoiceProviderRegistry, sttManager: SpeechToTextManager, ttsManager: TextToSpeechManager) {
    val language by settings.voiceLanguage.collectAsState(initial = "HINGLISH")
    val stt by settings.preferredSttProvider.collectAsState(initial = "AUTO")
    val tts by settings.preferredTtsProvider.collectAsState(initial = "AUTO")
    val speed by settings.speechSpeed.collectAsState(initial = 1f)
    val pitch by settings.speechPitch.collectAsState(initial = 1f)
    val autoSpeak by settings.autoSpeak.collectAsState(initial = true)
    val scope = rememberCoroutineScope()
    var editStt by remember { mutableStateOf<SpeechToTextConfig?>(null) }
    var editTts by remember { mutableStateOf<TextToSpeechConfig?>(null) }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Voice Settings")
        Text("VoiceEngine is the single microphone/STT/TTS entry point. Partial transcripts never enter the action pipeline.")
        HorizontalDivider()
        Selector("Language", language, listOf("HINDI", "HINGLISH", "ENGLISH", "MIXED")) { scope.launch { settings.setVoiceLanguage(it) } }
        Selector("Preferred STT Provider", stt, listOf("AUTO") + registry.sttConfigs().map { it.providerId }) { sttManager.setPreferredProvider(it); scope.launch { settings.setPreferredSttProvider(it) } }
        Selector("Preferred TTS Provider", tts, listOf("AUTO") + registry.ttsConfigs().map { it.providerId }) { ttsManager.setPreferredProvider(it); scope.launch { settings.setPreferredTtsProvider(it) } }
        Text("Speech Speed: ${"%.1f".format(speed)}")
        Slider(value = speed, onValueChange = { scope.launch { settings.setSpeechSpeed(it) } }, valueRange = 0.5f..2f)
        Text("Speech Pitch: ${"%.1f".format(pitch)}")
        Slider(value = pitch, onValueChange = { scope.launch { settings.setSpeechPitch(it) } }, valueRange = 0.5f..2f)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Auto Speak Voice Responses"); Switch(autoSpeak, { scope.launch { settings.setAutoSpeak(it) } }) }
        HorizontalDivider(); Text("STT Providers")
        registry.sttConfigs().forEach { config ->
            val health = sttManager.health()[config.providerId]
            ProviderCard(config.displayName, config.providerId, if (health == null) "NOT CONFIGURED" else health.status.name, registry.maskedKey(config.providerId)) { editStt = config }
        }
        Text("TTS Providers")
        registry.ttsConfigs().forEach { config ->
            val health = ttsManager.health()[config.providerId]
            ProviderCard(config.displayName, config.providerId, if (health == null) "NOT CONFIGURED" else health.status.name, registry.maskedKey(config.providerId)) { editTts = config }
        }
    }
    editStt?.let { config -> SttEditor(config, registry, onDismiss = { editStt = null }) }
    editTts?.let { config -> TtsEditor(config, registry, onDismiss = { editTts = null }) }
}

@Composable
private fun Selector(label: String, value: String, options: List<String>, onSelected: (String) -> Unit) { var expanded by remember { mutableStateOf(false) }; Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(label); OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(value) }; DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { expanded = false; onSelected(option) }) } } } }

@Composable
private fun ProviderCard(name: String, id: String, status: String, key: String, onConfigure: () -> Unit) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(name); Text("$id • $status"); Text("Key: $key"); OutlinedButton(onClick = onConfigure) { Text("CONFIGURE") } } } }

@Composable
private fun SttEditor(initial: SpeechToTextConfig, registry: VoiceProviderRegistry, onDismiss: () -> Unit) {
    var enabled by remember(initial.providerId) { mutableStateOf(initial.enabled) }; var endpoint by remember(initial.providerId) { mutableStateOf(initial.endpoint.orEmpty()) }; var model by remember(initial.providerId) { mutableStateOf(initial.model.orEmpty()) }; var priority by remember(initial.providerId) { mutableStateOf(initial.priority.toString()) }; var key by remember(initial.providerId) { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Configure ${initial.displayName}") }, text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Enabled"); Switch(enabled, { enabled = it }) }; OutlinedTextField(endpoint, { endpoint = it }, label = { Text("Endpoint") }, singleLine = true); OutlinedTextField(model, { model = it }, label = { Text("Model") }, singleLine = true); OutlinedTextField(priority, { priority = it.filter(Char::isDigit) }, label = { Text("Priority") }, singleLine = true); if (initial.providerId != "android_local") OutlinedTextField(key, { key = it }, label = { Text("API key (blank keeps existing)") }, singleLine = true) } }, confirmButton = { Button(onClick = { registry.saveStt(initial.copy(enabled = enabled, endpoint = endpoint.trim().ifBlank { null }, model = model.trim().ifBlank { null }, priority = priority.toIntOrNull() ?: initial.priority), key); onDismiss() }) { Text("SAVE") } }, dismissButton = { TextButton(onClick = { if (initial.providerId != "android_local") registry.removeKey(initial.providerId); onDismiss() }) { Text("REMOVE KEY") } })
}

@Composable
private fun TtsEditor(initial: TextToSpeechConfig, registry: VoiceProviderRegistry, onDismiss: () -> Unit) {
    var enabled by remember(initial.providerId) { mutableStateOf(initial.enabled) }; var endpoint by remember(initial.providerId) { mutableStateOf(initial.endpoint.orEmpty()) }; var model by remember(initial.providerId) { mutableStateOf(initial.model.orEmpty()) }; var voice by remember(initial.providerId) { mutableStateOf(initial.voiceId.orEmpty()) }; var priority by remember(initial.providerId) { mutableStateOf(initial.priority.toString()) }; var key by remember(initial.providerId) { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Configure ${initial.displayName}") }, text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Enabled"); Switch(enabled, { enabled = it }) }; OutlinedTextField(endpoint, { endpoint = it }, label = { Text("Endpoint") }, singleLine = true); OutlinedTextField(model, { model = it }, label = { Text("Model") }, singleLine = true); OutlinedTextField(voice, { voice = it }, label = { Text("Voice ID") }, singleLine = true); OutlinedTextField(priority, { priority = it.filter(Char::isDigit) }, label = { Text("Priority") }, singleLine = true); if (initial.providerId != "android_tts") OutlinedTextField(key, { key = it }, label = { Text("API key (blank keeps existing)") }, singleLine = true) } }, confirmButton = { Button(onClick = { registry.saveTts(initial.copy(enabled = enabled, endpoint = endpoint.trim().ifBlank { null }, model = model.trim().ifBlank { null }, voiceId = voice.trim().ifBlank { null }, priority = priority.toIntOrNull() ?: initial.priority), key); onDismiss() }) { Text("SAVE") } }, dismissButton = { TextButton(onClick = { if (initial.providerId != "android_tts") registry.removeKey(initial.providerId); onDismiss() }) { Text("REMOVE KEY") } })
}
