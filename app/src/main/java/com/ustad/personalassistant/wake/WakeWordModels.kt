package com.ustad.personalassistant.wake

enum class WakeWordState { IDLE, LISTENING_FOR_WAKE, WAKE_DETECTED, LISTENING_FOR_COMMAND, PROCESSING, SPEAKING, PAUSED, ERROR }
enum class WakeWordErrorCode { UNAVAILABLE, START_FAILED, DETECTION_FAILED, PERMISSION_REQUIRED, STOPPED }
data class WakeWordError(val code: WakeWordErrorCode, val userMessage: String, val causeMessage: String? = null)
data class WakeWordEvent(val phrase: String, val confidence: Float? = null)
