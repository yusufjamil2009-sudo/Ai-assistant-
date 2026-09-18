package com.ustad.personalassistant.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ustad.personalassistant.R
import com.ustad.personalassistant.UstadApplication
import com.ustad.personalassistant.domain.CapabilityStatus
import com.ustad.personalassistant.permissions.Capability
import com.ustad.personalassistant.voice.VoiceSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BackgroundAssistantManager(private val context: Context) {
    private val _state = MutableStateFlow(BackgroundAssistantState.DISABLED)
    val state: StateFlow<BackgroundAssistantState> = _state.asStateFlow()
    fun setEnabled(enabled: Boolean): Result<Unit> {
        val app = context.applicationContext as UstadApplication
        if (!enabled) {
            context.stopService(Intent(context, BackgroundAssistantService::class.java))
            app.settingsRepository.setBackgroundAssistantEnabledBlocking(false)
            _state.value = BackgroundAssistantState.DISABLED
            return Result.success(Unit)
        }
        if (app.permissionManager.verifyPermission(Capability.MICROPHONE) != CapabilityStatus.ON) {
            _state.value = BackgroundAssistantState.MIC_PERMISSION_REQUIRED
            return Result.failure(IllegalStateException("Microphone permission required"))
        }
        _state.value = BackgroundAssistantState.STARTING
        return runCatching { ContextCompat.startForegroundService(context, Intent(context, BackgroundAssistantService::class.java)) }
            .onFailure { _state.value = BackgroundAssistantState.ERROR }
    }
    fun updateState(state: BackgroundAssistantState) { _state.value = state }
}

enum class BackgroundAssistantState { DISABLED, STARTING, ACTIVE, PAUSED, MIC_PERMISSION_REQUIRED, BATTERY_RESTRICTION, WAKE_ENGINE_UNAVAILABLE, ERROR }

class BackgroundAssistantService : Service() {
    private val channelId = "ustad_background_assistant"
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("USTAD Personal AI")
            .setContentText("Background assistant is active. Wake phrase: Hello Assistant")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(4105, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                @Suppress("DEPRECATION") startForeground(4105, notification)
            }
        } catch (_: SecurityException) {
            stopSelf()
            return
        }
        val app = application as UstadApplication
        val manager = app.backgroundAssistantManager
        if (app.permissionManager.verifyPermission(Capability.MICROPHONE) != CapabilityStatus.ON) {
            manager.updateState(BackgroundAssistantState.MIC_PERMISSION_REQUIRED); stopSelf(); return
        }
        manager.updateState(BackgroundAssistantState.ACTIVE)
        app.voiceSessionManager.startBackgroundWakeListening(this) { error ->
            manager.updateState(if (error.code.name == "UNAVAILABLE") BackgroundAssistantState.WAKE_ENGINE_UNAVAILABLE else BackgroundAssistantState.ERROR)
            stopSelf()
        }
    }
    override fun onDestroy() {
        val app = application as? UstadApplication
        app?.voiceSessionManager?.stopBackgroundWakeListening()
        app?.backgroundAssistantManager?.updateState(BackgroundAssistantState.DISABLED)
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Background Assistant", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
