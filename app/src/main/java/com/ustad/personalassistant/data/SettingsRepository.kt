package com.ustad.personalassistant.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.edit

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
    suspend fun setAssistantEnabled(enabled: Boolean)
    suspend fun setDisplayName(name: String)
    suspend fun setVoiceLanguage(value: String)
    suspend fun setPreferredSttProvider(value: String)
    suspend fun setPreferredTtsProvider(value: String)
    suspend fun setVoiceName(value: String)
    suspend fun setSpeechSpeed(value: Float)
    suspend fun setSpeechPitch(value: Float)
    suspend fun setAutoSpeak(value: Boolean)
}

class SettingsRepositoryImpl(private val context: Context) : SettingsRepository {
    private object Keys {
        val assistantEnabled = booleanPreferencesKey("assistant_enabled")
        val displayName = stringPreferencesKey("display_name")
        val voiceLanguage = stringPreferencesKey("voice_language")
        val preferredSttProvider = stringPreferencesKey("preferred_stt_provider")
        val preferredTtsProvider = stringPreferencesKey("preferred_tts_provider")
        val voiceName = stringPreferencesKey("voice_name")
        val speechSpeed = androidx.datastore.preferences.core.floatPreferencesKey("speech_speed")
        val speechPitch = androidx.datastore.preferences.core.floatPreferencesKey("speech_pitch")
        val autoSpeak = booleanPreferencesKey("auto_speak")
    }

    override val assistantEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.assistantEnabled] ?: true }
    override val displayName: Flow<String> = context.settingsDataStore.data.map { it[Keys.displayName] ?: "Personal User" }
    override val voiceLanguage: Flow<String> = context.settingsDataStore.data.map { it[Keys.voiceLanguage] ?: "HINGLISH" }
    override val preferredSttProvider: Flow<String> = context.settingsDataStore.data.map { it[Keys.preferredSttProvider] ?: "AUTO" }
    override val preferredTtsProvider: Flow<String> = context.settingsDataStore.data.map { it[Keys.preferredTtsProvider] ?: "AUTO" }
    override val voiceName: Flow<String> = context.settingsDataStore.data.map { it[Keys.voiceName] ?: "" }
    override val speechSpeed: Flow<Float> = context.settingsDataStore.data.map { it[Keys.speechSpeed] ?: 1.0f }
    override val speechPitch: Flow<Float> = context.settingsDataStore.data.map { it[Keys.speechPitch] ?: 1.0f }
    override val autoSpeak: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.autoSpeak] ?: true }

    override suspend fun setAssistantEnabled(enabled: Boolean) { context.settingsDataStore.edit { it[Keys.assistantEnabled] = enabled } }
    override suspend fun setDisplayName(name: String) { context.settingsDataStore.edit { it[Keys.displayName] = name.trim().take(80) } }
    override suspend fun setVoiceLanguage(value: String) { context.settingsDataStore.edit { it[Keys.voiceLanguage] = value } }
    override suspend fun setPreferredSttProvider(value: String) { context.settingsDataStore.edit { it[Keys.preferredSttProvider] = value } }
    override suspend fun setPreferredTtsProvider(value: String) { context.settingsDataStore.edit { it[Keys.preferredTtsProvider] = value } }
    override suspend fun setVoiceName(value: String) { context.settingsDataStore.edit { it[Keys.voiceName] = value.take(120) } }
    override suspend fun setSpeechSpeed(value: Float) { context.settingsDataStore.edit { it[Keys.speechSpeed] = value.coerceIn(0.5f, 2.0f) } }
    override suspend fun setSpeechPitch(value: Float) { context.settingsDataStore.edit { it[Keys.speechPitch] = value.coerceIn(0.5f, 2.0f) } }
    override suspend fun setAutoSpeak(value: Boolean) { context.settingsDataStore.edit { it[Keys.autoSpeak] = value } }
}
