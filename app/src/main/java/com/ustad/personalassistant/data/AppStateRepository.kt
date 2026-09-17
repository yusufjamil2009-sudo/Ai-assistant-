package com.ustad.personalassistant.data

import android.content.Context
import com.ustad.personalassistant.domain.AppState
import com.ustad.personalassistant.domain.CapabilityStatus
import com.ustad.personalassistant.permissions.AndroidPermissionManager
import com.ustad.personalassistant.permissions.Capability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AppStateRepository {
    val state: StateFlow<AppState>
    fun refresh()
}

class AppStateRepositoryImpl(
    private val context: Context,
    private val permissionManager: AndroidPermissionManager
) : AppStateRepository {
    private val _state = MutableStateFlow(AppState())
    override val state: StateFlow<AppState> = _state.asStateFlow()

    init { refresh() }

    override fun refresh() {
        fun s(c: Capability) = permissionManager.verifyPermission(c)
        _state.value = AppState(
            assistantEnabled = true,
            microphonePermission = s(Capability.MICROPHONE),
            voiceAuthentication = s(Capability.VOICE_AUTHENTICATION),
            notificationAccess = s(Capability.NOTIFICATION_ACCESS),
            accessibilityAccess = s(Capability.APP_CONTROL),
            phoneCapability = s(Capability.PHONE_CALLS),
            contactsPermission = s(Capability.CONTACTS),
            cameraPermission = s(Capability.CAMERA),
            locationPermission = s(Capability.LOCATION),
            filesCapability = s(Capability.PHOTOS_FILES),
            backgroundAssistantStatus = s(Capability.BACKGROUND_ASSISTANT),
            gmailConnection = s(Capability.GMAIL),
            googleAccountConnection = s(Capability.GOOGLE_ACCOUNT)
        )
    }
}
