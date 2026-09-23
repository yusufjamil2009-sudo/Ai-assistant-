package com.yusufjamil.aicallassistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CallSummary(
    val id: Long, val callerName: String?, val callerNumber: String?,
    val savedContact: Boolean, val purpose: String, val category: String,
    val callerSaid: String, val assistantSaid: String,
    val importantPoints: List<String>, val durationSeconds: Long, val startedAt: Long
)

class CallHistoryStore(private val context: Context) {
    private val prefs get() = context.getSharedPreferences("call_history", Context.MODE_PRIVATE)
    fun save(summary: CallSummary) {
        val all = JSONArray(prefs.getString("items", "[]"))
        val obj = JSONObject().apply {
            put("id", summary.id); put("callerName", summary.callerName); put("callerNumber", summary.callerNumber)
            put("savedContact", summary.savedContact); put("purpose", summary.purpose); put("category", summary.category)
            put("callerSaid", summary.callerSaid); put("assistantSaid", summary.assistantSaid)
            put("importantPoints", JSONArray(summary.importantPoints)); put("durationSeconds", summary.durationSeconds)
            put("startedAt", summary.startedAt)
        }
        val next = JSONArray().apply { put(obj); for (i in 0 until minOf(all.length(), 49)) put(all.getJSONObject(i)) }
        prefs.edit().putString("items", next.toString()).apply()
    }
    fun all(): List<CallSummary> {
        val a = JSONArray(prefs.getString("items", "[]")); val out = mutableListOf<CallSummary>()
        for (i in 0 until a.length()) { val o=a.getJSONObject(i); val p=o.optJSONArray("importantPoints") ?: JSONArray()
            out += CallSummary(o.optLong("id"), o.optString("callerName").takeIf{it!="null"}, o.optString("callerNumber").takeIf{it!="null"},
                o.optBoolean("savedContact"), o.optString("purpose"), o.optString("category"), o.optString("callerSaid"), o.optString("assistantSaid"),
                List(p.length()){j->p.optString(j)}, o.optLong("durationSeconds"), o.optLong("startedAt")) }
        return out
    }
    fun clear() = prefs.edit().remove("items").apply()
}