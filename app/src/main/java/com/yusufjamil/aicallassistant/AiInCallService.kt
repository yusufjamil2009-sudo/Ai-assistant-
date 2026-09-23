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

    private val mainHandler = Handler(Looper.getMainLooper())
    private val sessions = mutableMapOf<Call, Runnable>()

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        if (call.state != Call.STATE_RINGING) return

        CallSession.currentCall = call
        CallSession.callerNumber = call.details?.handle?.let(::extractNumber)

        createNotificationChannel()
        postIncomingNotification()

        val timeout = Runnable {
            if (call.state == Call.STATE_RINGING) {
                call.answer(0)
                CallSession.autoAnswered = true
                postAnsweredNotification()
            }
        }

        sessions[call] = timeout
        mainHandler.postDelayed(timeout, AUTO_ANSWER_DELAY_MS)
    }

    override fun onCallRemoved(call: Call) {
        sessions.remove(call)?.let(mainHandler::removeCallbacks)

        if (CallSession.currentCall === call) {
            CallSession.currentCall = null
            CallSession.callerNumber = null
            CallSession.autoAnswered = false
            cancelNotification()
        }

        super.onCallRemoved(call)
    }

    private fun extractNumber(handle: Uri): String? =
        handle.schemeSpecificPart?.takeIf { it.isNotBlank() }

    private fun postIncomingNotification() {
        val intent = Intent(this, IncomingCallActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this,
            100,
            intent,
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

    private fun postAnsweredNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle("AI Call Assistant")
            .setContentText("Call answered automatically. Live AI voice is added in Part 4.")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    private fun cancelNotification() {
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
    }

    private fun createNotificationChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Incoming calls",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    companion object {
        const val AUTO_ANSWER_DELAY_MS = 20_000L
        private const val CHANNEL_ID = "incoming_calls"
        private const val NOTIFICATION_ID = 2001
    }
}

object CallSession {
    @Volatile var currentCall: Call? = null
    @Volatile var callerNumber: String? = null
    @Volatile var autoAnswered: Boolean = false
}
