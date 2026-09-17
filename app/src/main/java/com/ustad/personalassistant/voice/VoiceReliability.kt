package com.ustad.personalassistant.voice

class StreamingAudioBuffer(private val maxBytes: Int = 8 * 1024 * 1024) {
    private val chunks = ArrayDeque<ByteArray>()
    private var size = 0
    @Synchronized fun append(chunk: ByteArray) { if (chunk.isEmpty()) return; val copy = chunk.copyOf(); chunks.addLast(copy); size += copy.size; while (size > maxBytes && chunks.isNotEmpty()) size -= chunks.removeFirst().size }
    @Synchronized fun drain(): ByteArray { val out = ByteArray(size); var offset = 0; while (chunks.isNotEmpty()) { val chunk = chunks.removeFirst(); chunk.copyInto(out, offset); offset += chunk.size }; size = 0; return out }
    @Synchronized fun clear() { chunks.clear(); size = 0 }
    @Synchronized fun sizeBytes(): Int = size
}

enum class VoiceInputOrigin { AUTHORIZED_USER, CALLER }
class VoiceActionGate {
    fun mayEnterActionPipeline(origin: VoiceInputOrigin, finalTranscript: Boolean): Boolean = origin == VoiceInputOrigin.AUTHORIZED_USER && finalTranscript
}
