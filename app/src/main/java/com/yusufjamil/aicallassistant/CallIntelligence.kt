package com.yusufjamil.aicallassistant

data class CallIntelligence(
    val callerNumber: String?,
    val callerName: String?,
    val savedContact: Boolean,
    val purpose: String,
    val category: CallCategory,
    val callerSaid: String,
    val assistantSaid: String,
    val importantPoints: List<String>,
    val followUpRequired: Boolean,
    val followUpNote: String,
    val confidence: Float
)

enum class CallCategory { WORK, INFORMATION, PERSONAL, WRONG_NUMBER, FOLLOW_UP, OTHER }

object CallIntelligenceEngine {
    fun analyze(
        callerNumber: String?,
        callerName: String?,
        savedContact: Boolean,
        callerText: String,
        assistantText: String
    ): CallIntelligence {
        val normalized = callerText.lowercase()
        val category = when {
            listOf("wrong number", "galat number", "गलत नंबर").any { normalized.contains(it) } -> CallCategory.WRONG_NUMBER
            listOf("meeting", "office", "काम", "काम के", "project", "job").any { normalized.contains(it) } -> CallCategory.WORK
            listOf("call back", "callback", "baad mein", "बाद में", "follow up").any { normalized.contains(it) } -> CallCategory.FOLLOW_UP
            listOf("information", "info", "jaankari", "जानकारी", "detail", "details").any { normalized.contains(it) } -> CallCategory.INFORMATION
            listOf("family", "friend", "dost", "दोस्त", "ghar", "घर").any { normalized.contains(it) } -> CallCategory.PERSONAL
            else -> CallCategory.OTHER
        }

        val purpose = when (category) {
            CallCategory.WRONG_NUMBER -> "Possible wrong number"
            CallCategory.WORK -> "Work-related conversation"
            CallCategory.FOLLOW_UP -> "Follow-up or callback requested"
            CallCategory.INFORMATION -> "Information or details requested"
            CallCategory.PERSONAL -> "Personal conversation"
            CallCategory.OTHER -> "Purpose not confidently identified"
        }

        val followUp = category == CallCategory.FOLLOW_UP ||
            listOf("call me", "call back", "callback", "फिर फोन", "बाद में फोन").any { normalized.contains(it) }

        val points = callerText.lines().map { it.trim() }.filter { it.isNotBlank() }.take(8)
        return CallIntelligence(
            callerNumber, callerName, savedContact, purpose, category,
            callerText.trim(), assistantText.trim(), points, followUp,
            if (followUp) "Caller may expect a callback or further action." else "",
            if (callerText.isBlank()) 0f else 0.55f
        )
    }
}