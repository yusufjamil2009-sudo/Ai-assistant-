package com.yusufjamil.aicallassistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.InCallService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AiInCallService : InCallService() {

    override fun onCreate() {
        super.onCreate()
        AiCallServiceHolder.service = this
    }

    override fun onDestroy() {
        if (AiCallServiceHolder.service === this) AiCallServiceHolder.service = null
        super.onDestroy()
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val sessions = mutableMapOf<Call, Runnable>()

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        CallSession.currentCall = call
        CallSession.startedAt = System.currentTimeMillis()
        CallSession.callerNumber = call.details?.handle?.let(::extractNumber)

        if (call.state == Call.STATE_RINGING) {
            createNotificationChannel()
            postIncomingNotification()

            val timeout = Runnable {
                if (call.state == Call.STATE_RINGING) {
                    call.answer(0)
                    CallSession.autoAnswered = true
                    CallSession.status = "AI HANDLING"
                    LiveVoiceEngine.startForCurrentCall(this)
                    postActiveNotification()
                }
            }

            sessions[call] = timeout
            mainHandler.postDelayed(timeout, AUTO_ANSWER_DELAY_MS)
        } else {
            CallSession.status = "CONNECTED"
            postActiveNotification()
        }
    }

    override fun onCallRemoved(call: Call) {
        sessions.remove(call)?.let(mainHandler::removeCallbacks)

        if (CallSession.currentCall === call) {
            LiveVoiceEngine.stop()
            val duration = ((System.currentTimeMillis() - CallSession.startedAt).coerceAtLeast(0L)) / 1000L
            CallHistoryStore(this).save(CallSummary(System.currentTimeMillis(), null, CallSession.callerNumber, false, "Call completed", "OTHER", LiveVoiceEngine.lastTranscript, LiveVoiceEngine.lastResponse, emptyList(), duration, CallSession.startedAt))
            CallSession.status = "ENDED"
            CallSession.currentCall = null
            CallSession.callerNumber = null
            CallSession.autoAnswered = false
            CallSession.userJoined = false
            postEndedNotification()
        }

        super.onCallRemoved(call)
    }

    fun answerNow() {
        val call = CallSession.currentCall ?: return
        sessions.remove(call)?.let(mainHandler::removeCallbacks)
        if (call.state == Call.STATE_RINGING) {
            call.answer(0)
            CallSession.status = "CONNECTED"
            postActiveNotification()
        }
    }

    fun joinCall() {
        val call = CallSession.currentCall ?: return
        if (call.state == Call.STATE_RINGING) {
            sessions.remove(call)?.let(mainHandler::removeCallbacks)
            call.answer(0)
        }
        CallSession.userJoined = true
        CallSession.status = "USER JOINED"
        LiveVoiceEngine.stop()
        postActiveNotification()
    }

    fun endCall() {
        CallSession.currentCall?.disconnect()
    }

    fun toggleMute() {
        setMuted(!CallSession.isMuted)
        CallSession.isMuted = !CallSession.isMuted
        postActiveNotification()
    }

    private fun extractNumber(handle: Uri): String? =
        handle.schemeSpecificPart?.takeIf { it.isNotBlank() }

    private fun postIncomingNotification() {
        val intent = Intent(this, IncomingCallActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 100, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val number = CallSession.callerNumber ?: "Unknown caller"
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle("Incoming call")
            .setContentText("$number • Auto-answer in 20 seconds")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    private fun postActiveNotification() {
        val intent = Intent(this, IncomingCallActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 100, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = when {
            CallSession.status == "AI HANDLING" -> "AI handling call • JOIN CALL available"
            CallSession.userJoined -> "You joined the call • END CALL available"
            else -> "Call active • JOIN CALL available"
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle("AI Call Assistant")
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    private fun postEndedNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle("Call ended")
            .setContentText("Call session closed.")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Call Assistant",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming and active call controls"
            }
        )
    }

    companion object {
        const val AUTO_ANSWER_DELAY_MS = 20_000L
        private const val CHANNEL_ID = "call_assistant"
        private const val NOTIFICATION_ID = 2001
    }
}

object CallSession {
    @Volatile var currentCall: Call? = null
    @Volatile var callerNumber: String? = null
    @Volatile var autoAnswered: Boolean = false
    @Volatile var userJoined: Boolean = false
    @Volatile var isMuted: Boolean = false
    @Volatile var status: String = "IDLE"
    @Volatile var startedAt: Long = 0L
}
