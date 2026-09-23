package com.yusufjamil.aicallassistant

/**
 * Connection status for API providers.
 * Provides detailed states beyond simple connected/not-connected.
 */
enum class ConnectionStatus {
    NOT_CONFIGURED,
    TESTING,
    CONNECTED,
    INVALID_KEY,
    AUTHENTICATION_FAILED,
    RATE_LIMITED,
    QUOTA_EXCEEDED,
    NETWORK_ERROR,
    SERVER_ERROR,
    CONFIGURATION_ERROR,
    UNSUPPORTED,
    ERROR
}

/**
 * Result of an API test operation.
 */
data class ApiTestResult(
    val success: Boolean,
    val status: String,
    val detail: String
)

/**
 * Result of a speech-to-text operation.
 */
data class SpeechResult(
    val text: String,
    val confidence: Float,
    val providerId: String,
    val status: ConnectionStatus
)

/**
 * Result of a brain/LLM operation.
 */
data class BrainResult(
    val text: String,
    val providerId: String,
    val status: ConnectionStatus
)

/**
 * Result of a text-to-speech operation.
 */
data class TtsResult(
    val audio: ByteArray,
    val providerId: String,
    val status: ConnectionStatus
)
