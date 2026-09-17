package com.ustad.personalassistant.permissions

import android.app.Activity
import com.ustad.personalassistant.domain.CapabilityStatus

interface PermissionManager {
    fun currentStatus(capability: Capability): CapabilityStatus
    fun requestPermission(activity: Activity, capability: Capability)
    fun openSystemSettings(activity: Activity, capability: Capability)
    fun verifyPermission(capability: Capability): CapabilityStatus
    fun explainPermission(capability: Capability): String
    fun handlePermissionResult(capability: Capability): CapabilityStatus = verifyPermission(capability)
}

enum class Capability {
    MICROPHONE, VOICE_AUTHENTICATION, NOTIFICATION_ACCESS, APP_CONTROL, PHONE_CALLS, CONTACTS, OPEN_APPS, PHOTOS_FILES, LOCATION, CAMERA, BACKGROUND_ASSISTANT, GMAIL, GOOGLE_ACCOUNT, SMS
}
