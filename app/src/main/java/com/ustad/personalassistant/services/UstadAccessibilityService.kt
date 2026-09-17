package com.ustad.personalassistant.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class UstadAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Part 01 intentionally performs no app automation or gesture injection.
    }

    override fun onInterrupt() = Unit
}
