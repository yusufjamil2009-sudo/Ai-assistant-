package com.ustad.personalassistant.part08

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.TelecomManager
import android.app.role.RoleManager
import com.ustad.personalassistant.ai.AiBrain
import com.ustad.personalassistant.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class CallAssistantEngine(private val context: Context, private val settings: SettingsRepository, private val aiBrain: AiBrain) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default); private val handler = Handler(Looper.getMainLooper()); private val _state = MutableStateFlow(CallAssistantState.IDLE)
    val state: StateFlow<CallAssistantState> = _state
    @Volatile private var active: CallSession? = null; private var activeCall: Call? = null; private var timeoutRunnable: Runnable? = null
    fun capabilityStatus(): CallCapabilityStatus { val telecom = context.getSystemService(TelecomManager::class.java); val dialer = if (Build.VERSION.SDK_INT >= 29) context.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_DIALER) == true else false; val mic = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED; return CallCapabilityStatus(telecom != null, dialer && telecom != null, mic, false) }
    fun onCallAdded(call: Call) {
        if (active != null) return
        val id = UUID.randomUUID().toString(); val handle = call.details.handle?.schemeSpecificPart; activeCall = call; active = CallSession(id, handle, null, System.currentTimeMillis()); _state.value = CallAssistantState.INCOMING_CALL; call.registerCallback(callback)
        if (!settings.callAssistantEnabledSnapshot()) { _state.value = CallAssistantState.RINGING; return }
        if (!capabilityStatus().autoAnswerAvailable) { _state.value = CallAssistantState.FAILED; return }
        val timeout = settings.callAssistantTimeoutSnapshot().coerceIn(5, 120) * 1000L; _state.value = CallAssistantState.WAITING_FOR_TIMEOUT; timeoutRunnable = Runnable { if (activeCall === call && call.state == Call.STATE_RINGING && settings.callAssistantEnabledSnapshot()) attemptAnswer(call) }; handler.postDelayed(timeoutRunnable!!, timeout)
    }
    private fun attemptAnswer(call: Call) { if (!capabilityStatus().autoAnswerAvailable) { _state.value = CallAssistantState.FAILED; return }; _state.value = CallAssistantState.ANSWERING; call.answer(0) }
    private val callback = object : Call.Callback() { override fun onStateChanged(call: Call, state: Int) { if (call !== activeCall) return; when (state) { Call.STATE_ACTIVE -> { active?.answeredAtMs = System.currentTimeMillis(); _state.value = CallAssistantState.CALL_CONVERSATION }; Call.STATE_DISCONNECTED -> finishCall(call); Call.STATE_RINGING -> if (_state.value == CallAssistantState.INCOMING_CALL) _state.value = CallAssistantState.RINGING } } }
    fun markConversationUnavailable() { active?.conversationAvailable = false }
    fun finishCall(call: Call? = null) {
        if (call != null && call !== activeCall) return
        timeoutRunnable?.let(handler::removeCallbacks); timeoutRunnable = null; activeCall?.unregisterCallback(callback); val session = active ?: return; session.endedAtMs = System.currentTimeMillis(); activeCall = null; active = null; _state.value = CallAssistantState.CALL_ENDED
        if (!settings.callAssistantSummarySnapshot()) { _state.value = CallAssistantState.IDLE; return }
        _state.value = CallAssistantState.SUMMARY_PROCESSING; scope.launch { val duration = (session.endedAtMs ?: session.startedAtMs) - (session.answeredAtMs ?: session.startedAtMs); val text = if (!session.conversationAvailable) "Call details available hain, lekin conversation summary available nahi hai." else aiBrain.summarize(com.ustad.personalassistant.ai.AiRequest("Summarize only these call details. Do not invent conversation content. Caller: ${session.displayName ?: session.number ?: "Unknown"}. Duration ms: $duration. Purpose: ${session.purpose ?: "unknown"}. Callback: ${session.callbackRequest ?: "none"}.", setOf(com.ustad.personalassistant.ai.AiCapability.SUMMARIZATION, com.ustad.personalassistant.ai.AiCapability.TEXT_GENERATION))).getOrNull()?.text ?: "Call details available hain, lekin conversation summary available nahi hai."; _state.value = CallAssistantState.SUMMARY_READY }
    }
    fun destroy() { timeoutRunnable?.let(handler::removeCallbacks); timeoutRunnable = null; activeCall?.unregisterCallback(callback); activeCall = null; active = null; scope.cancel(); _state.value = CallAssistantState.IDLE }
}
private fun SettingsRepository.callAssistantEnabledSnapshot(): Boolean = kotlinx.coroutines.runBlocking { callAssistantEnabled.first() }
private fun SettingsRepository.callAssistantTimeoutSnapshot(): Int = kotlinx.coroutines.runBlocking { callAssistantTimeoutSeconds.first() }
private fun SettingsRepository.callAssistantSummarySnapshot(): Boolean = kotlinx.coroutines.runBlocking { callAssistantPostCallSummary.first() }
