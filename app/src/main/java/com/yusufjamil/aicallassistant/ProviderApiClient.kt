package com.yusufjamil.aicallassistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class ApiTestResult(val ok:Boolean,val status:String,val detail:String)

object ProviderApiClient {
 fun test(context:Context,id:String):ApiTestResult { val key=SecureApiKeyStore.read(context,id)?:return ApiTestResult(false,"Not Connected","API key is not configured"); return try { when(id) {
  "groq"->openAi("https://api.groq.com/openai/v1/chat/completions",key,"llama-3.3-70b-versatile")
  "openrouter"->openAi("https://openrouter.ai/api/v1/chat/completions",key,"openai/gpt-oss-20b")
  "gemini"->gemini(key)
  "elevenlabs"->eleven(key)
  else->ApiTestResult(false,"Adapter Pending","No verified adapter configured for this provider yet") } } catch(e:Exception){ApiTestResult(false,"Error",e.message?:"Request failed")} }
 fun chat(context:Context,id:String,prompt:String):String { val key=SecureApiKeyStore.read(context,id)?:return ""; return when(id){"groq"->openAiText("https://api.groq.com/openai/v1/chat/completions",key,"llama-3.3-70b-versatile",prompt);"openrouter"->openAiText("https://openrouter.ai/api/v1/chat/completions",key,"openai/gpt-oss-20b",prompt);"gemini"->geminiText(key,prompt);else->""} }
 private fun openAi(url:String,key:String,model:String):ApiTestResult { val t=openAiText(url,key,model,"Reply with exactly: connection ok"); return if(t.isNotBlank())ApiTestResult(true,"Connected",t.take(120))else ApiTestResult(false,"Invalid/Unavailable","No assistant text returned") }
 private fun openAiText(url:String,key:String,model:String,prompt:String):String { val b=JSONObject().put("model",model).put("messages",JSONArray().put(JSONObject().put("role","user").put("content",prompt))).put("max_completion_tokens",64); val j=post(url,key,b); return j.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty() }
 private fun gemini(key:String):ApiTestResult { val t=geminiText(key,"Reply with exactly: connection ok"); return if(t.isNotBlank())ApiTestResult(true,"Connected",t.take(120))else ApiTestResult(false,"Invalid/Unavailable","No text returned") }
 private fun geminiText(key:String,prompt:String):String { val u="https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="+java.net.URLEncoder.encode(key,"UTF-8"); val b=JSONObject().put("contents",JSONArray().put(JSONObject().put("parts",JSONArray().put(JSONObject().put("text",prompt))))); val j=post(u,null,b); return j.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text").orEmpty() }
 private fun eleven(key:String):ApiTestResult { val c=URL("https://api.elevenlabs.io/v1/models").openConnection() as HttpURLConnection; c.requestMethod="GET"; c.setRequestProperty("xi-api-key",key); c.connectTimeout=10000;c.readTimeout=10000;val n=c.responseCode;c.disconnect();return if(n in 200..299)ApiTestResult(true,"Connected","ElevenLabs API key accepted")else ApiTestResult(false,if(n==401||n==403)"Invalid" else if(n==429)"Rate Limit" else "HTTP $n","Provider returned HTTP $n") }
 private fun post(url:String,key:String?,body:JSONObject):JSONObject { val c=URL(url).openConnection() as HttpURLConnection;c.requestMethod="POST";c.doOutput=true;c.connectTimeout=15000;c.readTimeout=30000;c.setRequestProperty("Content-Type","application/json");if(!key.isNullOrBlank())c.setRequestProperty("Authorization","Bearer $key");OutputStreamWriter(c.outputStream).use{it.write(body.toString())};val n=c.responseCode;val st=if(n in 200..299)c.inputStream else c.errorStream;val txt=st.bufferedReader().use{it.readText()};c.disconnect();if(n !in 200..299)throw IllegalStateException("HTTP $n: ${txt.take(300)}");return JSONObject(txt) }
}