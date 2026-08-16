package com.hamdy.clarity

internal class DefaultClarityClient(
    private val config: ClarityConfig,
    private val sdk: ClaritySdkAdapter,
    // Defaulted so the platform factories and existing tests don't have to pass it. The real
    // binding (Android/iOS `actual` behind defaultMainThreadAccess()) hops to the platform main
    // thread; tests inject a fake.
    private val mainThreadAccess: MainThreadAccess = defaultMainThreadAccess(),
) : ClarityClient {
    private var sessionStartedCallback: (String) -> Unit = {}

    /**
     * Metadata operations accepted while not yet [ClarityState.Active] (when
     * [ClarityConfig.bufferUntilActive] is enabled). Each entry is a deferred sdk call. The
     * buffer is flushed in [onSessionStarted], **after** config-supplied metadata and **before**
     * the caller's session callback, so runtime calls do not clobber config values mid-flight
     * but are still applied before the caller observes the session.
     *
     * Only idempotent setters land here; see [applyMetadata].
     */
    private val pendingMetadata = mutableListOf<() -> Boolean>()

    /**
     * Registered [StateObserver]s, notified via [updateState] on every state **change**. All
     * mutations of [state] (and thus all reads/writes of this list) happen on the platform main
     * thread — init runs on the constructing thread (main, by contract) and every other transition
     * is routed through [onMain] / [onSessionStarted] — so no concurrency primitive is needed.
     */
    private val observers = mutableListOf<StateObserver>()

    override var currentScreenName: String? = null
        private set

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
                !config.enabled -> updateState(ClarityState.Disabled)
                !sdk.isSupported -> updateState(ClarityState.Unsupported)
                !sdk.initialize(config.projectId, config.logLevel) -> {
                    updateState(ClarityState.Failed("The platform SDK rejected initialization."))
                }
                else -> {
                    updateState(ClarityState.InitializationAccepted)
                    if (!sdk.setOnSessionStartedCallback(::onSessionStarted)) {
                        updateState(ClarityState.Failed("The platform SDK rejected the session callback."))
                    }
                }
            }
        }
        result.onFailure { error ->
            updateState(ClarityState.Failed("Initialization threw: ${error.message ?: error::class.simpleName}"))
        }
    }

    override fun setCustomUserId(value: String): Boolean = onMain(true) {
        applyMetadata(value.isValidClarityValue(ClarityConfig.MAX_VALUE_LENGTH)) { sdk.setCustomUserId(value) }
    }

    override fun setCustomSessionId(value: String): Boolean = onMain(true) {
        applyMetadata(value.isValidClarityValue(ClarityConfig.MAX_VALUE_LENGTH)) { sdk.setCustomSessionId(value) }
    }

    override fun setCurrentScreenName(value: String?): Boolean = onMain(true) {
        val valid = value == null || value.isValidClarityValue(ClarityConfig.MAX_VALUE_LENGTH)
        val applied = applyMetadata(valid) {
            sdk.setCurrentScreenName(value)
        }
        if (applied) {
            currentScreenName = value
        }
        applied
    }

    override fun sendCustomEvent(value: String): Boolean = onMain(true) {
        isOperational() && value.isValidClarityValue(ClarityConfig.MAX_EVENT_LENGTH) && sdk.sendCustomEvent(value)
    }

    override fun setCustomTag(key: String, values: Set<String>): Boolean = onMain(true) {
        applyMetadata(
            key.isValidClarityValue(ClarityConfig.MAX_VALUE_LENGTH) && values.isNotEmpty() &&
                values.all { it.isValidClarityValue(ClarityConfig.MAX_VALUE_LENGTH) }
        ) { sdk.setCustomTag(key, values) }
    }

    /**
     * Runs an idempotent metadata setter when operational, or — when buffering is enabled and
     * the client is still [ClarityState.InitializationAccepted] — defers it to [pendingMetadata]
     * so it is replayed on the next session start. Returns `true` in both the applied and
     * buffered cases (the call was accepted and will take effect); returns `false` only when the
     * input was invalid ([valid] is false) or the client is in a terminal/pre-active state with
     * buffering disabled.
     *
     * Point-in-time calls (events, pause/resume) bypass this and stay subject to [isOperational].
     */
    private fun applyMetadata(valid: Boolean, action: () -> Boolean): Boolean {
        if (!valid) return false
        return when {
            isOperational() -> action()
            config.bufferUntilActive && state == ClarityState.InitializationAccepted -> {
                pendingMetadata += action
                true
            }
            else -> false
        }
    }

    override fun pause(): Boolean = onMain(true) {
        if (state != ClarityState.Active || !sdk.pause()) return@onMain false
        updateState(ClarityState.Paused)
        true
    }

    override fun resume(): Boolean = onMain(true) {
        if (state != ClarityState.Paused || !sdk.resume()) return@onMain false
        updateState(ClarityState.Active)
        true
    }

    override fun startNewSession(callback: (String) -> Unit): Boolean = onMain(true) {
        if (!isOperational()) return@onMain false
        sdk.startNewSession(callback)
    }

    override fun getCurrentSessionUrl(): String? = onMain(null) {
        if (isOperational()) sdk.getCurrentSessionUrl() else null
    }

    override fun setOnSessionStartedCallback(callback: (String) -> Unit): Boolean = onMain(true) {
        if (state == ClarityState.Disabled || state == ClarityState.Unsupported || state is ClarityState.Failed) {
            return@onMain false
        }
        sessionStartedCallback = callback
        true
    }

    override fun setConsent(consent: ClarityConsent): Boolean = onMain(true) {
        isOperational() && sdk.setConsent(
            adsStorage = consent.adsStorage ?: false,
            analyticsStorage = consent.analyticsStorage,
        )
    }

    override fun observeState(observer: StateObserver): ObserverHandle {
        checkMainThread()
        observers.add(observer)
        // Immediate delivery of the current state so a caller can seed its own state (e.g.
        // Compose rememberClarityState) at registration without needing a second read.
        observer.onStateChanged(state)
        // cancel() removes the observer; safe to call multiple times (remove is idempotent) and
        // from within an onStateChanged callback (updateState snapshots before iterating).
        return ObserverHandle {
            checkMainThread()
            observers.remove(observer)
        }
    }

    private fun checkMainThread() {
        if (!mainThreadAccess.isMainThread) {
            throw IllegalStateException("Clarity observers must be registered and cancelled on the platform main thread.")
        }
    }

    private fun onSessionStarted(sessionId: String) {
        updateState(ClarityState.Active)
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
        // Replay any metadata that callers submitted while we were still initializing
        // (bufferUntilActive). Done AFTER config metadata so a config value always wins
        // for the same key on the very first session, and BEFORE the caller's callback so
        // the caller observes the session with all known metadata already applied.
        // Snapshot-then-clear so a setter that (defensively) recurses can't re-enter flush.
        runCatching {
            if (pendingMetadata.isNotEmpty()) {
                val toFlush = pendingMetadata.toList()
                pendingMetadata.clear()
                // Results are intentionally ignored: validation already ran at buffer time,
                // and the SDK's per-call success is not surfaced for deferred metadata.
                toFlush.forEach { runCatching(it) }
            }
        }
        sessionStartedCallback(sessionId)
    }

    /**
     * Runs [block] inline when already on the main thread — the fast path, whose behavior is
     * identical regardless of [ClarityConfig.dispatchStrategy]. Off the main thread the strategy
     * decides: [ClarityDispatchStrategy.EnforceMainThread] throws (fail-fast, the default), while
     * [ClarityDispatchStrategy.DispatchToMain] posts [block] to main asynchronously and returns
     * [default] ("accepted / queued") without blocking or throwing. Because the whole [block] —
     * validation, state mutation, and SDK call — runs on main as one unit, the main thread stays
     * the single mutator and the operation remains thread-safe even under concurrent callers.
     *
     * [default] is therefore the value returned for an off-main call under `DispatchToMain`:
     * `true` for mutating methods (the call was accepted and will be applied on main) and `null`
     * for [getCurrentSessionUrl] (the result is not known synchronously from a background thread).
     */
    private fun <T> onMain(default: T, block: () -> T): T {
        if (mainThreadAccess.isMainThread) return block()
        return when (config.dispatchStrategy) {
            ClarityDispatchStrategy.DispatchToMain -> {
                // Fire-and-forget: run the whole unit on main and drop its result, since the
                // real value is not known synchronously from the calling (background) thread.
                mainThreadAccess.post { block() }
                default
            }
            ClarityDispatchStrategy.EnforceMainThread ->
                throw IllegalStateException("Clarity APIs must be called on the platform main thread.")
        }
    }

    /**
     * The single funnel for every [state] transition. No-ops on same-value reassignments so
     * observers are not spuriously notified, then snapshots [observers] before iterating so a
     * callback that cancels itself or registers another observer cannot corrupt the live list.
     * Each observer is wrapped in [runCatching] so one throwing callback cannot break propagation
     * to the others or abort the SDK flow that triggered the transition. Always runs on the main
     * thread (every caller does), so no synchronization is needed.
     */
    private fun updateState(newState: ClarityState) {
        if (newState == state) return
        state = newState
        observers.toList().forEach { runCatching { it.onStateChanged(newState) } }
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
