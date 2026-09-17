package com.ustad.personalassistant.voice

/** User-facing catalog. Credentials are never embedded here. */
object CloudVoiceProviderCatalog {
    val stt: List<SpeechToTextConfig> = listOf(
        SpeechToTextConfig("deepgram", "Deepgram", priority = 1, streamingSupport = true),
        SpeechToTextConfig("assemblyai", "AssemblyAI", priority = 2, streamingSupport = true),
        SpeechToTextConfig("android_local", "Android / Local", enabled = true, priority = 99, offlineSupport = true, streamingSupport = true)
    )

    val tts: List<TextToSpeechConfig> = listOf(
        TextToSpeechConfig("elevenlabs", "ElevenLabs", priority = 1),
        TextToSpeechConfig("deepgram_aura", "Deepgram Aura", priority = 2),
        TextToSpeechConfig("android_tts", "Android Native TTS", enabled = true, priority = 99)
    )
}
