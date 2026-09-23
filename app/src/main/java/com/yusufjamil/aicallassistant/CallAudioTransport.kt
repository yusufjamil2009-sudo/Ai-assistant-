package com.yusufjamil.aicallassistant

import android.content.Context
import android.telecom.Call

/**
 * Abstraction for call audio transport.
 * Android standard InCallService API does not expose a generic PCM stream of a normal SIM call
 * to third-party apps. This interface provides a clean abstraction for the audio transport layer.
 */
interface CallAudioTransport {
    fun isSupported(context: Context): Boolean
    fun start(call: Call): Boolean
    fun stop()
    fun isRunning(): Boolean
    fun readCallerAudio(): ByteArray?
    fun writeAssistantAudio(pcm16: ByteArray): Boolean
    fun getStatus(): CallAudioTransportStatus
}

enum class CallAudioTransportStatus {
    UNAVAILABLE,
    CONNECTING,
    READY,
    RUNNING,
    ERROR
}

/**
 * Default implementation that acknowledges Android limitations.
 * On standard Android, third-party apps cannot access the remote SIM call audio PCM stream.
 */
object DefaultCallAudioTransport : CallAudioTransport {
    private var running = false
    
    override fun isSupported(context: Context): Boolean = false
    
    override fun start(call: Call): Boolean {
        running = true
        return false
    }
    
    override fun stop() {
        running = false
    }
    
    override fun isRunning(): Boolean = running
    
    override fun readCallerAudio(): ByteArray? = null
    
    override fun writeAssistantAudio(pcm16: ByteArray): Boolean = false
    
    override fun getStatus(): CallAudioTransportStatus = 
        if (running) CallAudioTransportStatus.ERROR else CallAudioTransportStatus.UNAVAILABLE
}

/**
 * Provider for call audio transport.
 * Currently returns the default implementation which correctly reports that
 * caller audio access is unavailable on standard Android devices.
 */
object CallAudioTransportProvider {
    @Volatile
    private var transport: CallAudioTransport = DefaultCallAudioTransport
    
    fun install(value: CallAudioTransport) {
        transport = value
    }
    
    fun get(): CallAudioTransport = transport
    
    fun isAvailable(): Boolean = transport.isSupported(CallSession.currentCall?.context ?: return false)
    
    fun readCallerAudio(): ByteArray? = transport.readCallerAudio()
    
    fun writeAssistantAudio(pcm16: ByteArray): Boolean = transport.writeAssistantAudio(pcm16)
    
    fun getStatus(): CallAudioTransportStatus = transport.getStatus()
}
