package com.ustad.personalassistant.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.ustad.personalassistant.domain.CapabilityStatus

class AndroidPermissionManager(private val context: Context) : PermissionManager {

    override fun currentStatus(capability: Capability): CapabilityStatus = verifyPermission(capability)

    override fun verifyPermission(capability: Capability): CapabilityStatus = when (capability) {
        Capability.MICROPHONE -> runtime(Manifest.permission.RECORD_AUDIO)
        Capability.CAMERA -> runtime(Manifest.permission.CAMERA)
        Capability.CONTACTS -> runtime(Manifest.permission.READ_CONTACTS)
        Capability.LOCATION -> locationStatus()
        Capability.PHONE_CALLS -> runtime(Manifest.permission.CALL_PHONE)
        Capability.NOTIFICATION_ACCESS -> if (isNotificationListenerEnabled()) CapabilityStatus.ON else CapabilityStatus.ACTION_REQUIRED
        Capability.APP_CONTROL -> if (isAccessibilityEnabled()) CapabilityStatus.ON else CapabilityStatus.ACTION_REQUIRED
        Capability.OPEN_APPS -> if (launchableAppsAvailable()) CapabilityStatus.ON else CapabilityStatus.NOT_AVAILABLE
        Capability.PHOTOS_FILES -> CapabilityStatus.ON
        Capability.BACKGROUND_ASSISTANT -> CapabilityStatus.NOT_AVAILABLE
        Capability.VOICE_AUTHENTICATION -> CapabilityStatus.NOT_AVAILABLE
        Capability.GMAIL -> CapabilityStatus.CONNECT
        Capability.GOOGLE_ACCOUNT -> CapabilityStatus.CONNECT
    }

    override fun requestPermission(activity: Activity, capability: Capability) {
        val permission = when (capability) {
            Capability.MICROPHONE -> Manifest.permission.RECORD_AUDIO
            Capability.CAMERA -> Manifest.permission.CAMERA
            Capability.CONTACTS -> Manifest.permission.READ_CONTACTS
            Capability.LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
            Capability.PHONE_CALLS -> Manifest.permission.CALL_PHONE
            else -> null
        }
        if (permission != null) activity.requestPermissions(arrayOf(permission), REQUEST_CODE)
        else openSystemSettings(activity, capability)
    }

    override fun openSystemSettings(activity: Activity, capability: Capability) {
        val action = when (capability) {
            Capability.NOTIFICATION_ACCESS -> Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            Capability.APP_CONTROL -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            else -> Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        }
        val intent = if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
            Intent(action).apply { data = android.net.Uri.parse("package:${context.packageName}") }
        } else Intent(action)
        activity.startActivity(intent)
    }

    override fun explainPermission(capability: Capability): String = when (capability) {
        Capability.MICROPHONE -> "Needed only when voice input is explicitly used."
        Capability.VOICE_AUTHENTICATION -> "Future voice-authentication capability; microphone access alone does not enable it."
        Capability.NOTIFICATION_ACCESS -> "Managed by Android Notification Access settings."
        Capability.APP_CONTROL -> "Future app-control capability managed by Android Accessibility settings."
        Capability.PHONE_CALLS -> "Uses Android call permissions; call automation is not implemented in Part 01."
        Capability.CONTACTS -> "Allows reading contacts after Android grants access."
        Capability.OPEN_APPS -> "Detects launchable apps using Android package-manager APIs."
        Capability.PHOTOS_FILES -> "Uses modern Android photo/file picker patterns without broad storage access."
        Capability.LOCATION -> "Uses Android location runtime permissions when location is requested."
        Capability.CAMERA -> "Allows camera access only after Android grants it."
        Capability.BACKGROUND_ASSISTANT -> "Future compliant foreground/background assistant capability."
        Capability.GMAIL -> "Reserved for future Google OAuth/Gmail integration."
        Capability.GOOGLE_ACCOUNT -> "Reserved for future Google OAuth integration."
    }

    private fun runtime(permission: String): CapabilityStatus =
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) CapabilityStatus.ON
        else CapabilityStatus.OFF

    private fun locationStatus(): CapabilityStatus = when {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> CapabilityStatus.ON
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> CapabilityStatus.ON
        else -> CapabilityStatus.OFF
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
        return enabled.contains(context.packageName)
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.contains(context.packageName)
    }

    private fun launchableAppsAvailable(): Boolean =
        context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PackageManager.MATCH_ALL).isNotEmpty()

    companion object { private const val REQUEST_CODE = 4101 }
}
