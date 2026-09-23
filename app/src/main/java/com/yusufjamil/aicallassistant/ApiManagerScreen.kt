package com.yusufjamil.aicallassistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.concurrent.Executors

/**
 * Complete API Manager screen with all 20 providers organized by category.
 */
@Composable
fun ApiManagerScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = remember { ApiManager(context) }
    
    var selectedCategory by remember { mutableStateOf(ProviderCategory.BRAIN) }
    var selectedProviderId by remember { mutableStateOf("groq") }
    var showCredentials by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<ConnectionStatus?>(null) }
    
    // Get the selected provider config
    val selectedProvider = ProviderCatalog.getById(selectedProviderId)
    
    // Get credential fields for the selected provider
    val credentialFields = selectedProvider?.credentialFields ?: emptyList()
    
    // State for each credential field
    val credentialValues = remember {
        mutableMapOf<String, String>().apply {
            credentialFields.forEach { field ->
                put(field.name, manager.getCredential(selectedProviderId, field.name) ?: "")
            }
        }
    }
    
    // Routing state
    var brainPrimary by remember { mutableStateOf(AppSettings.primaryBrain(context)) }
    var brainBackup by remember { mutableStateOf(AppSettings.backupBrain(context)) }
    var sttPrimary by remember { mutableStateOf(AppSettings.primaryStt(context)) }
    var sttBackup by remember { mutableStateOf(AppSettings.backupStt(context)) }
    var ttsPrimary by remember { mutableStateOf(AppSettings.primaryTts(context)) }
    var ttsBackup by remember { mutableStateOf(AppSettings.backupTts(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text("AI API MANAGER", style = MaterialTheme.typography.headlineMedium)
        Text("Secure API key management for Brain, STT, and TTS providers", style = MaterialTheme.typography.bodyMedium)
        
        // Category Tabs
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Category:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CategoryTab(
                        category = ProviderCategory.BRAIN,
                        label = "BRAIN / LLM",
                        modifier = Modifier.weight(1f),
                        selected = selectedCategory == ProviderCategory.BRAIN,
                        onSelect = { selectedCategory = ProviderCategory.BRAIN; selectedProviderId = "groq" }
                    )
                    CategoryTab(
                        category = ProviderCategory.STT,
                        label = "STT / SPEECH TO TEXT",
                        modifier = Modifier.weight(1f),
                        selected = selectedCategory == ProviderCategory.STT,
                        onSelect = { selectedCategory = ProviderCategory.STT; selectedProviderId = "deepgram" }
                    )
                    CategoryTab(
                        category = ProviderCategory.TTS,
                        label = "TTS / TEXT TO SPEECH",
                        modifier = Modifier.weight(1f),
                        selected = selectedCategory == ProviderCategory.TTS,
                        onSelect = { selectedCategory = ProviderCategory.TTS; selectedProviderId = "elevenlabs" }
                    )
                }
            }
        }
        
        // Provider List
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Provider:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                
                val providers = ProviderCatalog.getByCategory(selectedCategory)
                providers.forEach { provider ->
                    val isConfigured = manager.isProviderConfigured(provider.id)
                    val statusText = if (isConfigured) "Configured" else "Not Configured"
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedProviderId == provider.id,
                            onClick = { selectedProviderId = provider.id }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(provider.displayName)
                        Spacer(Modifier.width(8.dp))
                        Text(statusText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        
        // Provider Configuration
        selectedProvider?.let { provider ->
            ProviderConfigCard(
                provider = provider,
                credentialFields = credentialFields,
                credentialValues = credentialValues,
                showCredentials = showCredentials,
                onToggleShow = { showCredentials = !showCredentials },
                onCredentialChange = { fieldName, value ->
                    credentialValues[fieldName] = value
                },
                onSave = {
                    credentialFields.forEach { field ->
                        val value = credentialValues[field.name] ?: ""
                        if (value.isNotBlank()) {
                            manager.saveCredential(provider.id, field.name, value)
                        }
                    }
                    testResult = ConnectionStatus.CONNECTED
                },
                onTest = {
                    testing = true
                    testResult = ConnectionStatus.TESTING
                    Executors.newSingleThreadExecutor().execute {
                        val result = manager.testConnection(provider.id)
                        runOnUiThread {
                            testResult = result
                            testing = false
                        }
                    }
                },
                onRemove = {
                    manager.deleteAllCredentials(provider.id)
                    credentialFields.forEach { field ->
                        credentialValues[field.name] = ""
                    }
                    testResult = ConnectionStatus.NOT_CONFIGURED
                },
                testing = testing,
                testResult = testResult
            )
        }
        
        // Primary/Backup Routing
        RoutingCard(
            brainPrimary = brainPrimary,
            brainBackup = brainBackup,
            sttPrimary = sttPrimary,
            sttBackup = sttBackup,
            ttsPrimary = ttsPrimary,
            ttsBackup = ttsBackup,
            onSetBrainPrimary = { id -> brainPrimary = id; AppSettings.saveBrainRouting(context, id, brainBackup) },
            onSetBrainBackup = { id -> brainBackup = id; AppSettings.saveBrainRouting(context, brainPrimary, id) },
            onSetSttPrimary = { id -> sttPrimary = id; AppSettings.saveSttRouting(context, id, sttBackup) },
            onSetSttBackup = { id -> sttBackup = id; AppSettings.saveSttRouting(context, sttPrimary, id) },
            onSetTtsPrimary = { id -> ttsPrimary = id; AppSettings.saveTtsRouting(context, id, ttsBackup) },
            onSetTtsBackup = { id -> ttsBackup = id; AppSettings.saveTtsRouting(context, ttsPrimary, id) }
        )
        
        // Provider Count Summary
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("API Manager Summary", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("Brain Providers: ${ProviderCatalog.brainProviders.size} (GROQ, GOOGLE GEMINI, SAMBANOVA, Z.AI/ZHIPU, MISTRAL AI, OPENROUTER)")
                Text("STT Providers: ${ProviderCatalog.sttProviders.size} (DEEPGRAM, GOOGLE CLOUD STT, ASSEMBLYAI, ELEVENLABS SCRIBE, GROQ WHISPER, MISTRAL VOXTRAL, OPENAI WHISPER)")
                Text("TTS Providers: ${ProviderCatalog.ttsProviders.size} (ELEVENLABS, GOOGLE CLOUD TTS, MICROSOFT AZURE, AMAZON POLLY, FISH AUDIO, CARTESIA, RIME)")
                Spacer(Modifier.height(8.dp))
                Text("Total: ${ProviderCatalog.all.size} providers")
            }
        }
    }
}

@Composable
private fun CategoryTab(
    category: ProviderCategory,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    Button(
        onClick = onSelect,
        modifier = modifier
    ) {
        Text(label)
    }
}

@Composable
private fun ProviderConfigCard(
    provider: ProviderConfig,
    credentialFields: List<CredentialField>,
    credentialValues: MutableMap<String, String>,
    showCredentials: Boolean,
    onToggleShow: () -> Unit,
    onCredentialChange: (String, String) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onRemove: () -> Unit,
    testing: Boolean,
    testResult: ConnectionStatus?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Provider Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(provider.displayName, style = MaterialTheme.typography.titleLarge)
                    Text(provider.category.name, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    when (testResult) {
                        ConnectionStatus.CONNECTED -> "CONNECTED"
                        ConnectionStatus.TESTING -> "TESTING..."
                        ConnectionStatus.NOT_CONFIGURED -> "NOT CONFIGURED"
                        ConnectionStatus.INVALID_KEY -> "INVALID KEY"
                        ConnectionStatus.AUTHENTICATION_FAILED -> "AUTH FAILED"
                        ConnectionStatus.RATE_LIMITED -> "RATE LIMITED"
                        ConnectionStatus.QUOTA_EXCEEDED -> "QUOTA EXCEEDED"
                        ConnectionStatus.NETWORK_ERROR -> "NETWORK ERROR"
                        ConnectionStatus.SERVER_ERROR -> "SERVER ERROR"
                        ConnectionStatus.ERROR -> "ERROR"
                        else -> "UNKNOWN"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Model Selection (for Brain providers)
            if (provider.category == ProviderCategory.BRAIN) {
                val models = when (provider.id) {
                    "groq" -> GroqBrainProvider.getSupportedModels()
                    "gemini" -> GoogleGeminiBrainProvider.getSupportedModels()
                    "sambanova" -> SambaNovaBrainProvider.getSupportedModels()
                    "zhipu" -> ZhipuBrainProvider.getSupportedModels()
                    "mistral" -> MistralBrainProvider.getSupportedModels()
                    "openrouter" -> OpenRouterBrainProvider.getSupportedModels()
                    else -> emptyList()
                }
                
                if (models.isNotEmpty()) {
                    var selectedModel by remember { mutableStateOf(provider.defaultModel ?: models.first()) }
                    
                    OutlinedTextField(
                        value = selectedModel,
                        onValueChange = { selectedModel = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Model") },
                        singleLine = true
                    )
                    
                    Spacer(Modifier.height(8.dp))
                }
            }
            
            // Credential Fields
            credentialFields.forEach { field ->
                val value = credentialValues[field.name] ?: ""
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = if (showCredentials || !field.isSecret) value else "",
                        onValueChange = { newValue -> onCredentialChange(field.name, newValue) },
                        modifier = Modifier.weight(1f),
                        label = { Text(field.label) },
                        singleLine = true,
                        visualTransformation = if (field.isSecret && !showCredentials) {
                            PasswordVisualTransformation()
                        } else {
                            VisualTransformation.None
                        }
                    )
                }
                
                Spacer(Modifier.height(8.dp))
            }
            
            // Show/Hide Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onToggleShow) {
                    Text(if (showCredentials) "HIDE" else "SHOW")
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    enabled = credentialFields.any { (credentialValues[it.name] ?: "").isNotBlank() }
                ) {
                    Text("SAVE")
                }
                
                Button(
                    onClick = onTest,
                    modifier = Modifier.weight(1f),
                    enabled = !testing && credentialFields.all { (credentialValues[it.name] ?: "").isNotBlank() }
                ) {
                    Text(if (testing) "TESTING..." else "TEST API")
                }
                
                Button(
                    onClick = onRemove,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("REMOVE")
                }
            }
        }
    }
}

@Composable
private fun RoutingCard(
    brainPrimary: String,
    brainBackup: String,
    sttPrimary: String,
    sttBackup: String,
    ttsPrimary: String,
    ttsBackup: String,
    onSetBrainPrimary: (String) -> Unit,
    onSetBrainBackup: (String) -> Unit,
    onSetSttPrimary: (String) -> Unit,
    onSetSttBackup: (String) -> Unit,
    onSetTtsPrimary: (String) -> Unit,
    onSetTtsBackup: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Primary & Backup Providers", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            
            // Brain Routing
            Text("BRAIN / LLM", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Primary:")
                ProviderDropdown(
                    category = ProviderCategory.BRAIN,
                    selected = brainPrimary,
                    onSelect = onSetBrainPrimary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Backup:")
                ProviderDropdown(
                    category = ProviderCategory.BRAIN,
                    selected = brainBackup,
                    onSelect = onSetBrainBackup
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            // STT Routing
            Text("STT / SPEECH TO TEXT", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Primary:")
                ProviderDropdown(
                    category = ProviderCategory.STT,
                    selected = sttPrimary,
                    onSelect = onSetSttPrimary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Backup:")
                ProviderDropdown(
                    category = ProviderCategory.STT,
                    selected = sttBackup,
                    onSelect = onSetSttBackup
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            // TTS Routing
            Text("TTS / TEXT TO SPEECH", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Primary:")
                ProviderDropdown(
                    category = ProviderCategory.TTS,
                    selected = ttsPrimary,
                    onSelect = onSetTtsPrimary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Backup:")
                ProviderDropdown(
                    category = ProviderCategory.TTS,
                    selected = ttsBackup,
                    onSelect = onSetTtsBackup
                )
            }
        }
    }
}

@Composable
private fun ProviderDropdown(
    category: ProviderCategory,
    selected: String,
    onSelect: (String) -> Unit
) {
    val providers = ProviderCatalog.getByCategory(category)
    var expanded by remember { mutableStateOf(false) }
    
    // Simple dropdown using buttons for now
    Button(onClick = { expanded = true }) {
        Text(selected.ifBlank { "Not Set" })
    }
    
    if (expanded) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                providers.forEach { provider ->
                    Button(
                        onClick = {
                            onSelect(provider.id)
                            expanded = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(provider.displayName)
                    }
                }
                Button(
                    onClick = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

// Helper function to run on UI thread
private fun runOnUiThread(action: () -> Unit) {
    android.os.Handler(android.os.Looper.getMainLooper()).post(action)
}
