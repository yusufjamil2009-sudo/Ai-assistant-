package com.ustad.personalassistant.appcontrol

import android.content.Intent
import android.net.Uri

sealed interface AutomationStep {
    val timeoutMs: Long
    val expectedText: String?
    data class OpenApp(val query: String, override val timeoutMs: Long = 8_000L, override val expectedText: String? = null) : AutomationStep
    data class FindAndClick(val text: String, override val timeoutMs: Long = 5_000L, override val expectedText: String? = null) : AutomationStep
    data class SetText(val text: String, override val timeoutMs: Long = 5_000L, override val expectedText: String? = null) : AutomationStep
    data class Scroll(val forward: Boolean, val maxAttempts: Int = 5, override val timeoutMs: Long = 5_000L, override val expectedText: String? = null) : AutomationStep
    data class PressBack(override val timeoutMs: Long = 3_000L, override val expectedText: String? = null) : AutomationStep
    data class ReadVisibleText(override val timeoutMs: Long = 3_000L, override val expectedText: String? = null) : AutomationStep
}

data class AutomationPlan(val steps: List<AutomationStep>, val name: String = "automation") {
    init { require(steps.size <= 20) { "Automation plan exceeds safe step limit" } }
}

enum class AutomationEngineStatus { SUCCESS, APP_NOT_FOUND, AMBIGUOUS_APP, SERVICE_DISABLED, NODE_NOT_FOUND, ACTION_NOT_SUPPORTED, TIMEOUT, PERMISSION_REQUIRED, SECURITY_BLOCKED, VERIFICATION_FAILED, CANCELLED, ERROR }
data class AutomationEngineResult<T>(val status: AutomationEngineStatus, val value: T? = null, val message: String? = null, val completedSteps: Int = 0)

interface AppAutomationAdapter {
    fun supports(packageName: String): Boolean
    fun prepare(packageName: String): Result<Unit> = Result.success(Unit)
}

class GenericAndroidAppAdapter : AppAutomationAdapter {
    override fun supports(packageName: String): Boolean = packageName.isNotBlank()
}

interface PhotoFilePicker {
    fun pickImage(): Intent
    fun pickFile(mimeType: String = "*/*"): Intent
}

class AndroidPhotoFilePicker : PhotoFilePicker {
    override fun pickImage(): Intent = if (android.os.Build.VERSION.SDK_INT >= 33) Intent("android.provider.action.PICK_IMAGES") else Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE) }
    override fun pickFile(mimeType: String): Intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = mimeType; addCategory(Intent.CATEGORY_OPENABLE); putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false) }
}
