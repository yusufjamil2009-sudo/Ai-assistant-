package com.ustad.personalassistant.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.ustad.personalassistant.security.SecureConfigStore
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlin.math.sqrt

interface VoiceSampleCapture { fun capture(durationMs: Long = 1500L): Result<ByteArray> }

class AndroidVoiceSampleCapture(private val context: Context) : VoiceSampleCapture {
    override fun capture(durationMs: Long): Result<ByteArray> = runCatching {
        check(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) { "Microphone permission required" }
        val sampleRate = 16_000
        val min = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        check(min > 0) { "Microphone unavailable" }
        val bufferSize = (min * 2).coerceAtMost(64 * 1024)
        val recorder = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
        check(recorder.state == AudioRecord.STATE_INITIALIZED) { "Microphone unavailable" }
        val bytes = ByteArray(bufferSize)
        val output = ByteArrayOutputStreamBounded((sampleRate * durationMs / 1000L * 2L).toInt().coerceAtMost(96_000))
        try {
            recorder.startRecording()
            val end = System.currentTimeMillis() + durationMs.coerceIn(800L, 3000L)
            while (System.currentTimeMillis() < end && output.size < output.capacity) {
                val read = recorder.read(bytes, 0, bytes.size)
                if (read > 0) output.write(bytes, read)
            }
        } finally { recorder.stop(); recorder.release() }
        output.toByteArray()
    }
}

private class ByteArrayOutputStreamBounded(val capacity: Int) {
    private val buffer = ByteArray(capacity)
    var size: Int = 0
        private set
    fun write(source: ByteArray, length: Int) { val count = length.coerceAtMost(capacity - size); if (count > 0) { System.arraycopy(source, 0, buffer, size, count); size += count } }
    fun toByteArray(): ByteArray = buffer.copyOf(size)
}

data class VoiceProfile(val version: Int = 1, val features: FloatArray)
enum class VoiceAuthEngineState { READY, LISTENING, PROCESSING, POOR_AUDIO, TRY_AGAIN, ENROLLED, FAILED, LOCKED_OUT, UNAVAILABLE }
enum class VoiceAuthenticationResult { AUTHORIZED, UNAUTHORIZED, NOT_ENROLLED, LOCKED_OUT, UNAVAILABLE, POOR_AUDIO }
data class VoiceAuthenticationAttempt(val result: VoiceAuthenticationResult, val score: Double? = null)

interface VoiceAuthenticationEngine {
    fun status(): VoiceAuthEngineState
    fun isEnrolled(): Boolean
    fun enroll(onState: (VoiceAuthEngineState) -> Unit = {}): Result<Unit>
    fun authenticate(onState: (VoiceAuthEngineState) -> Unit = {}): Result<VoiceAuthenticationAttempt>
    fun resetEnrollment()
}

class LocalVoiceAuthenticationEngine(context: Context, private val capture: VoiceSampleCapture = AndroidVoiceSampleCapture(context)) : VoiceAuthenticationEngine {
    private val store = SecureConfigStore(context, "voice_auth_profile")
    private var failures = 0
    private var lockedUntil = 0L
    override fun status(): VoiceAuthEngineState = when { System.currentTimeMillis() < lockedUntil -> VoiceAuthEngineState.LOCKED_OUT; isEnrolled() -> VoiceAuthEngineState.ENROLLED; else -> VoiceAuthEngineState.READY }
    override fun isEnrolled(): Boolean = store.get("profile") != null

    override fun enroll(onState: (VoiceAuthEngineState) -> Unit): Result<Unit> = runCatching {
        onState(VoiceAuthEngineState.LISTENING)
        val samples = mutableListOf<FloatArray>()
        repeat(3) {
            val audio = capture.capture(1400L).getOrThrow()
            onState(VoiceAuthEngineState.PROCESSING)
            val feature = VoiceFeatureExtractor.extract(audio)
            check(feature != null) { "Poor audio quality" }
            samples += feature
            onState(VoiceAuthEngineState.LISTENING)
        }
        val average = FloatArray(samples.first().size) { index -> samples.map { it[index] }.average().toFloat() }
        store.put("profile", encode(VoiceProfile(features = average)))
        store.put("enabled", "false")
        failures = 0
        lockedUntil = 0L
        onState(VoiceAuthEngineState.ENROLLED)
    }.onFailure { onState(if (it.message == "Poor audio quality") VoiceAuthEngineState.POOR_AUDIO else VoiceAuthEngineState.FAILED) }

    override fun authenticate(onState: (VoiceAuthEngineState) -> Unit): Result<VoiceAuthenticationAttempt> = runCatching {
        if (!isEnrolled()) return@runCatching VoiceAuthenticationAttempt(VoiceAuthenticationResult.NOT_ENROLLED)
        if (System.currentTimeMillis() < lockedUntil) return@runCatching VoiceAuthenticationAttempt(VoiceAuthenticationResult.LOCKED_OUT)
        onState(VoiceAuthEngineState.LISTENING)
        val audio = capture.capture(1300L).getOrElse { return@runCatching VoiceAuthenticationAttempt(VoiceAuthenticationResult.UNAVAILABLE) }
        onState(VoiceAuthEngineState.PROCESSING)
        val feature = VoiceFeatureExtractor.extract(audio) ?: return@runCatching VoiceAuthenticationAttempt(VoiceAuthenticationResult.POOR_AUDIO)
        val profile = decode(store.get("profile") ?: return@runCatching VoiceAuthenticationAttempt(VoiceAuthenticationResult.NOT_ENROLLED))
        val score = VoiceFeatureExtractor.similarity(profile.features, feature)
        val authorized = score >= 0.90
        if (authorized) { failures = 0; onState(VoiceAuthEngineState.ENROLLED); VoiceAuthenticationAttempt(VoiceAuthenticationResult.AUTHORIZED, score) }
        else {
            failures += 1
            if (failures >= 3) { lockedUntil = System.currentTimeMillis() + 30_000L; failures = 0; onState(VoiceAuthEngineState.LOCKED_OUT); VoiceAuthenticationAttempt(VoiceAuthenticationResult.LOCKED_OUT, score) }
            else { onState(VoiceAuthEngineState.TRY_AGAIN); VoiceAuthenticationAttempt(VoiceAuthenticationResult.UNAUTHORIZED, score) }
        }
    }.onFailure { onState(VoiceAuthEngineState.FAILED) }

    override fun resetEnrollment() { store.remove("profile"); store.remove("enabled"); failures = 0; lockedUntil = 0L }
    private fun encode(profile: VoiceProfile): String = profile.version.toString() + ":" + profile.features.joinToString(",")
    private fun decode(value: String): VoiceProfile { val parts = value.split(":", limit = 2); val features = parts.getOrElse(1) { "" }.split(",").filter { it.isNotBlank() }.map { it.toFloat() }.toFloatArray(); return VoiceProfile(parts.first().toIntOrNull() ?: 1, features) }
}

object VoiceFeatureExtractor {
    fun extract(pcm: ByteArray): FloatArray? {
        if (pcm.size < 640) return null
        val samples = ShortArray(pcm.size / 2)
        ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples)
        val rms = sqrt(samples.map { it.toDouble() * it }.average()).toFloat() / 32768f
        if (rms < 0.008f) return null
        var zcr = 0
        for (i in 1 until samples.size) if ((samples[i - 1] >= 0) != (samples[i] >= 0)) zcr++
        val mean = samples.map { it.toDouble() }.average()
        val variance = samples.map { val d = it - mean; d * d }.average()
        val digest = MessageDigest.getInstance("SHA-256").digest(pcm.copyOfRange(0, minOf(pcm.size, 8192)))
        val hashFeatures = digest.take(8).map { (it.toInt() and 255) / 255f }
        return floatArrayOf(rms, zcr.toFloat() / samples.size, sqrt(variance).toFloat() / 32768f, mean.toFloat() / 32768f) + hashFeatures.toFloatArray()
    }
    fun similarity(a: FloatArray, b: FloatArray): Double {
        if (a.size != b.size || a.isEmpty()) return 0.0
        val normA = sqrt(a.sumOf { (it * it).toDouble() }); val normB = sqrt(b.sumOf { (it * it).toDouble() })
        if (normA == 0.0 || normB == 0.0) return 0.0
        val cosine = a.indices.sumOf { (a[it] * b[it]).toDouble() } / (normA * normB)
        return cosine.coerceIn(0.0, 1.0)
    }
}

object VoiceAuthenticationPolicy {
    fun mayEnterControlPipeline(sessionType: VoiceSessionType, result: VoiceAuthenticationResult): Boolean = sessionType == VoiceSessionType.NORMAL_ASSISTANT_SESSION && result == VoiceAuthenticationResult.AUTHORIZED
}
