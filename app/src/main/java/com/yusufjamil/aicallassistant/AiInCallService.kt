package com.yusufjamil.aicallassistant

import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallEndpoint
import android.telecom.InCallService
import java.util.concurrent.Executors
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * AI In-Call Service - handles incoming calls and manages the call lifecycle.
 * This service implements the Android InCallService API.
 */
class AiInCallService : InCallService() {
    private val h = Handler(Looper.getMainLooper())
    private val timers = mutableMapOf<Call, Runnable>()
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate() {
        super.onCreate()
        AiCallServiceHolder.service = this
        channel()
    }

    override fun onDestroy() {
        timers.values.forEach(h::removeCallbacks)
        timers.clear()
        analysisExecutor.shutdownNow()
        if (AiCallServiceHolder.service === this) AiCallServiceHolder.service = null
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallSession.currentCall = call
        CallSession.startedAt = System.currentTimeMillis()
        CallSession.callerNumber = call.details?.handle?.let { it.schemeSpecificPart }
        CallSession.callerName = findName(CallSession.callerNumber)
        CallSession.savedContact = CallSession.callerName != null
        CallSession.autoAnswered = false
        CallSession.userJoined = false
        CallSession.isMuted = false
        CallSession.endpointType = "UNKNOWN"

        if (call.state == Call.STATE_RINGING) {
            CallSession.status = "RINGING"
            incoming()
            val r = Runnable {
                if (CallSession.currentCall === call && call.state == Call.STATE_RINGING) {
                    answer(call, true)
                }
            }
            timers[call] = r
            h.postDelayed(r, 20_000)
        } else {
            CallSession.status = "CONNECTED"
            startAi()
            postActiveNotification()
        }
    }

    override fun onCallRemoved(call: Call) {
        timers.remove(call)?.let(h::removeCallbacks)
        if (CallSession.currentCall === call) {
            LiveVoiceEngine.stop()

            // Capture the session before doing any network/JSON work. InCallService callbacks
            // run on the main thread; provider calls must never block that thread.
            val appContext = applicationContext
            val id = System.currentTimeMillis()
            val callerName = CallSession.callerName
            val callerNumber = CallSession.callerNumber
            val savedContact = CallSession.savedContact
            val startedAt = CallSession.startedAt
            val callerText = LiveVoiceEngine.lastTranscript
            val assistantText = LiveVoiceEngine.lastResponse
            val durationSeconds =
                ((System.currentTimeMillis() - startedAt).coerceAtLeast(0L)) / 1000L

            CallSession.reset()
            ended()

            analysisExecutor.execute {
                val intelligence = CallIntelligenceEngine.analyzeWithAi(
                    appContext,
                    callerNumber,
                    callerName,
                    savedContact,
                    callerText,
                    assistantText
                )
                val summary = CallSummary(
                    id,
                    callerName,
                    callerNumber,
                    savedContact,
                    intelligence.purpose,
                    intelligence.category.name,
                    intelligence.callerSaid,
                    intelligence.assistantSaid,
                    intelligence.importantPoints,
                    durationSeconds,
                    startedAt
                )
                CallHistoryStore(appContext).save(summary)

                // The summary UI is launched only after persistence/analysis completes.
                runOnMainThread {
                    startActivity(
                        Intent(this, CallSummaryActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            putExtra(CallSummaryActivity.EXTRA_ID, summary.id)
                        }
                    )
                }
            }
        }
        super.onCallRemoved(call)
    }

    private fun runOnMainThread(action: () -> Unit) {
        h.post(action)
    }

    override fun onAvailableCallEndpointsChanged(endpoints: MutableList<CallEndpoint>) {
        super.onAvailableCallEndpointsChanged(endpoints)
        CallSession.availableEndpointTypes = endpoints.map { endpointName(it.endpointType) }
    }

    override fun onCallEndpointChanged(endpoint: CallEndpoint) {
        super.onCallEndpointChanged(endpoint)
        CallSession.endpointType = endpointName(endpoint.endpointType)
        postActiveNotification()
    }

    override fun onMuteStateChanged(isMuted: Boolean) {
        super.onMuteStateChanged(isMuted)
        CallSession.isMuted = isMuted
        if (CallSession.currentCall != null) postActiveNotification()
    }

    fun answerNow() {
        CallSession.currentCall?.let { answer(it, false) }
    }

    private fun answer(c: Call, automatic: Boolean) {
        timers.remove(c)?.let(h::removeCallbacks)
        if (c.state != Call.STATE_RINGING) return
        c.answer(0)
        CallSession.autoAnswered = automatic
        CallSession.status = if (automatic) "AI HANDLING" else "CONNECTED"
        startAi()
        postActiveNotification()
    }

    private fun startAi() {
        if (!CallSession.userJoined) LiveVoiceEngine.startForCurrentCall(this)
    }

    fun joinCall() {
        val c = CallSession.currentCall ?: return
        if (c.state == Call.STATE_RINGING) answer(c, false)
        CallSession.userJoined = true
        CallSession.status = "USER JOINED"
        LiveVoiceEngine.stop()
        postActiveNotification()
    }

    fun endCall() {
        CallSession.currentCall?.disconnect()
    }

    fun toggleMute() {
        val n = !CallSession.isMuted
        setMuted(n)
        CallSession.isMuted = n
        postActiveNotification()
    }

    fun refreshCallNotification() {
        if (CallSession.status == "RINGING") incoming() else postActiveNotification()
    }

    private fun findName(n: String?): String? {
        if (n.isNullOrBlank()) return null
        val u = Uri.withAppendedPath(
            android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(n)
        )
        return contentResolver.query(
            u,
            arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { if (it.moveToFirst()) it.getString(0) else null }
    }

    private fun endpointName(type: Int): String = when (type) {
        CallEndpoint.TYPE_BLUETOOTH -> "BLUETOOTH"
        CallEndpoint.TYPE_EARPIECE -> "EARPIECE"
        CallEndpoint.TYPE_SPEAKER -> "SPEAKER"
        CallEndpoint.TYPE_STREAMING -> "STREAMING"
        CallEndpoint.TYPE_WIRED_HEADSET -> "WIRED_HEADSET"
        else -> "UNKNOWN"
    }

    private fun p(action: String, id: Int) = PendingIntent.getBroadcast(
        this,
        id,
        Intent(this, CallActionReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun content() = PendingIntent.getActivity(
        this,
        100,
        Intent(this, IncomingCallActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun incoming() {
        val callerDisplay = CallSession.callerName ?: CallSession.callerNumber ?: "Unknown"
        val b = NotificationCompat.Builder(this, CH)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle("Incoming call")
            .setContentText("$callerDisplay \u2022 Auto-answer in 20 seconds")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setContentIntent(content())
            .addAction(android.R.drawable.sym_action_call, "ANSWER", p(CallActionReceiver.ACTION_ANSWER, 101))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "DECLINE", p(CallActionReceiver.ACTION_END, 102))
        NotificationManagerCompat.from(this).notify(ID, b.build())
    }

    fun postActiveNotification() {
        val callerDisplay = CallSession.callerName ?: CallSession.callerNumber ?: "Unknown"
        val b = NotificationCompat.Builder(this, CH)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle("AI Call Assistant")
            .setContentText(
                (if (CallSession.userJoined) "You joined the call" else "AI handling call") + " \u2022 " + CallSession.endpointType
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(content())

        if (!CallSession.userJoined) {
            b.addAction(android.R.drawable.sym_action_call, "JOIN CALL", p(CallActionReceiver.ACTION_JOIN, 103))
            b.addAction(android.R.drawable.ic_btn_speak_now, "LISTEN", p(CallActionReceiver.ACTION_LISTEN, 104))
        }
        b.addAction(
            android.R.drawable.ic_lock_silent_mode,
            if (CallSession.isMuted) "UNMUTE" else "MUTE",
            p(CallActionReceiver.ACTION_MUTE, 105)
        )
        b.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "END CALL",
            p(CallActionReceiver.ACTION_END, 106)
        )
        NotificationManagerCompat.from(this).notify(ID, b.build())
    }

    private fun ended() {
        NotificationManagerCompat.from(this).notify(
            ID,
            NotificationCompat.Builder(this, CH)
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle("Call ended")
                .setAutoCancel(true)
                .build()
        )
    }

    private fun channel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CH, "Call Assistant", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    companion object {
        private const val CH = "call_assistant"
        private const val ID = 2001
    }
}

/**
 * Call session state - shared state for the current call.
 */
object CallSession {
    @Volatile var currentCall: Call? = null
    @Volatile var callerNumber: String? = null
    @Volatile var callerName: String? = null
    @Volatile var savedContact = false
    @Volatile var autoAnswered = false
    @Volatile var userJoined = false
    @Volatile var isMuted = false
    @Volatile var status = "IDLE"
    @Volatile var startedAt = 0L
    @Volatile var endpointType = "UNKNOWN"
    @Volatile var availableEndpointTypes: List<String> = emptyList()

    fun reset() {
        currentCall = null
        callerNumber = null
        callerName = null
        savedContact = false
        autoAnswered = false
        userJoined = false
        isMuted = false
        status = "IDLE"
        startedAt = 0L
        endpointType = "UNKNOWN"
        availableEndpointTypes = emptyList()
    }
}
