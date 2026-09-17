package com.ustad.personalassistant.background

import android.content.Context
import android.os.PowerManager

interface BackgroundAssistantCapability {
    fun foregroundServiceSupported(): Boolean
    fun batteryOptimizationRelevant(): Boolean
}

class AndroidBackgroundAssistantCapability(private val context: Context) : BackgroundAssistantCapability {
    override fun foregroundServiceSupported(): Boolean = true

    override fun batteryOptimizationRelevant(): Boolean {
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return !power.isIgnoringBatteryOptimizations(context.packageName)
    }
}
