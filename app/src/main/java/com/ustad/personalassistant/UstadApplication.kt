package com.ustad.personalassistant

import android.app.Application
import com.ustad.personalassistant.accessibility.AccessibilityActionEngine
import com.ustad.personalassistant.accessibility.AndroidAccessibilityActionEngine
import com.ustad.personalassistant.accessibility.DefaultAutomationPolicy
import com.ustad.personalassistant.accessibility.AutomationPolicy
import com.ustad.personalassistant.ai.AiAutomationOrchestrator
import com.ustad.personalassistant.ai.AiBrain
import com.ustad.personalassistant.ai.AiProviderManager
import com.ustad.personalassistant.ai.AndroidNetworkMonitor
import com.ustad.personalassistant.ai.CentralAiBrain
import com.ustad.personalassistant.ai.UnavailableOnDeviceAiProvider
import com.ustad.personalassistant.automation.AutomationPipeline
import com.ustad.personalassistant.automation.ConfirmationPolicy
import com.ustad.personalassistant.automation.DefaultConfirmationPolicy
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
import com.ustad.personalassistant.services.ActionExecutor
import com.ustad.personalassistant.services.AdvancedAccessibilityService
import com.ustad.personalassistant.services.GuardedActionExecutor

class UstadApplication : Application() {
    lateinit var permissionManager: AndroidPermissionManager; private set
    lateinit var capabilityEngine: CapabilityEngine; private set
    lateinit var appStateRepository: AppStateRepositoryImpl; private set
    lateinit var settingsRepository: SettingsRepositoryImpl; private set
    lateinit var securityManager: SecurityManagerImpl; private set
    lateinit var automationPolicy: AutomationPolicy; private set
    lateinit var accessibilityActionEngine: AccessibilityActionEngine; private set
    lateinit var gmailService: GmailService; private set
    lateinit var aiProviderManager: AiProviderManager; private set
    lateinit var networkMonitor: AndroidNetworkMonitor; private set
    lateinit var aiBrain: AiBrain; private set
    lateinit var actionExecutor: ActionExecutor; private set
    lateinit var automationPipeline: AutomationPipeline; private set
    lateinit var confirmationPolicy: ConfirmationPolicy; private set
    lateinit var aiAutomationOrchestrator: AiAutomationOrchestrator; private set

    override fun onCreate() {
        super.onCreate()
        permissionManager = AndroidPermissionManager(this)
        capabilityEngine = DefaultCapabilityEngine(permissionManager)
        appStateRepository = AppStateRepositoryImpl(permissionManager)
        settingsRepository = SettingsRepositoryImpl(this)
        securityManager = SecurityManagerImpl()
        automationPolicy = DefaultAutomationPolicy()
        accessibilityActionEngine = AndroidAccessibilityActionEngine({ AdvancedAccessibilityService.active }, securityManager)
        val gmailState = SecureGmailAuthStateStore(SecureConfigStore(this, "gmail_auth_state"))
        gmailService = DefaultGmailService(UnconfiguredGmailRepository(), UnconfiguredGmailAuthManager(gmailState))

        aiProviderManager = AiProviderManager(this)
        networkMonitor = AndroidNetworkMonitor(this)
        val apiManager = aiProviderManager.buildApiManager { networkMonitor.state() }
        aiBrain = CentralAiBrain(UnavailableOnDeviceAiProvider(), apiManager) { aiProviderManager.routingPolicy() }
        actionExecutor = GuardedActionExecutor(securityManager, capabilityEngine)
        automationPipeline = AutomationPipeline(capabilityEngine, securityManager, automationPolicy, { action, target -> actionExecutor.execute(action, target) })
        confirmationPolicy = DefaultConfirmationPolicy()
        aiAutomationOrchestrator = AiAutomationOrchestrator(aiBrain, automationPipeline, confirmationPolicy)
    }
}
