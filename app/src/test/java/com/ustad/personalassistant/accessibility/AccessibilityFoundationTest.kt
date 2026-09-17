package com.ustad.personalassistant.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

class AccessibilityFoundationTest {
    @Test fun disabledServiceHasExplicitStatus() { assertEquals(AccessibilityActionStatus.SERVICE_DISABLED, AccessibilityActionResult<Unit>(AccessibilityActionStatus.SERVICE_DISABLED).status) }
    @Test fun nodeMissingHasExplicitStatus() { assertEquals(AccessibilityActionStatus.NODE_NOT_FOUND, AccessibilityActionResult<Unit>(AccessibilityActionStatus.NODE_NOT_FOUND).status) }
    @Test fun timeoutHasExplicitStatus() { assertEquals(AccessibilityActionStatus.TIMEOUT, AccessibilityActionResult<Unit>(AccessibilityActionStatus.TIMEOUT).status) }
    @Test fun verificationFailureHasExplicitStatus() { assertEquals(AccessibilityActionStatus.VERIFICATION_FAILED, AccessibilityActionResult<Unit>(AccessibilityActionStatus.VERIFICATION_FAILED).status) }
}
