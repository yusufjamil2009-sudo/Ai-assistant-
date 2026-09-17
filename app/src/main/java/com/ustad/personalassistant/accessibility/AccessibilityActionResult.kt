package com.ustad.personalassistant.accessibility

enum class AccessibilityActionStatus {
    SUCCESS,
    NODE_NOT_FOUND,
    ACTION_NOT_SUPPORTED,
    SERVICE_DISABLED,
    APP_NOT_SUPPORTED,
    TIMEOUT,
    SECURITY_BLOCKED,
    PERMISSION_REQUIRED,
    VERIFICATION_FAILED,
    ERROR
}

data class AccessibilityActionResult<T>(
    val status: AccessibilityActionStatus,
    val value: T? = null,
    val message: String? = null,
    val durationMs: Long = 0L
) {
    val isSuccess: Boolean get() = status == AccessibilityActionStatus.SUCCESS
}
