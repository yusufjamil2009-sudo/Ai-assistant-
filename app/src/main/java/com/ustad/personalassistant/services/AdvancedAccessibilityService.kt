package com.ustad.personalassistant.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.ustad.personalassistant.accessibility.ScreenSnapshot
import com.ustad.personalassistant.security.DefaultProtectedAppPolicy

open class AdvancedAccessibilityService : AccessibilityService() {
    companion object { @Volatile var active: AdvancedAccessibilityService? = null }
    private val protectedPolicy = DefaultProtectedAppPolicy()
    @Volatile var currentPackageName: String? = null
        private set
    override fun onServiceConnected() { super.onServiceConnected(); active = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { currentPackageName = event?.packageName?.toString() ?: currentPackageName }
    override fun onInterrupt() = Unit
    override fun onDestroy() { if (active === this) active = null; currentPackageName = null; super.onDestroy() }
    fun targetAllowed(): Boolean = currentPackageName?.let { !protectedPolicy.isProtected(it) && !sensitiveScreen(rootInActiveWindow) } ?: false
    fun launchApp(packageName: String): Boolean = try { packageManager.getLaunchIntentForPackage(packageName)?.also { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } != null } catch (_: Exception) { false }
    fun nodeByText(text: String): AccessibilityNodeInfo? = nodesByText(text).firstOrNull()
    fun nodesByText(text: String): List<AccessibilityNodeInfo> = rootInActiveWindow?.findAccessibilityNodeInfosByText(text)?.filter { it.isVisibleToUser }.orEmpty()
    fun nodeByContentDescription(text: String): AccessibilityNodeInfo? = find(rootInActiveWindow) { it.isVisibleToUser && it.contentDescription?.toString()?.equals(text, true) == true }
    fun nodeByViewId(viewId: String): AccessibilityNodeInfo? = runCatching { rootInActiveWindow?.findAccessibilityNodeInfosByViewId(viewId)?.firstOrNull { it.isVisibleToUser } }.getOrNull()
    fun focusedEditable(): AccessibilityNodeInfo? = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.takeIf { it.isEditable && it.isVisibleToUser }
    fun firstEditable(): AccessibilityNodeInfo? = find(rootInActiveWindow) { it.isEditable && it.isVisibleToUser }
    fun firstScrollable(): AccessibilityNodeInfo? = find(rootInActiveWindow) { it.isScrollable && it.isVisibleToUser }
    fun button(text: String?): AccessibilityNodeInfo? = find(rootInActiveWindow) { node -> node.isVisibleToUser && node.className?.toString()?.contains("Button", true) == true && (text == null || node.text?.toString()?.contains(text, true) == true || node.contentDescription?.toString()?.contains(text, true) == true) }
    fun visibleText(): List<String> = buildList { walk(rootInActiveWindow) { node -> if (!node.isVisibleToUser) return@walk; node.text?.toString()?.takeIf { it.isNotBlank() }?.let(::add); node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let(::add) } }.distinct().take(200)
    fun snapshot(): ScreenSnapshot = buildSnapshot()
    private fun buildSnapshot(): ScreenSnapshot {
        val clickable = mutableListOf<String>(); val editable = mutableListOf<String>(); val buttons = mutableListOf<String>(); val descriptions = mutableListOf<String>(); var scrollable = 0; var focused: String? = null
        walk(rootInActiveWindow) { node ->
            if (!node.isVisibleToUser) return@walk
            val label = node.text?.toString()?.takeIf { it.isNotBlank() } ?: node.contentDescription?.toString()?.takeIf { it.isNotBlank() } ?: node.viewIdResourceName
            if (node.contentDescription?.toString()?.isNotBlank() == true) descriptions += node.contentDescription.toString()
            if (node.isClickable && label != null) clickable += label
            if (node.isEditable && label != null) editable += label
            if (node.className?.toString()?.contains("Button", true) == true && label != null) buttons += label
            if (node.isScrollable) scrollable++
            if (node.isFocused) focused = label
        }
        return ScreenSnapshot(currentPackageName, visibleText(), descriptions.distinct().take(100), clickable.distinct().take(100), editable.distinct().take(50), buttons.distinct().take(100), scrollable, focused)
    }
    private fun sensitiveScreen(root: AccessibilityNodeInfo?): Boolean { var blocked = false; walk(root) { node -> if (node.isPassword) blocked = true; val s = listOfNotNull(node.text?.toString(), node.contentDescription?.toString()).joinToString(" ").lowercase(); if (listOf("pin", "password", "passcode", "biometric", "fingerprint", "face unlock", "device unlock").any(s::contains)) blocked = true }; return blocked }
    private fun walk(node: AccessibilityNodeInfo?, visit: (AccessibilityNodeInfo) -> Unit) { if (node == null) return; visit(node); for (i in 0 until node.childCount) node.getChild(i)?.let { walk(it, visit) } }
    private fun find(node: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? { if (node == null) return null; if (predicate(node)) return node; for (i in 0 until node.childCount) find(node.getChild(i), predicate)?.let { return it }; return null }
}
