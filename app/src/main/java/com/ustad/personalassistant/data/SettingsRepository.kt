package com.ustad.personalassistant.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "ustad_settings")

interface SettingsRepository {
    val assistantEnabled: Flow<Boolean>
    val displayName: Flow<String>
    val voiceLanguage: Flow<String>
    val preferredSttProvider: Flow<String>
    val preferredTtsProvider: Flow<String>
    val voiceName: Flow<String>
    val speechSpeed: Flow<Float>
    val speechPitch: Flow<Float>
    val autoSpeak: Flow<Boolean>
    val backgroundAssistantEnabled: Flow<Boolean>
    val wakeWordEnabled: Flow<Boolean>
    val wakePhrase: Flow<String>
    val voiceAuthenticationEnabled: Flow<Boolean>
    suspend fun setAssistantEnabled(enabled: Boolean)
    suspend fun setDisplayName(name: String)
    suspend fun setVoiceLanguage(value: String)
    suspend fun setPreferredSttProvider(value: String)
    suspend fun setPreferredTtsProvider(value: String)
    suspend fun setVoiceName(value: String)
    suspend fun setSpeechSpeed(value: Float)
    suspend fun setSpeechPitch(value: Float)
    suspend fun setAutoSpeak(value: Boolean)
    suspend fun setBackgroundAssistantEnabled(enabled: Boolean)
    suspend fun setWakeWordEnabled(enabled: Boolean)
    suspend fun setWakePhrase(value: String)
    suspend fun setVoiceAuthenticationEnabled(enabled: Boolean)
    fun setBackgroundAssistantEnabledBlocking(enabled: Boolean)
}

class SettingsRepositoryImpl(private val context: Context) : SettingsRepository {
    private object Keys {
        val assistantEnabled = booleanPreferencesKey("assistant_enabled"); val displayName = stringPreferencesKey("display_name"); val voiceLanguage = stringPreferencesKey("voice_language"); val preferredSttProvider = stringPreferencesKey("preferred_stt_provider"); val preferredTtsProvider = stringPreferencesKey("preferred_tts_provider"); val voiceName = stringPreferencesKey("voice_name"); val speechSpeed = floatPreferencesKey("speech_speed"); val speechPitch = floatPreferencesKey("speech_pitch"); val autoSpeak = booleanPreferencesKey("auto_speak"); val backgroundAssistantEnabled = booleanPreferencesKey("background_assistant_enabled"); val wakeWordEnabled = booleanPreferencesKey("wake_word_enabled"); val wakePhrase = stringPreferencesKey("wake_phrase"); val voiceAuthenticationEnabled = booleanPreferencesKey("voice_authentication_enabled")
    }
    override val assistantEnabled = context.settingsDataStore.data.map { it[Keys.assistantEnabled] ?: true }
    override val displayName = context.settingsDataStore.data.map { it[Keys.displayName] ?: "Personal User" }
    override val voiceLanguage = context.settingsDataStore.data.map { it[Keys.voiceLanguage] ?: "HINGLISH" }
    override val preferredSttProvider = context.settingsDataStore.data.map { it[Keys.preferredSttProvider] ?: "AUTO" }
    override val preferredTtsProvider = context.settingsDataStore.data.map { it[Keys.preferredTtsProvider] ?: "AUTO" }
    override val voiceName = context.settingsDataStore.data.map { it[Keys.voiceName] ?: "" }
    override val speechSpeed = context.settingsDataStore.data.map { it[Keys.speechSpeed] ?: 1.0f }
    override val speechPitch = context.settingsDataStore.data.map { it[Keys.speechPitch] ?: 1.0f }
    override val autoSpeak = context.settingsDataStore.data.map { it[Keys.autoSpeak] ?: true }
    override val backgroundAssistantEnabled = context.settingsDataStore.data.map { it[Keys.backgroundAssistantEnabled] ?: false }
    override val wakeWordEnabled = context.settingsDataStore.data.map { it[Keys.wakeWordEnabled] ?: true }
    override val wakePhrase = context.settingsDataStore.data.map { it[Keys.wakePhrase] ?: "Hello Assistant" }
    override val voiceAuthenticationEnabled = context.settingsDataStore.data.map { it[Keys.voiceAuthenticationEnabled] ?: false }
    override suspend fun setAssistantEnabled(enabled: Boolean) { context.settingsDataStore.edit { it[Keys.assistantEnabled] = enabled } }
    override suspend fun setDisplayName(name: String) { context.settingsDataStore.edit { it[Keys.displayName] = name.trim().take(80) } }
    override suspend fun setVoiceLanguage(value: String) { context.settingsDataStore.edit { it[Keys.voiceLanguage] = value } }
    override suspend fun setPreferredSttProvider(value: String) { context.settingsDataStore.edit { it[Keys.preferredSttProvider] = value } }
    override suspend fun setPreferredTtsProvider(value: String) { context.settingsDataStore.edit { it[Keys.preferredTtsProvider] = value } }
    override suspend fun setVoiceName(value: String) { context.settingsDataStore.edit { it[Keys.voiceName] = value.take(120) } }
    override suspend fun setSpeechSpeed(value: Float) { context.settingsDataStore.edit { it[Keys.speechSpeed] = value.coerceIn(0.5f, 2.0f) } }
    override suspend fun setSpeechPitch(value: Float) { context.settingsDataStore.edit { it[Keys.speechPitch] = value.coerceIn(0.5f, 2.0f) } }
    override suspend fun setAutoSpeak(value: Boolean) { context.settingsDataStore.edit { it[Keys.autoSpeak] = value } }
    override suspend fun setBackgroundAssistantEnabled(enabled: Boolean) { context.settingsDataStore.edit { it[Keys.backgroundAssistantEnabled] = enabled } }
    override suspend fun setWakeWordEnabled(enabled: Boolean) { context.settingsDataStore.edit { it[Keys.wakeWordEnabled] = enabled } }
    override suspend fun setWakePhrase(value: String) { context.settingsDataStore.edit { it[Keys.wakePhrase] = value.trim().take(80).ifBlank { "Hello Assistant" } } }
    override suspend fun setVoiceAuthenticationEnabled(enabled: Boolean) { context.settingsDataStore.edit { it[Keys.voiceAuthenticationEnabled] = enabled } }
    override fun setBackgroundAssistantEnabledBlocking(enabled: Boolean) { kotlinx.coroutines.runBlocking { setBackgroundAssistantEnabled(enabled) } }
}
