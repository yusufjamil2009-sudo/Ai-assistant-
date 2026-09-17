package com.ustad.personalassistant.part08

import android.telecom.Call
import android.telecom.InCallService

class UstadCallInCallService : InCallService() {
    private val engine: CallAssistantEngine?
        get() = (application as? com.ustad.personalassistant.UstadApplication)?.callAssistantEngine
    override fun onCallAdded(call: Call) { super.onCallAdded(call); engine?.onCallAdded(call) }
    override fun onCallRemoved(call: Call) { engine?.finishCall(); super.onCallRemoved(call) }
    override fun onDestroy() { engine?.destroy(); super.onDestroy() }
}
