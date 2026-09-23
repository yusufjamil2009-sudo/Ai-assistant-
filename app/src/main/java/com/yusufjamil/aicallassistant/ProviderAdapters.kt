package com.yusufjamil.aicallassistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

interface ProviderAdapter { val id:String; fun test(context:Context):ApiTestResult; fun chat(context:Context,prompt:String):String="" }

object ProviderAdapters {
 private val all=listOf(
  GroqAdapter,GeminiAdapter,SambaNovaAdapter,ZhipuAdapter,MistralAdapter,OpenRouterAdapter,
  DeepgramAdapter,GoogleSttAdapter,AssemblyAiAdapter,ElevenLabsScribeAdapter,GroqWhisperAdapter,MistralVoxtralAdapter,OpenAiWhisperAdapter,
  ElevenLabsTtsAdapter,GoogleTtsAdapter,AzureSpeechAdapter,AmazonPollyAdapter,FishAudioAdapter,CartesiaAdapter,RimeAdapter)
 private val map=all.associateBy{it.id}
 fun test(c:Context,id:String)=map[id]?.test(c)?:ApiTestResult(false,"Unknown","Adapter not found")
 fun chat(c:Context,id:String,p:String)=map[id]?.chat(c,p).orEmpty()
}

private fun k(c:Context,id:String)=SecureApiKeyStore.read(c,id)
private fun status(n:Int)=when(n){in 200..299->"Connected";401,403->"Invalid";402->"Billing Required";404->"Not Found";408,504->"Timeout";429->"Rate Limit/Quota";else->"HTTP $n"}

private fun req(url:String,key:String,method:String="GET",body:String?=null,header:String="Authorization"):ApiTestResult=try{
 val c=URL(url).openConnection() as HttpURLConnection
 c.requestMethod=method;c.connectTimeout=15000;c.readTimeout=30000;c.setRequestProperty("Accept","application/json")
 if(key.isNotBlank())c.setRequestProperty(header,if(header=="Authorization")"Bearer $key" else key)
 if(body!=null){c.doOutput=true;c.setRequestProperty("Content-Type","application/json");OutputStreamWriter(c.outputStream).use{it.write(body)}}
 val n=c.responseCode;c.disconnect();ApiTestResult(n in 200..299,status(n),"HTTP $n")
}catch(e:Exception){ApiTestResult(false,"Network Error",e.message?:"Request failed")}

private fun chatAdapter(pid:String,url:String,model:String)=object:ProviderAdapter{
 override val id=pid
 override fun test(c:Context):ApiTestResult{val key=k(c,id)?:return ApiTestResult(false,"Not Connected","API key is not configured");return try{val b=JSONObject().put("model",model).put("messages",JSONArray().put(JSONObject().put("role","user").put("content","Reply only OK"))).put("max_completion_tokens",8);val x=post(url,key,b);if(x.contains("choices"))ApiTestResult(true,"Connected","Authenticated")else ApiTestResult(false,"Unexpected Response","Provider returned no choices")}catch(e:Exception){ApiTestResult(false,"Error",e.message?:"Request failed")}}
 override fun chat(c:Context,p:String):String{val key=k(c,id)?:return "";return try{val b=JSONObject().put("model",model).put("messages",JSONArray().put(JSONObject().put("role","user").put("content",p)));JSONObject(post(url,key,b)).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty()}catch(_:Exception){""}}
}

private fun post(url:String,key:String,b:JSONObject):String{
 val c=URL(url).openConnection() as HttpURLConnection;c.requestMethod="POST";c.doOutput=true;c.connectTimeout=15000;c.readTimeout=60000;c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Authorization","Bearer $key");OutputStreamWriter(c.outputStream).use{it.write(b.toString())};val n=c.responseCode;val s=if(n in 200..299)c.inputStream else c.errorStream;val t=s?.bufferedReader()?.use{it.readText()}.orEmpty();c.disconnect();if(n !in 200..299)throw IllegalStateException("HTTP $n");return t
}

private val GroqAdapter=chatAdapter("groq","https://api.groq.com/openai/v1/chat/completions","llama-3.3-70b-versatile")
private val SambaNovaAdapter=chatAdapter("sambanova","https://api.sambanova.ai/v1/chat/completions","Meta-Llama-3.1-405B-Instruct")
private val ZhipuAdapter=chatAdapter("zhipu","https://api.z.ai/api/paas/v4/chat/completions","glm-4.6")
private val MistralAdapter=chatAdapter("mistral","https://api.mistral.ai/v1/chat/completions","mistral-small-latest")
private val OpenRouterAdapter=chatAdapter("openrouter","https://openrouter.ai/api/v1/chat/completions","openai/gpt-oss-20b")

private val GeminiAdapter=object:ProviderAdapter{
 override val id="gemini"
 override fun test(c:Context):ApiTestResult{val key=k(c,id)?:return ApiTestResult(false,"Not Connected","API key is not configured");return try{val t=gemini(key,"Reply only OK");if(t.isNotBlank())ApiTestResult(true,"Connected","Authenticated")else ApiTestResult(false,"Unexpected Response","No text returned")}catch(e:Exception){ApiTestResult(false,"Error",e.message?:"Request failed")}}
 override fun chat(c:Context,p:String)=try{k(c,id)?.let{gemini(it,p)}.orEmpty()}catch(_:Exception){""}
}
private fun gemini(key:String,p:String):String{
 val u="https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="+java.net.URLEncoder.encode(key,"UTF-8")
 val b=JSONObject().put("contents",JSONArray().put(JSONObject().put("parts",JSONArray().put(JSONObject().put("text",p)))))
 val c=URL(u).openConnection() as HttpURLConnection;c.requestMethod="POST";c.doOutput=true;c.setRequestProperty("Content-Type","application/json");OutputStreamWriter(c.outputStream).use{it.write(b.toString())};val n=c.responseCode;val s=if(n in 200..299)c.inputStream else c.errorStream;val t=s?.bufferedReader()?.use{it.readText()}.orEmpty();c.disconnect();if(n !in 200..299)throw IllegalStateException("HTTP $n");return JSONObject(t).optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text").orEmpty()
}

private val DeepgramAdapter=object:ProviderAdapter{override val id="deepgram";override fun test(c:Context)=k(c,id)?.let{req("https://api.deepgram.com/v1/projects",it,header="Authorization").let{r->if(r.success)r else r}}?:ApiTestResult(false,"Not Connected","API key is not configured")}
private val GoogleSttAdapter=object:ProviderAdapter{
 override val id="google_stt"
 override fun test(c:Context)=k(c,id)?.let{key->
  val pcm=ByteArray(3200)
  val audio=android.util.Base64.encodeToString(pcm,android.util.Base64.NO_WRAP)
  req("https://speech.googleapis.com/v1/speech:recognize?key="+java.net.URLEncoder.encode(key,"UTF-8"),"","POST",
   JSONObject().put("config",JSONObject().put("encoding","LINEAR16").put("sampleRateHertz",16000).put("languageCode","en-US")).put("audio",JSONObject().put("content",audio)).toString())
 }?:ApiTestResult(false,"Not Connected","API key is not configured")
}
private val AssemblyAiAdapter=object:ProviderAdapter{override val id="assemblyai";override fun test(c:Context)=k(c,id)?.let{req("https://api.assemblyai.com/v2/transcript",it)}?:ApiTestResult(false,"Not Connected","API key is not configured")}
private val ElevenLabsScribeAdapter=object:ProviderAdapter{override val id="elevenlabs_scribe";override fun test(c:Context)=k(c,id)?.let{req("https://api.elevenlabs.io/v1/models",it,header="xi-api-key")}?:ApiTestResult(false,"Not Connected","API key is not configured")}
private val GroqWhisperAdapter=object:ProviderAdapter{override val id="groq_whisper";override fun test(c:Context)=k(c,id)?.let{req("https://api.groq.com/openai/v1/models",it)}?:ApiTestResult(false,"Not Connected","API key is not configured")}
private val MistralVoxtralAdapter=object:ProviderAdapter{override val id="mistral_voxtral";override fun test(c:Context)=k(c,id)?.let{req("https://api.mistral.ai/v1/models",it)}?:ApiTestResult(false,"Not Connected","API key is not configured")}
private val OpenAiWhisperAdapter=object:ProviderAdapter{override val id="openai_whisper";override fun test(c:Context)=k(c,id)?.let{req("https://api.openai.com/v1/models",it)}?:ApiTestResult(false,"Not Connected","API key is not configured")}
private val ElevenLabsTtsAdapter=object:ProviderAdapter{override val id="elevenlabs";override fun test(c:Context)=k(c,id)?.let{req("https://api.elevenlabs.io/v1/models",it,header="xi-api-key")}?:ApiTestResult(false,"Not Connected","API key is not configured")}
private val GoogleTtsAdapter=object:ProviderAdapter{override val id="google_tts";override fun test(c:Context)=k(c,id)?.let{req("https://texttospeech.googleapis.com/v1/voices?key="+java.net.URLEncoder.encode(it,"UTF-8"),"")}?:ApiTestResult(false,"Not Connected","API key is not configured")}
private val AzureSpeechAdapter=object:ProviderAdapter{
 override val id="azure_speech"
 override fun test(c:Context)=k(c,id)?.let{key->
  val region=AppSettings.azureRegion(c)
  req("https://$region.api.cognitive.microsoft.com/sts/v1.0/issueToken",key,"POST",null,"Ocp-Apim-Subscription-Key")
 }?:ApiTestResult(false,"Not Connected","API key is not configured")
}
private val AmazonPollyAdapter=object:ProviderAdapter{override val id="amazon_polly";override fun test(c:Context)=ApiTestResult(false,"Credentials Required","AWS Polly needs access-key + secret-key signing; this adapter is present and explicitly blocks unsafe one-field authentication")}
private val FishAudioAdapter=object:ProviderAdapter{override val id="fish_audio";override fun test(c:Context)=k(c,id)?.let{req("https://api.fish.audio/v1/models",it,"GET",null,"Authorization")}?:ApiTestResult(false,"Not Connected","API key is not configured")}
private val CartesiaAdapter=object:ProviderAdapter{override val id="cartesia";override fun test(c:Context)=k(c,id)?.let{req("https://api.cartesia.ai/tts/bytes",it,"POST",JSONObject().put("model_id","sonic-2").put("transcript","OK").put("voice",JSONObject().put("mode","id").put("id","694f9389-aacb-45b6-b726-9d9369183238")).put("output_format",JSONObject().put("container","wav").put("encoding","pcm_s16le").put("sample_rate",16000)).toString(),"X-API-Key")}?:ApiTestResult(false,"Not Connected","API key is not configured")}
private val RimeAdapter=object:ProviderAdapter{override val id="rime";override fun test(c:Context)=k(c,id)?.let{req("https://users.rime.ai/v1/rime-tts",it,"POST",JSONObject().put("text","OK").put("speaker","astra").toString())}?:ApiTestResult(false,"Not Connected","API key is not configured")}
