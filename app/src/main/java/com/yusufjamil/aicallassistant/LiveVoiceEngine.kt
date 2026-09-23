package com.yusufjamil.aicallassistant
import android.content.Context
import android.telecom.Call
object LiveVoiceEngine {
 @Volatile var state: VoiceEngineState=VoiceEngineState.IDLE; private set
 @Volatile var lastTranscript:String=""; private set
 @Volatile var lastResponse:String=""; private set
 fun startForCurrentCall(context:Context){CallSession.currentCall?.let{start(context,it)}}
 fun start(context:Context,call:Call){if(state==VoiceEngineState.RUNNING||state==VoiceEngineState.WAITING_FOR_AUDIO_BRIDGE)return;state=VoiceEngineState.WAITING_FOR_AUDIO_BRIDGE;lastTranscript="";lastResponse=""}
 fun submitCallerTextForPipeline(text:String){if(text.isBlank())return;lastTranscript=text.trim();state=VoiceEngineState.STT_RECEIVED}
 fun processCallerText(context:Context,text:String):String {
  if(text.isBlank())return "";submitCallerTextForPipeline(text)
  val name=AppSettings.name(context).ifBlank{"the owner"};val language=AppSettings.language(context).ifBlank{"English"}
  val prompt="You are a concise phone AI assistant for "+name+". Reply in "+language+". Be polite, ask one useful clarification when needed, never claim to have completed an action you cannot actually perform, and keep the reply short. Caller said: "+text.take(6000)
  val response=ProviderApiClient.chatWithFallback(context,prompt);if(response.isBlank()){state=VoiceEngineState.ERROR;return ""};state=VoiceEngineState.LLM_READY;submitAssistantResponse(response);return response
 }
 fun greeting(context:Context):String {val name=AppSettings.name(context).ifBlank{"the owner"};val language=AppSettings.language(context).lowercase();val female=AppSettings.voice(context).equals("Female",true);return if(language.startsWith("hindi")||language.startsWith("hinglish")) "Hello, main "+name+" ka AI Assistant "+if(female)"baat kar rahi hoon." else "baat kar raha hoon." else "Hello, I am "+name+"'s AI Assistant."}
 fun submitAssistantResponse(text:String){if(text.isBlank())return;lastResponse=text.trim();state=VoiceEngineState.TTS_READY}
 fun stop(){state=VoiceEngineState.IDLE}
}
enum class VoiceEngineState{IDLE,WAITING_FOR_AUDIO_BRIDGE,STT_RECEIVED,LLM_READY,TTS_READY,RUNNING,ERROR}