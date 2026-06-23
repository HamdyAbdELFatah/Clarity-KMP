package com.hamdy.clarity

internal class DefaultClarityClient(
    private val config: ClarityConfig,
    private val sdk: ClaritySdkAdapter,
) : ClarityClient {
    private var sessionStartedCallback: (String) -> Unit = {}

    override var state: ClarityState = ClarityState.NotInitialized
        private set

    override val isSupported: Boolean
        get() = sdk.isSupported

    override val isPaused: Boolean
        get() = state == ClarityState.Paused && sdk.isPaused()

    init {
        // Defensive: a well-behaved adapter never throws (it returns false instead), but we must
        // not let a platform SDK exception propagate out of construction and crash the host app.
        // Capture it as a terminal Failed state so the client degrades safely to a no-op.
        val result = runCatching {
            when {
                !config.enabled -> state = ClarityState.Disabled
                !sdk.isSupported -> state = ClarityState.Unsupported
                !sdk.initialize(config.projectId, config.logLevel) -> {
                    state = ClarityState.Failed("The platform SDK rejected initialization.")
                }
                else -> {
                    state = ClarityState.InitializationAccepted
                    if (!sdk.setOnSessionStartedCallback(::onSessionStarted)) {
                        state = ClarityState.Failed("The platform SDK rejected the session callback.")
                    }
                }
            }
        }
        result.onFailure { error ->
            state = ClarityState.Failed("Initialization threw: ${error.message ?: error::class.simpleName}")
        }
    }

    override fun setCustomUserId(value: String): Boolean =
        isOperational() && value.isValidClarityValue(ClarityConfig.MAX_VALUE_LENGTH) && sdk.setCustomUserId(value)

    override fun setCustomSessionId(value: String): Boolean =
        isOperational() && value.isValidClarityValue(ClarityConfig.MAX_VALUE_LENGTH) && sdk.setCustomSessionId(value)

    override fun setCurrentScreenName(value: String?): Boolean =
        isOperational() && (value == null || value.isValidClarityValue(ClarityConfig.MAX_VALUE_LENGTH)) &&
            sdk.setCurrentScreenName(value)

    override fun sendCustomEvent(value: String): Boolean =
        isOperational() && value.isValidClarityValue(ClarityConfig.MAX_EVENT_LENGTH) && sdk.sendCustomEvent(value)

    override fun setCustomTag(key: String, values: Set<String>): Boolean =
        isOperational() && key.isValidClarityValue(ClarityConfig.MAX_VALUE_LENGTH) && values.isNotEmpty() &&
            values.all { it.isValidClarityValue(ClarityConfig.MAX_VALUE_LENGTH) } && sdk.setCustomTag(key, values)

    override fun pause(): Boolean {
        if (state != ClarityState.Active || !sdk.pause()) return false
        state = ClarityState.Paused
        return true
    }

    override fun resume(): Boolean {
        if (state != ClarityState.Paused || !sdk.resume()) return false
        state = ClarityState.Active
        return true
    }

    override fun startNewSession(callback: (String) -> Unit): Boolean {
        if (!isOperational()) return false
        return sdk.startNewSession(callback)
    }

    override fun getCurrentSessionUrl(): String? = if (isOperational()) sdk.getCurrentSessionUrl() else null

    override fun setOnSessionStartedCallback(callback: (String) -> Unit): Boolean {
        if (state == ClarityState.Disabled || state == ClarityState.Unsupported || state is ClarityState.Failed) {
            return false
        }
        sessionStartedCallback = callback
        return true
    }

    override fun setConsent(consent: ClarityConsent): Boolean =
        isOperational() && sdk.setConsent(
            adsStorage = consent.adsStorage ?: false,
            analyticsStorage = consent.analyticsStorage,
        )

    private fun onSessionStarted(sessionId: String) {
        state = ClarityState.Active
        // Config-supplied metadata is re-applied on EVERY new session (including the
        // first and any started via startNewSession). This intentionally overrides any
        // value set at runtime so config stays the source of truth across sessions.
        // Wrapped defensively: a throwing setter must not break session activation or
        // suppress the caller's session-started callback.
        runCatching {
            config.customUserId?.let(sdk::setCustomUserId)
            config.customSessionId?.let(sdk::setCustomSessionId)
            config.customTags.forEach { (key, values) -> sdk.setCustomTag(key, values) }
        }
        sessionStartedCallback(sessionId)
    }

    private fun isOperational(): Boolean = state == ClarityState.Active || state == ClarityState.Paused
}

internal interface ClaritySdkAdapter {
    val isSupported: Boolean
    fun initialize(projectId: String, logLevel: ClarityLogLevel): Boolean
    fun setOnSessionStartedCallback(callback: (String) -> Unit): Boolean
    fun setCustomUserId(value: String): Boolean
    fun setCustomSessionId(value: String): Boolean
    fun setCurrentScreenName(value: String?): Boolean
    fun sendCustomEvent(value: String): Boolean
    fun setCustomTag(key: String, values: Set<String>): Boolean
    fun pause(): Boolean
    fun resume(): Boolean
    fun isPaused(): Boolean
    fun startNewSession(callback: (String) -> Unit): Boolean
    fun getCurrentSessionUrl(): String?
    fun setConsent(adsStorage: Boolean, analyticsStorage: Boolean): Boolean
}
