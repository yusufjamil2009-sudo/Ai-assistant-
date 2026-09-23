package com.yusufjamil.aicallassistant
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
class CallActionReceiver : BroadcastReceiver() {
 override fun onReceive(context: Context, intent: Intent) {
  when(intent.action){
   ACTION_ANSWER,ACTION_JOIN -> AiCallServiceHolder.service?.joinCall()
   ACTION_END -> AiCallServiceHolder.service?.endCall()
   ACTION_MUTE -> AiCallServiceHolder.service?.toggleMute()
   ACTION_LISTEN -> { CallSession.status="LISTENING"; AiCallServiceHolder.service?.refreshCallNotification() }
  }
 }
 companion object {
  const val ACTION_ANSWER="com.yusufjamil.aicallassistant.ANSWER"
  const val ACTION_JOIN="com.yusufjamil.aicallassistant.JOIN"
  const val ACTION_END="com.yusufjamil.aicallassistant.END"
  const val ACTION_MUTE="com.yusufjamil.aicallassistant.MUTE"
  const val ACTION_LISTEN="com.yusufjamil.aicallassistant.LISTEN"
 }
}