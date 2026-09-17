package com.ustad.personalassistant

import android.app.Application
import com.ustad.personalassistant.accessibility.AccessibilityActionEngine
import com.ustad.personalassistant.accessibility.AndroidAccessibilityActionEngine
import com.ustad.personalassistant.accessibility.DefaultAutomationPolicy
import com.ustad.personalassistant.accessibility.AutomationPolicy
import com.ustad.personalassistant.capability.CapabilityEngine
import com.ustad.personalassistant.capability.DefaultCapabilityEngine
import com.ustad.personalassistant.data.AppStateRepositoryImpl
import com.ustad.personalassistant.data.SettingsRepositoryImpl
import com.ustad.personalassistant.gmail.DefaultGmailService
import com.ustad.personalassistant.gmail.GmailService
import com.ustad.personalassistant.gmail.SecureGmailAuthStateStore
import com.ustad.personalassistant.gmail.UnconfiguredGmailAuthManager
import com.ustad.personalassistant.gmail.UnconfiguredGmailRepository
import com.ustad.personalassistant.permissions.AndroidPermissionManager
import com.ustad.personalassistant.security.SecureConfigStore
import com.ustad.personalassistant.security.SecurityManagerImpl
import com.ustad.personalassistant.services.UstadAccessibilityService

class UstadApplication : Application() {
    lateinit var permissionManager: AndroidPermissionManager; private set
    lateinit var capabilityEngine: CapabilityEngine; private set
    lateinit var appStateRepository: AppStateRepositoryImpl; private set
    lateinit var settingsRepository: SettingsRepositoryImpl; private set
    lateinit var securityManager: SecurityManagerImpl; private set
    lateinit var automationPolicy: AutomationPolicy; private set
    lateinit var accessibilityActionEngine: AccessibilityActionEngine; private set
    lateinit var gmailService: GmailService; private set

    override fun onCreate() {
        super.onCreate()
        permissionManager = AndroidPermissionManager(this)
        capabilityEngine = DefaultCapabilityEngine(permissionManager)
        appStateRepository = AppStateRepositoryImpl(permissionManager)
        settingsRepository = SettingsRepositoryImpl(this)
        securityManager = SecurityManagerImpl()
        automationPolicy = DefaultAutomationPolicy()
        accessibilityActionEngine = AndroidAccessibilityActionEngine({ UstadAccessibilityService.active }, securityManager)
        val gmailState = SecureGmailAuthStateStore(SecureConfigStore(this, "gmail_auth_state"))
        gmailService = DefaultGmailService(UnconfiguredGmailRepository(), UnconfiguredGmailAuthManager(gmailState))
    }
}
