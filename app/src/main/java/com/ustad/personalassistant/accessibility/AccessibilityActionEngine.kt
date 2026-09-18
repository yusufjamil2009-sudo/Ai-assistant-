package com.ustad.personalassistant.accessibility

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.ustad.personalassistant.security.SecurityManager
import com.ustad.personalassistant.services.AdvancedAccessibilityService

interface AccessibilityActionEngine {
    fun openApp(packageName: String): AccessibilityActionResult<Unit>
    fun findNode(text: String): AccessibilityActionResult<AccessibilityNodeInfo>
    fun findNodes(text: String): AccessibilityActionResult<List<AccessibilityNodeInfo>>
    fun findByText(text: String): AccessibilityActionResult<AccessibilityNodeInfo>
    fun findByContentDescription(text: String): AccessibilityActionResult<AccessibilityNodeInfo>
    fun findByViewId(viewId: String): AccessibilityActionResult<AccessibilityNodeInfo>
    fun clickNode(text: String): AccessibilityActionResult<Unit>
    fun longClickNode(text: String): AccessibilityActionResult<Unit>
    fun setText(text: String): AccessibilityActionResult<Unit>
    fun scrollForward(): AccessibilityActionResult<Unit>
    fun scrollBackward(): AccessibilityActionResult<Unit>
    fun pressBack(): AccessibilityActionResult<Unit>
    fun readVisibleText(): AccessibilityActionResult<List<String>>
    fun findEditableField(): AccessibilityActionResult<AccessibilityNodeInfo>
    fun findButton(text: String? = null): AccessibilityActionResult<AccessibilityNodeInfo>
    fun waitForNode(text: String, timeoutMs: Long = 3_000L): AccessibilityActionResult<AccessibilityNodeInfo>
    fun waitForWindow(packageName: String, timeoutMs: Long = 3_000L): AccessibilityActionResult<Unit>
    fun verifyAction(expectedText: String? = null, timeoutMs: Long = 3_000L): AccessibilityActionResult<Unit>
    fun snapshot(): AccessibilityActionResult<ScreenSnapshot>
    fun currentPackageName(): String?
    fun currentTargetAllowed(): Boolean
    fun cancelPendingOperations()
}

class AndroidAccessibilityActionEngine(private val serviceProvider: () -> AdvancedAccessibilityService?, private val securityManager: SecurityManager) : AccessibilityActionEngine {
    @Volatile private var cancelled = false
    override fun cancelPendingOperations() { cancelled = true }
    private fun begin() { cancelled = false }

    override fun openApp(packageName: String) = withService { service -> if (!authorized(packageName)) blocked() else if (service.launchApp(packageName)) success(Unit) else failure(AccessibilityActionStatus.APP_NOT_SUPPORTED) }
    override fun findNode(text: String) = withAuthorizedService { service -> service.nodeByText(text)?.let(::success) ?: failure(AccessibilityActionStatus.NODE_NOT_FOUND) }
    override fun findNodes(text: String) = withAuthorizedService { service -> val nodes = service.nodesByText(text); if (nodes.isEmpty()) failure(AccessibilityActionStatus.NODE_NOT_FOUND) else success(nodes) }
    override fun findByText(text: String) = findNode(text)
    override fun findByContentDescription(text: String) = withAuthorizedService { service -> service.nodeByContentDescription(text)?.let(::success) ?: failure(AccessibilityActionStatus.NODE_NOT_FOUND) }
    override fun findByViewId(viewId: String) = withAuthorizedService { service -> service.nodeByViewId(viewId)?.let(::success) ?: failure(AccessibilityActionStatus.NODE_NOT_FOUND) }
    override fun clickNode(text: String) = withService { service -> if (!authorized(service.currentPackageName)) return@withService blocked(); val node = service.nodeByText(text) ?: return@withService failure(AccessibilityActionStatus.NODE_NOT_FOUND); if ((node.isEnabled && node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) || (node.parent?.let { p -> p.isEnabled && p.isClickable && p.performAction(AccessibilityNodeInfo.ACTION_CLICK) } == true)) success(Unit) else failure(AccessibilityActionStatus.ACTION_NOT_SUPPORTED) }
    override fun longClickNode(text: String) = withService { service -> if (!authorized(service.currentPackageName)) return@withService blocked(); val node = service.nodeByText(text) ?: return@withService failure(AccessibilityActionStatus.NODE_NOT_FOUND); if (node.isEnabled && node.isLongClickable && node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)) success(Unit) else failure(AccessibilityActionStatus.ACTION_NOT_SUPPORTED) }
    override fun setText(text: String) = withService { service -> if (!authorized(service.currentPackageName)) return@withService blocked(); val node = service.focusedEditable() ?: service.firstEditable() ?: return@withService failure(AccessibilityActionStatus.NODE_NOT_FOUND); val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }; if (node.isEnabled && node.isEditable && node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) success(Unit) else failure(AccessibilityActionStatus.ACTION_NOT_SUPPORTED) }
    override fun scrollForward() = scroll(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    override fun scrollBackward() = scroll(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    private fun scroll(action: Int) = withService { service -> if (!authorized(service.currentPackageName)) return@withService blocked(); val node = service.firstScrollable() ?: return@withService failure(AccessibilityActionStatus.NODE_NOT_FOUND); if (node.performAction(action)) success(Unit) else failure(AccessibilityActionStatus.ACTION_NOT_SUPPORTED) }
    override fun pressBack() = withService { service -> if (!authorized(service.currentPackageName)) return@withService blocked(); if (service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)) success(Unit) else failure(AccessibilityActionStatus.ACTION_NOT_SUPPORTED) }
    override fun readVisibleText() = withService { service -> if (!authorized(service.currentPackageName)) blocked() else success(service.visibleText()) }
    override fun findEditableField() = withAuthorizedService { service -> service.focusedEditable()?.let(::success) ?: service.firstEditable()?.let(::success) ?: failure(AccessibilityActionStatus.NODE_NOT_FOUND) }
    override fun findButton(text: String?) = withAuthorizedService { service -> service.button(text)?.let(::success) ?: failure(AccessibilityActionStatus.NODE_NOT_FOUND) }
    override fun waitForNode(text: String, timeoutMs: Long): AccessibilityActionResult<AccessibilityNodeInfo> { begin(); val end = System.currentTimeMillis() + timeoutMs.coerceIn(0L, 10_000L); while (!cancelled && System.currentTimeMillis() <= end) { val result = findNode(text); if (result.isSuccess) return result; Thread.sleep(100L) }; return if (cancelled) failure(AccessibilityActionStatus.ERROR) else failure(AccessibilityActionStatus.TIMEOUT) }
    override fun waitForWindow(packageName: String, timeoutMs: Long): AccessibilityActionResult<Unit> { begin(); val end = System.currentTimeMillis() + timeoutMs.coerceIn(0L, 10_000L); while (!cancelled && System.currentTimeMillis() <= end) { val service = serviceProvider(); if (service?.currentPackageName == packageName) return success(Unit); Thread.sleep(100L) }; return if (cancelled) failure(AccessibilityActionStatus.ERROR) else failure(AccessibilityActionStatus.TIMEOUT) }
    override fun verifyAction(expectedText: String?, timeoutMs: Long) = if (expectedText == null) success(Unit) else if (waitForNode(expectedText, timeoutMs).isSuccess) success(Unit) else failure(AccessibilityActionStatus.VERIFICATION_FAILED)
    override fun snapshot() = withAuthorizedService { service -> success(service.snapshot()) }
    override fun currentPackageName(): String? = serviceProvider()?.currentPackageName
    override fun currentTargetAllowed(): Boolean = serviceProvider()?.targetAllowed() == true
    private fun authorized(packageName: String?): Boolean = !packageName.isNullOrBlank() && securityManager.isActionAuthorized("accessibility") && !securityManager.isProtectedApp(packageName)
    private fun <T> withService(block: (AdvancedAccessibilityService) -> AccessibilityActionResult<T>) = serviceProvider()?.let(block) ?: failure<T>(AccessibilityActionStatus.SERVICE_DISABLED)
    private fun <T> withAuthorizedService(block: (AdvancedAccessibilityService) -> AccessibilityActionResult<T>): AccessibilityActionResult<T> =
        withService { service -> if (!authorized(service.currentPackageName)) blocked() else block(service) }
    private fun <T> success(value: T) = AccessibilityActionResult(AccessibilityActionStatus.SUCCESS, value)
    private fun <T> blocked() = failure<T>(AccessibilityActionStatus.SECURITY_BLOCKED)
    private fun <T> failure(status: AccessibilityActionStatus) = AccessibilityActionResult<T>(status)
}
