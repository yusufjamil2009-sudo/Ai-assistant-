package com.ustad.personalassistant.accessibility

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.ustad.personalassistant.security.SecurityManager

interface AccessibilityActionEngine {
    fun openApp(packageName: String): AccessibilityActionResult<Unit>
    fun findNode(text: String): AccessibilityActionResult<AccessibilityNodeInfo>
    fun clickNode(text: String): AccessibilityActionResult<Unit>
    fun setText(text: String): AccessibilityActionResult<Unit>
    fun scrollForward(): AccessibilityActionResult<Unit>
    fun scrollBackward(): AccessibilityActionResult<Unit>
    fun pressBack(): AccessibilityActionResult<Unit>
    fun readVisibleText(): AccessibilityActionResult<List<String>>
    fun findEditableField(): AccessibilityActionResult<AccessibilityNodeInfo>
    fun findButton(text: String? = null): AccessibilityActionResult<AccessibilityNodeInfo>
    fun waitForNode(text: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): AccessibilityActionResult<AccessibilityNodeInfo>
    fun verifyAction(expectedText: String? = null, timeoutMs: Long = DEFAULT_TIMEOUT_MS): AccessibilityActionResult<Unit>
    fun snapshot(): AccessibilityActionResult<ScreenSnapshot>
    companion object { const val DEFAULT_TIMEOUT_MS = 3_000L }
}

class AndroidAccessibilityActionEngine(
    private val serviceProvider: () -> UstadAccessibilityService?,
    private val securityManager: SecurityManager
) : AccessibilityActionEngine {
    override fun openApp(packageName: String) = serviceProvider()?.let {
        if (!securityManager.isActionAuthorized("openApp:$packageName") || securityManager.isProtectedApp(packageName)) error(AccessibilityActionStatus.SECURITY_BLOCKED)
        if (it.openAppInternal(packageName)) success(Unit) else error(AccessibilityActionStatus.APP_NOT_SUPPORTED)
    } ?: error(AccessibilityActionStatus.SERVICE_DISABLED)

    override fun findNode(text: String) = withService { it.findNodeByText(text)?.let(::success) ?: error(AccessibilityActionStatus.NODE_NOT_FOUND) }

    override fun clickNode(text: String) = withService { service ->
        if (!securityManager.isActionAuthorized("clickNode") || !service.isTargetAllowed()) return@withService error(AccessibilityActionStatus.SECURITY_BLOCKED)
        val node = service.findNodeByText(text) ?: return@withService error(AccessibilityActionStatus.NODE_NOT_FOUND)
        if (clickSemantic(node)) success(Unit) else error(AccessibilityActionStatus.ACTION_NOT_SUPPORTED)
    }

    override fun setText(text: String) = withService { service ->
        if (!service.isTargetAllowed()) return@withService error(AccessibilityActionStatus.SECURITY_BLOCKED)
        val node = service.findFocusedEditable() ?: service.findFirstEditable() ?: return@withService error(AccessibilityActionStatus.NODE_NOT_FOUND)
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
        if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) success(Unit) else error(AccessibilityActionStatus.ACTION_NOT_SUPPORTED)
    }

    override fun scrollForward() = scroll(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    override fun scrollBackward() = scroll(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    private fun scroll(action: Int) = withService { service ->
        if (!service.isTargetAllowed()) return@withService error(AccessibilityActionStatus.SECURITY_BLOCKED)
        val node = service.findFirstScrollable() ?: return@withService error(AccessibilityActionStatus.NODE_NOT_FOUND)
        if (node.performAction(action)) success(Unit) else error(AccessibilityActionStatus.ACTION_NOT_SUPPORTED)
    }

    override fun pressBack() = withService { service ->
        if (!service.isTargetAllowed()) return@withService error(AccessibilityActionStatus.SECURITY_BLOCKED)
        if (service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)) success(Unit) else error(AccessibilityActionStatus.ACTION_NOT_SUPPORTED)
    }

    override fun readVisibleText() = withService { service ->
        if (!service.isTargetAllowed()) return@withService error(AccessibilityActionStatus.SECURITY_BLOCKED)
        success(service.collectVisibleText())
    }

    override fun findEditableField() = withService { service -> service.findFocusedEditable()?.let(::success) ?: service.findFirstEditable()?.let(::success) ?: error(AccessibilityActionStatus.NODE_NOT_FOUND) }
    override fun findButton(text: String?) = withService { service -> service.findButton(text)?.let(::success) ?: error(AccessibilityActionStatus.NODE_NOT_FOUND) }

    override fun waitForNode(text: String, timeoutMs: Long): AccessibilityActionResult<AccessibilityNodeInfo> {
        val end = System.currentTimeMillis() + timeoutMs.coerceIn(0L, 10_000L)
        while (System.currentTimeMillis() <= end) {
            val result = findNode(text)
            if (result.isSuccess) return result
            Thread.sleep(100L)
        }
        return error(AccessibilityActionStatus.TIMEOUT)
    }

    override fun verifyAction(expectedText: String?, timeoutMs: Long): AccessibilityActionResult<Unit> =
        if (expectedText == null) success(Unit) else if (waitForNode(expectedText, timeoutMs).isSuccess) success(Unit) else error(AccessibilityActionStatus.VERIFICATION_FAILED)

    override fun snapshot() = withService { service ->
        if (!service.isTargetAllowed()) return@withService error(AccessibilityActionStatus.SECURITY_BLOCKED)
        success(service.buildSnapshot())
    }

    private fun <T> withService(block: (UstadAccessibilityService) -> AccessibilityActionResult<T>): AccessibilityActionResult<T> = serviceProvider()?.let(block) ?: error(AccessibilityActionStatus.SERVICE_DISABLED)
    private fun clickSemantic(node: AccessibilityNodeInfo): Boolean = (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) || (node.parent?.let(::clickSemantic) == true)
    private fun <T> success(value: T) = AccessibilityActionResult(AccessibilityActionStatus.SUCCESS, value)
    private fun <T> error(status: AccessibilityActionStatus) = AccessibilityActionResult<T>(status)
}
