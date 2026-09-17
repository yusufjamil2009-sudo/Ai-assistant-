package com.ustad.personalassistant.domain

sealed interface UstadError {
    data object PermissionDenied : UstadError
    data object PermissionPermanentlyDenied : UstadError
    data object PermissionUnavailable : UstadError
    data object SettingsRequired : UstadError
    data object AuthenticationRequired : UstadError
    data object ServiceDisabled : UstadError
    data object ServiceUnavailable : UstadError
    data object OAuthRequired : UstadError
    data object OAuthFailed : UstadError
    data object CapabilityUnavailable : UstadError
    data object SecurityBlocked : UstadError
    data object NetworkError : UstadError
    data object ConfigurationError : UstadError
    data class UnknownError(val message: String? = null) : UstadError
}

fun UstadError.userMessage(): String = when (this) {
    UstadError.PermissionDenied -> "Permission was denied. You can try again from Permission Center."
    UstadError.PermissionPermanentlyDenied -> "Permission is blocked. Open this app's Android settings to enable it."
    UstadError.PermissionUnavailable -> "This capability is not available on this device."
    UstadError.SettingsRequired -> "Android system settings are required to enable this capability."
    UstadError.AuthenticationRequired -> "Device authentication is required before this action."
    UstadError.ServiceDisabled -> "The required Android service is disabled."
    UstadError.ServiceUnavailable -> "The required Android service is unavailable."
    UstadError.OAuthRequired -> "A Google connection is required before this action."
    UstadError.OAuthFailed -> "The Google connection could not be completed."
    UstadError.CapabilityUnavailable -> "The required capability is currently unavailable."
    UstadError.SecurityBlocked -> "This action is blocked by the assistant security policy."
    UstadError.NetworkError -> "A network connection is required."
    UstadError.ConfigurationError -> "The assistant configuration is incomplete."
    is UstadError.UnknownError -> message ?: "Something went wrong. Please try again."
}
