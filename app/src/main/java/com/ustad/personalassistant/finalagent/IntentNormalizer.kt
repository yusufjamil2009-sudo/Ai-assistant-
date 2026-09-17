package com.ustad.personalassistant.finalagent

/** Small, deterministic normalization layer. It only normalizes wording; it never grants authority. */
object IntentNormalizer {
    fun normalize(input: String): String {
        var value = input.trim().replace(Regex("\\s+"), " ")
        if (value.isBlank()) return value
        val replacements = listOf(
            Regex("(?i)\\bwhatsapp kholo\\b") to "open whatsapp",
            Regex("(?i)\\bwhatsapp open karo\\b") to "open whatsapp",
            Regex("(?i)\\bwhatsapp khol do\\b") to "open whatsapp",
            Regex("(?i)\\bbattery (kitni|kitna) hai\\b") to "battery status",
            Regex("(?i)\\bbattery status batao\\b") to "battery status",
            Regex("(?i)\\bphone battery check karo\\b") to "battery status",
            Regex("(?i)\\bstorage (kitni|kitna) hai\\b") to "storage status",
            Regex("(?i)\\bstorage check karo\\b") to "storage status",
            Regex("(?i)\\bphone check karo\\b") to "phone diagnostics",
            Regex("(?i)\\bphone diagnostics karo\\b") to "phone diagnostics",
            Regex("(?i)\\binternet check karo\\b") to "network status"
        )
        replacements.forEach { (pattern, replacement) -> value = pattern.replace(value, replacement) }
        return value
    }

    fun isCancellation(input: String): Boolean =
        input.trim().lowercase() in setOf("cancel", "stop", "ruko", "ruk jao", "mat bhejo", "don't send", "do not send")

    fun isExplicitConfirmation(input: String): Boolean =
        input.trim().lowercase() in setOf("yes", "haan", "ha", "kar do", "bhej do", "send it", "confirm", "confirmed")
}
