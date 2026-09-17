package com.ustad.personalassistant

import android.app.Application
import com.ustad.personalassistant.capability.CapabilityEngine
import com.ustad.personalassistant.capability.DefaultCapabilityEngine
import com.ustad.personalassistant.data.AppStateRepositoryImpl
import com.ustad.personalassistant.data.SettingsRepositoryImpl
import com.ustad.personalassistant.permissions.AndroidPermissionManager
import com.ustad.personalassistant.security.SecurityManagerImpl

class UstadApplication : Application() {
    lateinit var permissionManager: AndroidPermissionManager
        private set
    lateinit var capabilityEngine: CapabilityEngine
        private set
    lateinit var appStateRepository: AppStateRepositoryImpl
        private set
    lateinit var settingsRepository: SettingsRepositoryImpl
        private set
    lateinit var securityManager: SecurityManagerImpl
        private set

    override fun onCreate() {
        super.onCreate()
        permissionManager = AndroidPermissionManager(this)
        capabilityEngine = DefaultCapabilityEngine(permissionManager)
        appStateRepository = AppStateRepositoryImpl(permissionManager)
        settingsRepository = SettingsRepositoryImpl(this)
        securityManager = SecurityManagerImpl()
    }
}
