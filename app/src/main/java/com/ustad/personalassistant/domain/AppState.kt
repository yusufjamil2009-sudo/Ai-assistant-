package com.ustad.personalassistant.domain

enum class CapabilityStatus { UNKNOWN, OFF, ON, CONNECT, CONNECTED, NOT_AVAILABLE, ACTION_REQUIRED, NOT_ENROLLED }

data class AppState(
    val assistantEnabled: Boolean = true,
    val microphonePermission: CapabilityStatus = CapabilityStatus.OFF,
    val voiceAuthentication: CapabilityStatus = CapabilityStatus.NOT_ENROLLED,
    val notificationAccess: CapabilityStatus = CapabilityStatus.OFF,
    val accessibilityAccess: CapabilityStatus = CapabilityStatus.OFF,
    val phoneCapability: CapabilityStatus = CapabilityStatus.OFF,
    val contactsPermission: CapabilityStatus = CapabilityStatus.OFF,
    val cameraPermission: CapabilityStatus = CapabilityStatus.OFF,
    val locationPermission: CapabilityStatus = CapabilityStatus.OFF,
    val filesCapability: CapabilityStatus = CapabilityStatus.ON,
    val openAppsCapability: CapabilityStatus = CapabilityStatus.ON,
    val backgroundAssistantStatus: CapabilityStatus = CapabilityStatus.NOT_AVAILABLE,
    val gmailConnection: CapabilityStatus = CapabilityStatus.CONNECT,
    val googleAccountConnection: CapabilityStatus = CapabilityStatus.CONNECT,
    val smsCapability: CapabilityStatus = CapabilityStatus.OFF
)

data class CapabilityInfo(val title: String, val description: String, val status: CapabilityStatus)
