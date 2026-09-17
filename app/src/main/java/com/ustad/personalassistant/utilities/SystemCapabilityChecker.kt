package com.ustad.personalassistant.utilities

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

interface SystemCapabilityChecker {
    fun hasLaunchableApps(): Boolean
    fun canHandle(intent: Intent): Boolean
}

class AndroidSystemCapabilityChecker(private val context: Context) : SystemCapabilityChecker {
    override fun hasLaunchableApps(): Boolean =
        context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            PackageManager.MATCH_ALL
        ).isNotEmpty()

    override fun canHandle(intent: Intent): Boolean =
        intent.resolveActivity(context.packageManager) != null
}
