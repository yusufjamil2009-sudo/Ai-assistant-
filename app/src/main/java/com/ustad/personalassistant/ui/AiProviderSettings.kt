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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ustad.personalassistant.ai.AiProviderManager
import com.ustad.personalassistant.ai.ProviderConfig
import com.ustad.personalassistant.ai.RoutingPolicy

@Composable
fun AiProviderManagerScreen(manager: AiProviderManager) {
    var selected by remember { mutableStateOf<ProviderConfig?>(null) }
    var version by remember { mutableStateOf(0) }
    val configs = remember(version) { manager.allConfigs() }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("AI Provider Manager")
        Text("One central API manager routes requests by priority, capability, health and the selected privacy policy.")
        Text("Routing Policy")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoutingPolicy.values().forEach { policy -> FilterChip(selected = manager.routingPolicy() == policy, onClick = { manager.setRoutingPolicy(policy); version++ }, label = { Text(policy.name.replace('_', ' ')) }) }
        }
        HorizontalDivider()
        Text("Cloud Providers")
        configs.forEach { config ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(config.displayName)
                Text("Priority ${config.priority} • ${if (config.enabled) "Enabled" else "Disabled"} • ${manager.maskedKey(config.providerId)}")
                Text("Model: ${config.model.ifBlank { "Not configured" }}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { selected = config }) { Text("CONFIGURE") }
                    TextButton(onClick = { manager.save(config.copy(enabled = !config.enabled)); version++ }) { Text(if (config.enabled) "DISABLE" else "ENABLE") }
                    if (manager.hasKey(config.providerId)) TextButton(onClick = { manager.removeApiKey(config.providerId); version++ }) { Text("REMOVE KEY") }
                }
            } }
        }
        Text("On-Device AI")
        Text("State: NOT SUPPORTED until a compatible on-device runtime/model is supplied. Cloud routing remains available as configured.")
        Text("Usage & Health")
        Text("Usage is tracked in memory for the current process; provider health includes latency, failures and cooldown state. No prompt content is stored by the usage tracker.")
    }
    selected?.let { config -> ProviderEditorDialog(manager, config, onDismiss = { selected = null }, onSaved = { selected = null; version++ }) }
}

@Composable
private fun ProviderEditorDialog(manager: AiProviderManager, initial: ProviderConfig, onDismiss: () -> Unit, onSaved: () -> Unit) {
    var model by remember(initial.providerId) { mutableStateOf(initial.model) }
    var endpoint by remember(initial.providerId) { mutableStateOf(initial.endpoint.orEmpty()) }
    var key by remember(initial.providerId) { mutableStateOf("") }
    var priority by remember(initial.providerId) { mutableStateOf(initial.priority.toString()) }
    var timeout by remember(initial.providerId) { mutableStateOf(initial.timeoutMs.toString()) }
    var retries by remember(initial.providerId) { mutableStateOf(initial.retryCount.toString()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Configure ${initial.displayName}") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(model, { model = it }, label = { Text("Model") }, singleLine = true)
            OutlinedTextField(endpoint, { endpoint = it }, label = { Text("Endpoint") }, singleLine = true)
            OutlinedTextField(key, { key = it }, label = { Text("API key (leave blank to keep existing)") }, singleLine = true)
            OutlinedTextField(priority, { priority = it.filter(Char::isDigit) }, label = { Text("Priority") }, singleLine = true)
            OutlinedTextField(timeout, { timeout = it.filter(Char::isDigit) }, label = { Text("Timeout ms") }, singleLine = true)
            OutlinedTextField(retries, { retries = it.filter(Char::isDigit) }, label = { Text("Retries 0–3") }, singleLine = true)
        }
    }, confirmButton = {
        Button(onClick = {
            manager.save(initial.copy(model = model.trim(), endpoint = endpoint.trim().ifBlank { null }, priority = priority.toIntOrNull() ?: initial.priority, timeoutMs = timeout.toLongOrNull() ?: initial.timeoutMs, retryCount = retries.toIntOrNull() ?: initial.retryCount), key.takeIf { it.isNotBlank() })
            onSaved()
        }) { Text("SAVE") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } })
}
