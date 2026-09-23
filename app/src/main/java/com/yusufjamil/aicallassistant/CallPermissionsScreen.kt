package com.yusufjamil.aicallassistant

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * Call Permissions screen - shows and manages call-related permissions.
 */
@Composable
fun CallPermissionsScreen() {
    val context = LocalContext.current
    
    var isDefaultDialer by remember { mutableStateOf(checkDefaultDialer(context)) }
    
    // Check all permissions
    val phoneState = checkPermission(context, Manifest.permission.READ_PHONE_STATE)
    val phoneNumbers = checkPermission(context, Manifest.permission.READ_PHONE_NUMBERS)
    val contacts = checkPermission(context, Manifest.permission.READ_CONTACTS)
    val answerCalls = checkPermission(context, Manifest.permission.ANSWER_PHONE_CALLS)
    val microphone = checkPermission(context, Manifest.permission.RECORD_AUDIO)
    val notifications = checkPermission(context, Manifest.permission.POST_NOTIFICATIONS)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("CALL PERMISSIONS & SETUP", style = MaterialTheme.typography.headlineMedium)
        Text("Manage permissions required for the AI Call Assistant", style = MaterialTheme.typography.bodyMedium)
        
        // Phone State Permission
        PermissionCard(
            name = "Phone State",
            description = "Required to detect incoming calls and read call state",
            status = phoneState,
            isRequired = true,
            onRequest = { requestPermission(context, Manifest.permission.READ_PHONE_STATE) }
        )
        
        // Phone Numbers Permission
        PermissionCard(
            name = "Phone Numbers",
            description = "Required to read phone numbers from the device",
            status = phoneNumbers,
            isRequired = true,
            onRequest = { requestPermission(context, Manifest.permission.READ_PHONE_NUMBERS) }
        )
        
        // Contacts Permission
        PermissionCard(
            name = "Contacts",
            description = "Required to look up caller names from contacts",
            status = contacts,
            isRequired = false,
            onRequest = { requestPermission(context, Manifest.permission.READ_CONTACTS) }
        )
        
        // Answer Calls Permission
        PermissionCard(
            name = "Answer Calls",
            description = "Required to automatically answer incoming calls",
            status = answerCalls,
            isRequired = true,
            onRequest = { requestPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) }
        )
        
        // Microphone Permission
        PermissionCard(
            name = "Microphone",
            description = "Required for audio input (note: this does NOT provide remote call audio)",
            status = microphone,
            isRequired = false,
            onRequest = { requestPermission(context, Manifest.permission.RECORD_AUDIO) }
        )
        
        // Notifications Permission
        PermissionCard(
            name = "Notifications",
            description = "Required to show call notifications",
            status = notifications,
            isRequired = true,
            onRequest = { requestPermission(context, Manifest.permission.POST_NOTIFICATIONS) }
        )
        
        // Default Phone App
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Default Phone App", style = MaterialTheme.typography.titleMedium)
                        Text("Required for full InCallService functionality", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        if (isDefaultDialer) "GRANTED" else "NOT GRANTED",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                if (!isDefaultDialer) {
                    Text(
                        "Android requires the selected default phone app to provide the full InCallService experience.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { requestDefaultDialerRole(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Set as Default Phone App")
                    }
                } else {
                    Text(
                        "This app is currently the default phone app.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        // Permission Summary
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Permission Summary", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                
                val allGranted = listOf(phoneState, phoneNumbers, answerCalls, notifications).all { it == PermissionStatus.GRANTED }
                val optionalGranted = listOf(contacts, microphone).count { it == PermissionStatus.GRANTED }
                
                Text(if (allGranted) "All required permissions are granted ✓" else "Some required permissions are missing ✗")
                Text("$optionalGranted / 2 optional permissions granted")
                Text(if (isDefaultDialer) "Default phone app: Set ✓" else "Default phone app: Not set ✗")
            }
        }
        
        // Request All Required Button
        Button(
            onClick = {
                requestPermission(context, Manifest.permission.READ_PHONE_STATE)
                requestPermission(context, Manifest.permission.READ_PHONE_NUMBERS)
                requestPermission(context, Manifest.permission.ANSWER_PHONE_CALLS)
                requestPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("GRANT ALL REQUIRED PERMISSIONS")
        }
    }
}

@Composable
private fun PermissionCard(
    name: String,
    description: String,
    status: PermissionStatus,
    isRequired: Boolean,
    onRequest: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text(description, style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when (status) {
                            PermissionStatus.GRANTED -> "GRANTED"
                            PermissionStatus.NOT_GRANTED -> "NOT GRANTED"
                            PermissionStatus.OPTIONAL -> "OPTIONAL"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (isRequired) {
                        Text(" (REQUIRED)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            if (status != PermissionStatus.GRANTED) {
                Button(
                    onClick = onRequest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("REQUEST PERMISSION")
                }
            }
        }
    }
}

/**
 * Permission status enum.
 */
enum class PermissionStatus {
    GRANTED,
    NOT_GRANTED,
    OPTIONAL
}

/**
 * Check if a permission is granted.
 */
private fun checkPermission(context: Context, permission: String): PermissionStatus {
    return if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
        PermissionStatus.GRANTED
    } else {
        PermissionStatus.NOT_GRANTED
    }
}

/**
 * Check if this app is the default dialer.
 */
private fun checkDefaultDialer(context: Context): Boolean {
    val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
    return roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) ?: false
}

/**
 * Request a single permission.
 */
private fun requestPermission(context: Context, permission: String) {
    // This would typically use ActivityResultContracts.RequestPermission
    // For now, we'll just show that the request was made
}

/**
 * Request the default dialer role.
 */
private fun requestDefaultDialerRole(context: Context) {
    val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
    roleManager?.let { manager ->
        if (!manager.isRoleHeld(RoleManager.ROLE_DIALER)) {
            // This would typically launch the role request intent
            // For now, we'll just show that the request was made
        }
    }
}
