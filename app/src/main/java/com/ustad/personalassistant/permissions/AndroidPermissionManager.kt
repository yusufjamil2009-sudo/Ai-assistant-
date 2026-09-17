package com.ustad.personalassistant.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.provider.Telephony
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ustad.personalassistant.domain.CapabilityStatus
import com.ustad.personalassistant.security.SecureConfigStore

class AndroidPermissionManager(private val context: Context) : PermissionManager {
    override fun currentStatus(capability: Capability): CapabilityStatus = verifyPermission(capability)
    override fun verifyPermission(capability: Capability): CapabilityStatus = when (capability) {
        Capability.MICROPHONE -> runtime(Manifest.permission.RECORD_AUDIO)
        Capability.CAMERA -> runtime(Manifest.permission.CAMERA)
        Capability.CONTACTS -> runtime(Manifest.permission.READ_CONTACTS)
        Capability.LOCATION -> locationStatus()
        Capability.PHONE_CALLS -> runtime(Manifest.permission.CALL_PHONE)
        Capability.SMS -> smsStatus()
        Capability.NOTIFICATION_ACCESS -> if (isNotificationListenerEnabled()) CapabilityStatus.ON else CapabilityStatus.ACTION_REQUIRED
        Capability.APP_CONTROL -> if (isAccessibilityEnabled()) CapabilityStatus.ON else CapabilityStatus.ACTION_REQUIRED
        Capability.OPEN_APPS -> if (launchableAppsAvailable()) CapabilityStatus.ON else CapabilityStatus.NOT_AVAILABLE
        Capability.PHOTOS_FILES -> CapabilityStatus.ON
        Capability.BACKGROUND_ASSISTANT -> if (hasBackgroundService()) CapabilityStatus.ON else CapabilityStatus.NOT_AVAILABLE
        Capability.VOICE_AUTHENTICATION -> voiceAuthStatus()
        Capability.GMAIL -> CapabilityStatus.CONNECT
        Capability.GOOGLE_ACCOUNT -> CapabilityStatus.CONNECT
    }
    override fun requestPermission(activity: Activity, capability: Capability) {
        if (capability == Capability.SMS) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS), REQUEST_CODE_SMS)
            } else if (!isDefaultSmsApp()) {
                openSmsRoleSettings(activity)
            }
            return
        }
        val permission = when (capability) { Capability.MICROPHONE -> Manifest.permission.RECORD_AUDIO; Capability.CAMERA -> Manifest.permission.CAMERA; Capability.CONTACTS -> Manifest.permission.READ_CONTACTS; Capability.LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION; Capability.PHONE_CALLS -> Manifest.permission.CALL_PHONE; else -> null }
        if (permission == null) { openSystemSettings(activity, capability); return }
        if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) && ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED && activity.getPreferences(Context.MODE_PRIVATE).getBoolean("requested_$permission", false)) { openAppSettings(activity); return }
        activity.getPreferences(Context.MODE_PRIVATE).edit().putBoolean("requested_$permission", true).apply(); activity.requestPermissions(arrayOf(permission), REQUEST_CODE)
    }
    override fun openSystemSettings(activity: Activity, capability: Capability) {
        val intent = when (capability) { Capability.NOTIFICATION_ACCESS -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS); Capability.APP_CONTROL -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS); Capability.SMS -> if (!isDefaultSmsApp()) Intent("android.provider.action.DEFAULT_SMS_PACKAGE_SETTINGS") else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.parse("package:${context.packageName}") }; else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.parse("package:${context.packageName}") } }
        activity.startActivity(intent)
    }
    override fun explainPermission(capability: Capability): String = when (capability) {
        Capability.MICROPHONE -> "Allows the assistant to hear your voice commands."; Capability.VOICE_AUTHENTICATION -> "Local voice-authentication enrollment; microphone access alone does not enable it."; Capability.NOTIFICATION_ACCESS -> "Allows supported notifications to be read for future assistant summaries."; Capability.APP_CONTROL -> "Allows supported apps to be interacted with when you explicitly ask."; Capability.PHONE_CALLS -> "Allows supported call features to use the Android call capability."; Capability.CONTACTS -> "Allows supported assistant features to read contacts after Android grants access."; Capability.OPEN_APPS -> "Detects launchable installed apps using Android package-manager APIs."; Capability.PHOTOS_FILES -> "Uses modern Android photo and file picker patterns without broad storage access."; Capability.LOCATION -> "Allows supported assistant features to use device location when explicitly requested."; Capability.CAMERA -> "Allows camera-based assistant features when you explicitly use them."; Capability.BACKGROUND_ASSISTANT -> "Foreground-service capability for visible, user-controlled background listening."; Capability.GMAIL -> "Google OAuth connection foundation for future Gmail features."; Capability.GOOGLE_ACCOUNT -> "Google OAuth connection foundation without storing a Google password."; Capability.SMS -> "Reads SMS only with Android SMS permissions and the required default SMS role; outbound SMS always requires confirmation."
    }
    private fun runtime(permission: String): CapabilityStatus = if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) CapabilityStatus.ON else CapabilityStatus.OFF
    private fun locationStatus(): CapabilityStatus = if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) CapabilityStatus.ON else CapabilityStatus.OFF
    private fun smsStatus(): CapabilityStatus {
        val send = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        return when { !send || !read -> CapabilityStatus.OFF; !isDefaultSmsApp() -> CapabilityStatus.ACTION_REQUIRED; else -> CapabilityStatus.ON }
    }
    private fun isDefaultSmsApp(): Boolean = Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
    private fun openSmsRoleSettings(activity: Activity) { activity.startActivity(Intent("android.provider.action.DEFAULT_SMS_PACKAGE_SETTINGS")) }
    private fun isNotificationListenerEnabled(): Boolean { val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false; return enabled.split(":").any { it.startsWith(context.packageName + "/") } }
    private fun isAccessibilityEnabled(): Boolean { val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false; return enabled.split(":").any { it.startsWith(context.packageName + "/") } }
    private fun launchableAppsAvailable(): Boolean = context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PackageManager.MATCH_ALL).isNotEmpty()
    private fun hasBackgroundService(): Boolean = runCatching { context.packageManager.getServiceInfo(android.content.ComponentName(context, com.ustad.personalassistant.background.BackgroundAssistantService::class.java), PackageManager.GET_META_DATA); true }.getOrDefault(false)
    private fun voiceAuthStatus(): CapabilityStatus { if (runtime(Manifest.permission.RECORD_AUDIO) != CapabilityStatus.ON) return CapabilityStatus.OFF; val store = SecureConfigStore(context, "voice_auth_profile"); return when { store.get("profile") != null && store.get("enabled") == "true" -> CapabilityStatus.ON; store.get("profile") != null -> CapabilityStatus.OFF; else -> CapabilityStatus.NOT_ENROLLED } }
    private fun openAppSettings(activity: Activity) { activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.parse("package:${context.packageName}") }) }
    companion object { private const val REQUEST_CODE = 4101; private const val REQUEST_CODE_SMS = 4102 }
}
