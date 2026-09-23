package com.yusufjamil.aicallassistant

import android.content.Context

/**
 * Application settings stored in SharedPreferences.
 * Handles profile, language, voice, and provider routing settings.
 */
object AppSettings {
    private const val PREFS = "app_settings"
    
    // Profile settings
    fun name(c: Context) = c.getSharedPreferences(PREFS, 0).getString("name", "").orEmpty()
    fun language(c: Context) = c.getSharedPreferences(PREFS, 0).getString("language", "English").orEmpty()
    fun voice(c: Context) = c.getSharedPreferences(PREFS, 0).getString("voice", "Female").orEmpty()
    
    fun saveProfile(c: Context, name: String, language: String, voice: String) = 
        c.getSharedPreferences(PREFS, 0).edit()
            .putString("name", name)
            .putString("language", language)
            .putString("voice", voice)
            .apply()
    
    // Azure region for Azure Speech
    fun azureRegion(c: Context) = c.getSharedPreferences(PREFS, 0).getString("azure_region", "centralindia").orEmpty()
    fun saveAzureRegion(c: Context, region: String) = 
        c.getSharedPreferences(PREFS, 0).edit()
            .putString("azure_region", region.trim())
            .apply()
    
    // Legacy primary/backup (for backward compatibility)
    fun primary(c: Context) = c.getSharedPreferences(PREFS, 0).getString("primary", "groq").orEmpty()
    fun backup(c: Context) = c.getSharedPreferences(PREFS, 0).getString("backup", "gemini").orEmpty()
    fun saveRouting(c: Context, primary: String, backup: String) = 
        c.getSharedPreferences(PREFS, 0).edit()
            .putString("primary", primary)
            .putString("backup", backup)
            .apply()
    
    // Brain provider routing
    fun primaryBrain(c: Context) = c.getSharedPreferences(PREFS, 0).getString("primary_brain", "groq").orEmpty()
    fun backupBrain(c: Context) = c.getSharedPreferences(PREFS, 0).getString("backup_brain", "gemini").orEmpty()
    fun saveBrainRouting(c: Context, primary: String, backup: String) = 
        c.getSharedPreferences(PREFS, 0).edit()
            .putString("primary_brain", primary)
            .putString("backup_brain", backup)
            .apply()
    
    // STT provider routing
    fun primaryStt(c: Context) = c.getSharedPreferences(PREFS, 0).getString("primary_stt", "deepgram").orEmpty()
    fun backupStt(c: Context) = c.getSharedPreferences(PREFS, 0).getString("backup_stt", "groq_whisper").orEmpty()
    fun saveSttRouting(c: Context, primary: String, backup: String) = 
        c.getSharedPreferences(PREFS, 0).edit()
            .putString("primary_stt", primary)
            .putString("backup_stt", backup)
            .apply()
    
    // TTS provider routing
    fun primaryTts(c: Context) = c.getSharedPreferences(PREFS, 0).getString("primary_tts", "elevenlabs").orEmpty()
    fun backupTts(c: Context) = c.getSharedPreferences(PREFS, 0).getString("backup_tts", "google_tts").orEmpty()
    fun saveTtsRouting(c: Context, primary: String, backup: String) = 
        c.getSharedPreferences(PREFS, 0).edit()
            .putString("primary_tts", primary)
            .putString("backup_tts", backup)
            .apply()
    
    // Call audio status display preference
    fun showCallAudioStatus(c: Context) = c.getSharedPreferences(PREFS, 0).getBoolean("show_call_audio_status", true)
    fun setShowCallAudioStatus(c: Context, show: Boolean) = 
        c.getSharedPreferences(PREFS, 0).edit()
            .putBoolean("show_call_audio_status", show)
            .apply()
}
