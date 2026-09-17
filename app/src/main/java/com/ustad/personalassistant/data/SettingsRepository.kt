package com.ustad.personalassistant.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.edit

private val Context.settingsDataStore by preferencesDataStore(name = "ustad_settings")

interface SettingsRepository {
    val assistantEnabled: Flow<Boolean>
    val displayName: Flow<String>
    suspend fun setAssistantEnabled(enabled: Boolean)
    suspend fun setDisplayName(name: String)
}

class SettingsRepositoryImpl(private val context: Context) : SettingsRepository {
    private object Keys {
        val assistantEnabled = booleanPreferencesKey("assistant_enabled")
        val displayName = stringPreferencesKey("display_name")
    }

    override val assistantEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.assistantEnabled] ?: true }
    override val displayName: Flow<String> = context.settingsDataStore.data.map { it[Keys.displayName] ?: "Personal User" }

    override suspend fun setAssistantEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.assistantEnabled] = enabled }
    }

    override suspend fun setDisplayName(name: String) {
        context.settingsDataStore.edit { it[Keys.displayName] = name.trim().take(80) }
    }
}
