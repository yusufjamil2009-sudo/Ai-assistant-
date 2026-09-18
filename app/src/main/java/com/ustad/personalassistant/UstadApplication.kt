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
import com.ustad.personalassistant.appcontrol.AndroidActionVerificationEngine
import com.ustad.personalassistant.appcontrol.AndroidAppAutomationEngine
import com.ustad.personalassistant.appcontrol.AndroidAppResolver
import com.ustad.personalassistant.appcontrol.AppAutomationAdapter
import com.ustad.personalassistant.appcontrol.GenericAndroidAppAdapter
import com.ustad.personalassistant.appcontrol.PhotoFilePicker
import com.ustad.personalassistant.appcontrol.AndroidPhotoFilePicker
import com.ustad.personalassistant.automation.AutomationPipeline
import com.ustad.personalassistant.automation.ConfirmationPolicy
import com.ustad.personalassistant.automation.DefaultConfirmationPolicy
import com.ustad.personalassistant.background.BackgroundAssistantManager
import com.ustad.personalassistant.capability.CapabilityEngine
import com.ustad.personalassistant.capability.DefaultCapabilityEngine
import com.ustad.personalassistant.capability.CapabilityGate
import com.ustad.personalassistant.data.AppStateRepositoryImpl
import com.ustad.personalassistant.data.SettingsRepositoryImpl
import com.ustad.personalassistant.gmail.DefaultGmailService
import com.ustad.personalassistant.gmail.GmailService
import com.ustad.personalassistant.gmail.GmailApiAdapter
import com.ustad.personalassistant.gmail.GmailOAuthManager
import com.ustad.personalassistant.gmail.UnconfiguredGmailRepository
import com.ustad.personalassistant.messaging.AndroidContactResolver
import com.ustad.personalassistant.messaging.DefaultMessagingEngine
import com.ustad.personalassistant.messaging.MessengerAdapter
import com.ustad.personalassistant.messaging.MessageProvider
import com.ustad.personalassistant.messaging.SmsAdapter
import com.ustad.personalassistant.messaging.WhatsAppAdapter
import com.ustad.personalassistant.messaging.MessagingEngine
import com.ustad.personalassistant.permissions.AndroidPermissionManager
import com.ustad.personalassistant.security.SecureConfigStore
import com.ustad.personalassistant.security.SecurityFirewall
import com.ustad.personalassistant.security.DefaultProtectedAppPolicy
import com.ustad.personalassistant.finalagent.AgentOrchestrator
import com.ustad.personalassistant.security.SecurityManagerImpl
import com.ustad.personalassistant.services.ActionExecutor
import com.ustad.personalassistant.services.AdvancedAccessibilityService
import com.ustad.personalassistant.services.GuardedActionExecutor
import com.ustad.personalassistant.voice.AndroidVoiceEngine
import com.ustad.personalassistant.voice.AndroidVoiceSampleCapture
import com.ustad.personalassistant.voice.LocalVoiceAuthenticationEngine
import com.ustad.personalassistant.voice.SpeechToTextManager
import com.ustad.personalassistant.voice.TextToSpeechManager
import com.ustad.personalassistant.voice.VoiceAuthenticationEngine
import com.ustad.personalassistant.voice.VoiceEngine
import com.ustad.personalassistant.voice.VoiceSessionManager
import com.ustad.personalassistant.voice.VoiceProviderRegistry
import com.ustad.personalassistant.part08.CallAssistantEngine
import com.ustad.personalassistant.part08.Part08Orchestrator

class UstadApplication : Application() {
    lateinit var permissionManager: AndroidPermissionManager; private set; lateinit var capabilityEngine: CapabilityEngine; private set; lateinit var appStateRepository: AppStateRepositoryImpl; private set; lateinit var settingsRepository: SettingsRepositoryImpl; private set; lateinit var securityManager: SecurityManagerImpl; private set; lateinit var automationPolicy: AutomationPolicy; private set; lateinit var accessibilityActionEngine: AccessibilityActionEngine; private set; lateinit var appResolver: AndroidAppResolver; private set; lateinit var appAutomationEngine: AndroidAppAutomationEngine; private set; lateinit var photoFilePicker: PhotoFilePicker; private set; lateinit var messagingEngine: MessagingEngine; private set; lateinit var gmailService: GmailService; private set; lateinit var gmailAuthManager: GmailOAuthManager; private set; lateinit var part08Orchestrator: Part08Orchestrator; private set; lateinit var callAssistantEngine: CallAssistantEngine; private set; lateinit var aiProviderManager: AiProviderManager; private set; lateinit var networkMonitor: AndroidNetworkMonitor; private set; lateinit var aiBrain: AiBrain; private set; lateinit var actionExecutor: ActionExecutor; private set; lateinit var automationPipeline: AutomationPipeline; private set; lateinit var confirmationPolicy: ConfirmationPolicy; private set; lateinit var securityFirewall: SecurityFirewall; private set; lateinit var finalAgentOrchestrator: AgentOrchestrator; private set; lateinit var aiAutomationOrchestrator: AiAutomationOrchestrator; private set; lateinit var voiceProviderRegistry: VoiceProviderRegistry; private set; lateinit var sttManager: SpeechToTextManager; private set; lateinit var ttsManager: TextToSpeechManager; private set; lateinit var voiceEngine: VoiceEngine; private set; lateinit var voiceAuthenticationEngine: VoiceAuthenticationEngine; private set; lateinit var voiceSessionManager: VoiceSessionManager; private set; lateinit var backgroundAssistantManager: BackgroundAssistantManager; private set
    override fun onCreate() {
        super.onCreate(); permissionManager = AndroidPermissionManager(this); capabilityEngine = DefaultCapabilityEngine(permissionManager); appStateRepository = AppStateRepositoryImpl(permissionManager); settingsRepository = SettingsRepositoryImpl(this); securityManager = SecurityManagerImpl(); automationPolicy = DefaultAutomationPolicy(); accessibilityActionEngine = AndroidAccessibilityActionEngine({ AdvancedAccessibilityService.active }, securityManager); appResolver = AndroidAppResolver(this); appAutomationEngine = AndroidAppAutomationEngine(appResolver, accessibilityActionEngine, AndroidActionVerificationEngine(accessibilityActionEngine), capabilityEngine, securityManager, automationPolicy, listOf<AppAutomationAdapter>(GenericAndroidAppAdapter())); photoFilePicker = AndroidPhotoFilePicker(); aiProviderManager = AiProviderManager(this); networkMonitor = AndroidNetworkMonitor(this); val apiManager = aiProviderManager.buildApiManager { networkMonitor.state() }; aiBrain = CentralAiBrain(UnavailableOnDeviceAiProvider(), apiManager) { aiProviderManager.routingPolicy() }
        val gmailStore = SecureConfigStore(this, "gmail_auth_state"); gmailAuthManager = GmailOAuthManager(this, gmailStore, BuildConfig.GMAIL_CLIENT_ID); gmailService = if (BuildConfig.GMAIL_CLIENT_ID.isBlank()) DefaultGmailService(UnconfiguredGmailRepository(), gmailAuthManager) else DefaultGmailService(GmailApiAdapter(gmailAuthManager), gmailAuthManager)
        messagingEngine = DefaultMessagingEngine(this, AndroidContactResolver(this), aiBrain, listOf<MessageProvider>(WhatsAppAdapter(this), MessengerAdapter(this), SmsAdapter(this))); actionExecutor = GuardedActionExecutor(securityManager, capabilityEngine, appAutomationEngine); confirmationPolicy = DefaultConfirmationPolicy(); securityFirewall = SecurityFirewall(securityManager, DefaultProtectedAppPolicy(), automationPolicy); automationPipeline = AutomationPipeline(capabilityEngine, securityManager, automationPolicy, { action, target -> actionExecutor.execute(action, target) }, securityFirewall = securityFirewall); finalAgentOrchestrator = AgentOrchestrator(aiBrain, automationPipeline, capabilityEngine, securityFirewall, confirmationPolicy); aiAutomationOrchestrator = AiAutomationOrchestrator(aiBrain, automationPipeline, confirmationPolicy, finalAgentOrchestrator); voiceProviderRegistry = VoiceProviderRegistry(this) { networkMonitor.state().name == "ONLINE" }; sttManager = SpeechToTextManager({ voiceProviderRegistry.sttProviders(this) }, { networkMonitor.state().name == "ONLINE" }); ttsManager = TextToSpeechManager({ voiceProviderRegistry.ttsProviders(this) }, { networkMonitor.state().name == "ONLINE" }); voiceEngine = AndroidVoiceEngine(permissionManager, capabilityEngine, sttManager, ttsManager, aiAutomationOrchestrator, settingsRepository); voiceAuthenticationEngine = LocalVoiceAuthenticationEngine(this, AndroidVoiceSampleCapture(this)); voiceSessionManager = VoiceSessionManager(this, voiceEngine, voiceAuthenticationEngine, settingsRepository); backgroundAssistantManager = BackgroundAssistantManager(this)
        part08Orchestrator = Part08Orchestrator(CapabilityGate(capabilityEngine), securityManager, gmailService, gmailAuthManager, aiBrain); callAssistantEngine = CallAssistantEngine(this, settingsRepository, aiBrain, voiceSessionManager)
    }
}
