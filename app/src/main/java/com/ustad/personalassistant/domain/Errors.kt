package com.ustad.personalassistant.domain

sealed interface UstadError {
    data object PermissionDenied : UstadError
    data object PermissionUnavailable : UstadError
    data object SettingsRequired : UstadError
    data object AuthenticationRequired : UstadError
    data object ServiceUnavailable : UstadError
    data object NetworkError : UstadError
    data object ConfigurationError : UstadError
    data class UnknownError(val message: String? = null) : UstadError
}

fun UstadError.userMessage(): String = when (this) {
    UstadError.PermissionDenied -> "Permission was denied. You can try again from Permission Center."
    UstadError.PermissionUnavailable -> "This capability is not available on this device."
    UstadError.SettingsRequired -> "Android system settings are required to enable this capability."
    UstadError.AuthenticationRequired -> "Device authentication is required before this action."
    UstadError.ServiceUnavailable -> "The required Android service is unavailable."
    UstadError.NetworkError -> "A network connection is required."
    UstadError.ConfigurationError -> "The assistant configuration is incomplete."
    is UstadError.UnknownError -> message ?: "Something went wrong. Please try again."
}
