package com.ustad.personalassistant.accessibility

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.ustad.personalassistant.security.SecurityManager
import com.ustad.personalassistant.services.AdvancedAccessibilityService

interface AccessibilityActionEngine { fun openApp(packageName: String): AccessibilityActionResult<Unit>; fun findNode(text: String): AccessibilityActionResult<AccessibilityNodeInfo>; fun clickNode(text: String): AccessibilityActionResult<Unit>; fun setText(text: String): AccessibilityActionResult<Unit>; fun scrollForward(): AccessibilityActionResult<Unit>; fun scrollBackward(): AccessibilityActionResult<Unit>; fun pressBack(): AccessibilityActionResult<Unit>; fun readVisibleText(): AccessibilityActionResult<List<String>>; fun findEditableField(): AccessibilityActionResult<AccessibilityNodeInfo>; fun findButton(text: String? = null): AccessibilityActionResult<AccessibilityNodeInfo>; fun waitForNode(text: String, timeoutMs: Long = 3_000L): AccessibilityActionResult<AccessibilityNodeInfo>; fun verifyAction(expectedText: String? = null, timeoutMs: Long = 3_000L): AccessibilityActionResult<Unit>; fun snapshot(): AccessibilityActionResult<ScreenSnapshot> }

class AndroidAccessibilityActionEngine(private val serviceProvider: () -> AdvancedAccessibilityService?, private val securityManager: SecurityManager) : AccessibilityActionEngine {
    override fun openApp(packageName: String) = withService { service -> if (!authorized(packageName)) blocked() else if (service.launchApp(packageName)) success(Unit) else failure(AccessibilityActionStatus.APP_NOT_SUPPORTED) }
    override fun findNode(text: String) = withService { service -> service.nodeByText(text)?.let(::success) ?: failure(AccessibilityActionStatus.NODE_NOT_FOUND) }
    override fun clickNode(text: String) = withService { service -> if (!authorized(service.currentPackageName)) return@withService blocked(); val node = service.nodeByText(text) ?: return@withService failure(AccessibilityActionStatus.NODE_NOT_FOUND); if ((node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) || (node.parent?.let { p -> p.isClickable && p.performAction(AccessibilityNodeInfo.ACTION_CLICK) } == true)) success(Unit) else failure(AccessibilityActionStatus.ACTION_NOT_SUPPORTED) }
    override fun setText(text: String) = withService { service -> if (!authorized(service.currentPackageName)) return@withService blocked(); val node = service.focusedEditable() ?: service.firstEditable() ?: return@withService failure(AccessibilityActionStatus.NODE_NOT_FOUND); val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }; if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) success(Unit) else failure(AccessibilityActionStatus.ACTION_NOT_SUPPORTED) }
    override fun scrollForward() = scroll(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    override fun scrollBackward() = scroll(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    private fun scroll(action: Int) = withService { service -> if (!authorized(service.currentPackageName)) return@withService blocked(); val node = service.firstScrollable() ?: return@withService failure(AccessibilityActionStatus.NODE_NOT_FOUND); if (node.performAction(action)) success(Unit) else failure(AccessibilityActionStatus.ACTION_NOT_SUPPORTED) }
    override fun pressBack() = withService { service -> if (!authorized(service.currentPackageName)) return@withService blocked(); if (service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)) success(Unit) else failure(AccessibilityActionStatus.ACTION_NOT_SUPPORTED) }
    override fun readVisibleText() = withService { service -> if (!authorized(service.currentPackageName)) blocked() else success(service.visibleText()) }
    override fun findEditableField() = withService { service -> service.focusedEditable()?.let(::success) ?: service.firstEditable()?.let(::success) ?: failure(AccessibilityActionStatus.NODE_NOT_FOUND) }
    override fun findButton(text: String?) = withService { service -> service.button(text)?.let(::success) ?: failure(AccessibilityActionStatus.NODE_NOT_FOUND) }
    override fun waitForNode(text: String, timeoutMs: Long): AccessibilityActionResult<AccessibilityNodeInfo> { val end = System.currentTimeMillis() + timeoutMs.coerceIn(0L, 10_000L); while (System.currentTimeMillis() <= end) { val result = findNode(text); if (result.isSuccess) return result; Thread.sleep(100L) }; return failure(AccessibilityActionStatus.TIMEOUT) }
    override fun verifyAction(expectedText: String?, timeoutMs: Long) = if (expectedText == null) success(Unit) else if (waitForNode(expectedText, timeoutMs).isSuccess) success(Unit) else failure(AccessibilityActionStatus.VERIFICATION_FAILED)
    override fun snapshot() = withService { service -> if (!authorized(service.currentPackageName)) blocked() else success(service.snapshot()) }
    private fun authorized(packageName: String?): Boolean = !packageName.isNullOrBlank() && securityManager.isActionAuthorized("accessibility") && !securityManager.isProtectedApp(packageName)
    private fun <T> withService(block: (AdvancedAccessibilityService) -> AccessibilityActionResult<T>) = serviceProvider()?.let(block) ?: failure<T>(AccessibilityActionStatus.SERVICE_DISABLED)
    private fun <T> success(value: T) = AccessibilityActionResult(AccessibilityActionStatus.SUCCESS, value)
    private fun <T> blocked() = failure<T>(AccessibilityActionStatus.SECURITY_BLOCKED)
    private fun <T> failure(status: AccessibilityActionStatus) = AccessibilityActionResult<T>(status)
}
