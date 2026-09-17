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
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { currentPackageName = event?.packageName?.toString() }
    override fun onInterrupt() = Unit
    override fun onDestroy() { if (active === this) active = null; currentPackageName = null; super.onDestroy() }
    fun targetAllowed(): Boolean = currentPackageName?.let { !protectedPolicy.isProtected(it) && !sensitiveScreen(rootInActiveWindow) } ?: false
    fun launchApp(packageName: String): Boolean = try { packageManager.getLaunchIntentForPackage(packageName)?.also { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } != null } catch (_: Exception) { false }
    fun nodeByText(text: String): AccessibilityNodeInfo? = rootInActiveWindow?.findAccessibilityNodeInfosByText(text)?.firstOrNull()
    fun focusedEditable(): AccessibilityNodeInfo? = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.takeIf { it.isEditable }
    fun firstEditable(): AccessibilityNodeInfo? = find(rootInActiveWindow) { it.isEditable }
    fun firstScrollable(): AccessibilityNodeInfo? = find(rootInActiveWindow) { it.isScrollable }
    fun button(text: String?): AccessibilityNodeInfo? = find(rootInActiveWindow) { node -> node.className?.toString()?.contains("Button", true) == true && (text == null || node.text?.toString()?.contains(text, true) == true || node.contentDescription?.toString()?.contains(text, true) == true) }
    fun visibleText(): List<String> = buildList { walk(rootInActiveWindow) { node -> node.text?.toString()?.takeIf { it.isNotBlank() }?.let(::add); node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let(::add) } }.distinct().take(200)
    fun snapshot(): ScreenSnapshot = ScreenSnapshot(currentPackageName, visibleText(), emptyList(), emptyList(), emptyList(), emptyList(), 0, null)
    private fun sensitiveScreen(root: AccessibilityNodeInfo?): Boolean { var blocked = false; walk(root) { node -> if (node.isPassword) blocked = true; val s = listOfNotNull(node.text?.toString(), node.contentDescription?.toString()).joinToString(" ").lowercase(); if (listOf("pin", "password", "passcode", "biometric", "fingerprint", "face unlock", "device unlock").any(s::contains)) blocked = true }; return blocked }
    private fun walk(node: AccessibilityNodeInfo?, visit: (AccessibilityNodeInfo) -> Unit) { if (node == null) return; visit(node); for (i in 0 until node.childCount) walk(node.getChild(i), visit) }
    private fun find(node: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? { if (node == null) return null; if (predicate(node)) return node; for (i in 0 until node.childCount) find(node.getChild(i), predicate)?.let { return it }; return null }
}
