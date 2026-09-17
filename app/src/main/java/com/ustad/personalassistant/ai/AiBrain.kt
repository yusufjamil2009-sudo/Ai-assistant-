package com.ustad.personalassistant.ai

interface OnDeviceAiProvider {
    val state: OnDeviceAiState
    fun supports(request: AiRequest): Boolean
    fun generate(request: AiRequest): Result<AiResponse>
}

class UnavailableOnDeviceAiProvider : OnDeviceAiProvider {
    override val state = OnDeviceAiState.NOT_SUPPORTED
    override fun supports(request: AiRequest) = false
    override fun generate(request: AiRequest) = Result.failure<AiResponse>(AiException(AiErrorCode.ON_DEVICE_UNAVAILABLE))
}

interface AiBrain {
    fun processRequest(request: AiRequest): Result<AiResponse>
    fun understandIntent(request: AiRequest): Result<AiResponse>
    fun generateResponse(request: AiRequest): Result<AiResponse>
    fun planAction(request: AiRequest): Result<ActionPlan?>
    fun summarize(request: AiRequest): Result<AiResponse>
    fun classify(request: AiRequest): Result<AiResponse>
    fun extractEntities(request: AiRequest): Result<List<AiEntity>>
}

class CentralAiBrain(private val onDevice: OnDeviceAiProvider, private val apiManager: ApiManager, private val policy: () -> RoutingPolicy = { RoutingPolicy.PRIVACY_FIRST }) : AiBrain {
    override fun processRequest(request: AiRequest) = route(request)
    override fun understandIntent(request: AiRequest) = route(request)
    override fun generateResponse(request: AiRequest) = route(request)
    override fun planAction(request: AiRequest) = route(request).map { it.actionPlan }
    override fun summarize(request: AiRequest) = route(request.copy(requiredCapabilities = setOf(AiCapability.SUMMARIZATION, AiCapability.TEXT_GENERATION)))
    override fun classify(request: AiRequest) = route(request.copy(requiredCapabilities = setOf(AiCapability.JSON_OUTPUT)))
    override fun extractEntities(request: AiRequest) = route(request.copy(requiredCapabilities = setOf(AiCapability.JSON_OUTPUT))).map { it.entities }
    private fun route(request: AiRequest): Result<AiResponse> {
        if (policy() != RoutingPolicy.CLOUD_FIRST && onDevice.state == OnDeviceAiState.AVAILABLE && onDevice.supports(request)) {
            onDevice.generate(request).getOrNull()?.let { return Result.success(it) }
        }
        return apiManager.generate(request)
    }
}

interface ConversationContextManager {
    fun startSession()
    fun endSession()
    fun clearConversation()
    fun addMessage(message: String)
    fun getContext(): ConversationContext
}

class InMemoryConversationContextManager(private val maxMessages: Int = 12) : ConversationContextManager {
    private var context = ConversationContext(maxMessages = maxMessages)
    override fun startSession() { context = ConversationContext(maxMessages = maxMessages) }
    override fun endSession() { context = ConversationContext(maxMessages = maxMessages) }
    override fun clearConversation() { context = context.copy(recentMessages = emptyList(), currentTask = null, currentAction = null, previousIntent = null, entities = emptyList()) }
    override fun addMessage(message: String) { context = context.copy(recentMessages = (context.recentMessages + message).takeLast(maxMessages.coerceIn(1, 50))) }
    override fun getContext() = context.bounded()
}

interface ActionPlanValidator { fun validate(plan: ActionPlan): Boolean }
class DefaultActionPlanValidator : ActionPlanValidator {
    override fun validate(plan: ActionPlan): Boolean = plan.action.isNotBlank() && plan.requiredCapabilities.distinct().size == plan.requiredCapabilities.size && !(plan.action.startsWith("SEND", true) && !plan.requiresConfirmation)
}
