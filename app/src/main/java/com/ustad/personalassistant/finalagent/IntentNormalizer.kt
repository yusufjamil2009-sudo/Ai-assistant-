package com.ustad.personalassistant.finalagent

/** Small, deterministic normalization layer. It only normalizes wording; it never grants authority. */
object IntentNormalizer {
    fun normalize(input: String): String {
        var value = input.trim().replace(Regex("\\s+"), " ")
        if (value.isBlank()) return value
        val replacements = listOf(
            Regex("(?i)\\bwhatsapp\\s+(?:kholo|khol\\s+do|open\\s+karo)\\b[?.!]*") to "open whatsapp",
            Regex("(?i)\\bbattery\\s+(?:kitni|kitna)\\s+hai\\b[?.!]*") to "battery status",
            Regex("(?i)\\bbattery\\s+status\\s+batao\\b[?.!]*") to "battery status",
            Regex("(?i)\\bphone\\s+battery\\s+check\\s+karo\\b[?.!]*") to "battery status",
            Regex("(?i)\\bstorage\\s+(?:kitni|kitna)\\s+hai\\b[?.!]*") to "storage status",
            Regex("(?i)\\bstorage\\s+check\\s+karo\\b[?.!]*") to "storage status",
            Regex("(?i)\\bphone\\s+check\\s+karo\\b[?.!]*") to "phone diagnostics",
            Regex("(?i)\\bphone\\s+diagnostics\\s+karo\\b[?.!]*") to "phone diagnostics",
            Regex("(?i)\\binternet\\s+check\\s+karo\\b[?.!]*") to "network status"
        )
        replacements.forEach { (pattern, replacement) -> value = pattern.replace(value, replacement) }
        return value
    }

    fun isCancellation(input: String): Boolean =
        input.trim().lowercase() in setOf("cancel", "stop", "ruko", "ruk jao", "mat bhejo", "don't send", "do not send")

    fun isExplicitConfirmation(input: String): Boolean =
        input.trim().lowercase() in setOf("yes", "haan", "ha", "kar do", "bhej do", "send it", "confirm", "confirmed")
}
