package com.ustad.personalassistant.voice

import android.content.Context
import com.ustad.personalassistant.capability.CapabilityEngine
import com.ustad.personalassistant.permissions.Capability
import com.ustad.personalassistant.security.SecureConfigStore

/** Foundation only: no raw voice samples are stored and no voice model is run in Part 02. */
enum class VoiceAuthenticationState { OFF, ON, NOT_ENROLLED }

interface VoiceAuthenticationRepository {
    fun state(): VoiceAuthenticationState
    fun setEnabled(enabled: Boolean): Boolean
    fun markEnrollmentComplete(): Boolean
    fun clearEnrollment()
}

class SecureVoiceAuthenticationRepository(
    context: Context,
    private val capabilityEngine: CapabilityEngine
) : VoiceAuthenticationRepository {
    private val store = SecureConfigStore(context, "voice_auth_state")

    override fun state(): VoiceAuthenticationState {
        if (!capabilityEngine.isAvailable(Capability.MICROPHONE)) return VoiceAuthenticationState.OFF
        val enrolled = store.get("enrolled") == "true"
        val enabled = store.get("enabled") == "true"
        return when {
            !enrolled -> VoiceAuthenticationState.NOT_ENROLLED
            enabled -> VoiceAuthenticationState.ON
            else -> VoiceAuthenticationState.OFF
        }
    }

    override fun setEnabled(enabled: Boolean): Boolean {
        if (enabled && (!capabilityEngine.isAvailable(Capability.MICROPHONE) || store.get("enrolled") != "true")) return false
        store.put("enabled", enabled.toString())
        return true
    }

    override fun markEnrollmentComplete(): Boolean {
        if (!capabilityEngine.isAvailable(Capability.MICROPHONE)) return false
        store.put("enrolled", "true")
        store.put("enabled", "false")
        return true
    }

    override fun clearEnrollment() {
        store.put("enrolled", "false")
        store.put("enabled", "false")
    }
}
