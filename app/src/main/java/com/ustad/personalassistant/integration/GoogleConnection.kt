package com.ustad.personalassistant.integration

import com.ustad.personalassistant.domain.CapabilityStatus

/** OAuth contract only. Credential exchange and Gmail operations belong to a later part. */
enum class OAuthConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

interface GmailConnection {
    fun connectGmail(): OAuthConnectionState
    fun disconnectGmail(): OAuthConnectionState
    fun getGmailConnectionState(): OAuthConnectionState
}

interface GoogleAccountConnection {
    fun connectGoogleAccount(): OAuthConnectionState
    fun disconnectGoogleAccount(): OAuthConnectionState
    fun getGoogleAccountConnectionState(): OAuthConnectionState
}

/** Safe placeholder until Google OAuth client configuration is supplied in a later part. */
class UnconfiguredGmailConnection : GmailConnection {
    override fun connectGmail() = OAuthConnectionState.ERROR
    override fun disconnectGmail() = OAuthConnectionState.DISCONNECTED
    override fun getGmailConnectionState() = OAuthConnectionState.DISCONNECTED
}

class UnconfiguredGoogleAccountConnection : GoogleAccountConnection {
    override fun connectGoogleAccount() = OAuthConnectionState.ERROR
    override fun disconnectGoogleAccount() = OAuthConnectionState.DISCONNECTED
    override fun getGoogleAccountConnectionState() = OAuthConnectionState.DISCONNECTED
}

fun OAuthConnectionState.toCapabilityStatus(): CapabilityStatus = when (this) {
    OAuthConnectionState.DISCONNECTED -> CapabilityStatus.CONNECT
    OAuthConnectionState.CONNECTING -> CapabilityStatus.ACTION_REQUIRED
    OAuthConnectionState.CONNECTED -> CapabilityStatus.CONNECTED
    OAuthConnectionState.ERROR -> CapabilityStatus.ACTION_REQUIRED
}
