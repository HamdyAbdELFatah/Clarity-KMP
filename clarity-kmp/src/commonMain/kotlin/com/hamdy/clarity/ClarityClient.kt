package com.hamdy.clarity

/**
 * A testable, platform-neutral client for Microsoft Clarity.
 *
 * This is the single entry point shared code uses to drive Clarity on Android
 * and iOS. Construct it with `createClarityClient(...)` (platform-specific) and
 * hold the returned instance for the lifetime of the process (typically on the
 * `Application` on Android or before the UI is created on iOS).
 *
 * ### Threading
 * Every member — reads and writes — **must be called on the platform main
 * thread** (the Android UI thread / the iOS main thread). Off-main calls throw.
 *
 * ### Return values
 * Mutating methods return `true` only when the call was applied. They return
 * `false` (and never throw) when the client is not in an operational state
 * (see [state]) or when an argument fails validation — callers that want to
 * react should check the result.
 *
 * ### State
 * Mutating calls take effect only while [state] is [ClarityState.Active] or
 * [ClarityState.Paused]. See [state] and [ClarityState] for the lifecycle.
 */
public interface ClarityClient {
    /**
     * The current lifecycle state. Reads must occur on the main thread.
     *
     * Use this to inspect readiness before/after initialization: the client
     * progresses through [ClarityState.NotInitialized] →
     * [ClarityState.InitializationAccepted] → [ClarityState.Active] on a
     * successful start, and reports [ClarityState.Disabled],
     * [ClarityState.Unsupported], or [ClarityState.Failed] when it cannot run.
     */
    public val state: ClarityState

    /**
     * `true` when the platform/OS meets Clarity's capture floor (Android API 29+,
     * iOS 15+). When `false`, the SDK is present but records nothing and [state]
     * is [ClarityState.Unsupported]. Reads must occur on the main thread.
     */
    public val isSupported: Boolean

    /**
     * `true` while capture is paused via [pause]. Reads must occur on the main thread.
     */
    public val isPaused: Boolean

    /**
     * Associates the current user with a stable identifier so sessions can be
     * stitched across devices/sessions.
     *
     * @param value a non-blank identifier, at most [ClarityConfig.MAX_VALUE_LENGTH] (255) chars.
     * @return `true` if applied; `false` if blank/over-length or the client is not active/paused.
     */
    public fun setCustomUserId(value: String): Boolean

    /**
     * Overrides the auto-generated session id with a caller-supplied one.
     *
     * @param value a non-blank identifier, at most [ClarityConfig.MAX_VALUE_LENGTH] (255) chars.
     * @return `true` if applied; `false` if blank/over-length or the client is not active/paused.
     */
    public fun setCustomSessionId(value: String): Boolean

    /**
     * Records the current screen/view name so it appears in Clarity dashboards.
     *
     * @param value the screen name (max [ClarityConfig.MAX_VALUE_LENGTH] / 255 chars),
     *             or `null` to reset/clear the current screen name.
     * @return `true` if applied; `false` if non-null and over-length, or the client is not active/paused.
     */
    public fun setCurrentScreenName(value: String?): Boolean

    /**
     * Sends a custom event to be attached to the current session.
     *
     * @param value a non-blank event name, at most [ClarityConfig.MAX_EVENT_LENGTH] (254) chars.
     * @return `true` if sent; `false` if blank/over-length or the client is not active/paused.
     */
    public fun sendCustomEvent(value: String): Boolean

    /**
     * Attaches a single-value custom tag to the current session.
     *
     * @param key a non-blank tag key (max [ClarityConfig.MAX_VALUE_LENGTH] / 255 chars).
     * @param value a non-blank tag value (max [ClarityConfig.MAX_VALUE_LENGTH] / 255 chars).
     * @return `true` if applied; `false` if blank/over-length or the client is not active/paused.
     */
    public fun setCustomTag(key: String, value: String): Boolean = setCustomTag(key, setOf(value))

    /**
     * Attaches a multi-value custom tag to the current session.
     *
     * @param key a non-blank tag key (max [ClarityConfig.MAX_VALUE_LENGTH] / 255 chars).
     * @param values a non-empty set of non-blank values (each max 255 chars).
     * @return `true` if applied; `false` if blank/empty/over-length or the client is not active/paused.
     */
    public fun setCustomTag(key: String, values: Set<String>): Boolean

    /**
     * Temporarily pauses session capture. Resume with [resume].
     *
     * @return `true` if capture is now paused.
     */
    public fun pause(): Boolean

    /**
     * Resumes capture previously paused with [pause].
     *
     * @return `true` if capture is now resumed.
     */
    public fun resume(): Boolean

    /**
     * Ends the current session and starts a fresh one.
     *
     * @param callback invoked on the main thread with the new session id when it
     *                 starts; defaults to a no-op. Note that config-supplied
     *                 user/session ids and tags are re-applied on each new session.
     * @return `true` if a new session was requested; `false` if the client is not active/paused.
     */
    public fun startNewSession(callback: (String) -> Unit = {}): Boolean

    /**
     * @return the URL of the current session's replay in the Clarity dashboard,
     *         or `null` if no session is active yet.
     */
    public fun getCurrentSessionUrl(): String?

    /**
     * Registers a callback fired whenever a new session starts (including the
     * first one). Config-supplied metadata is applied before this callback runs.
     *
     * Replacing the callback is allowed in any non-terminal state; this is the
     * one setter that does not require an active/paused session.
     *
     * @return `true` if registered; `false` if the client is disabled/unsupported/failed
     *         or the platform SDK rejected the registration.
     */
    public fun setOnSessionStartedCallback(callback: (String) -> Unit): Boolean

    /**
     * Applies the user's consent preferences (GDPR). On Android both analytics
     * and ads storage are honored; on iOS only analytics storage is applied
     * (see [ClarityConsent.adsStorage]).
     *
     * @return `true` if applied; `false` if the client is not active/paused.
     */
    public fun setConsent(consent: ClarityConsent): Boolean
}

/**
 * Returns a client that never initializes or records data.
 *
 * Useful as a safe default for Compose previews, tests, or when Clarity is
 * disabled at build/runtime. It reports [ClarityState.Disabled] and returns
 * `false`/`null` from every method.
 */
public fun noOpClarityClient(): ClarityClient = NoOpClarityClient

private object NoOpClarityClient : ClarityClient {
    override val state: ClarityState = ClarityState.Disabled
    override val isSupported: Boolean = false
    override val isPaused: Boolean = false
    override fun setCustomUserId(value: String): Boolean = false
    override fun setCustomSessionId(value: String): Boolean = false
    override fun setCurrentScreenName(value: String?): Boolean = false
    override fun sendCustomEvent(value: String): Boolean = false
    override fun setCustomTag(key: String, values: Set<String>): Boolean = false
    override fun pause(): Boolean = false
    override fun resume(): Boolean = false
    override fun startNewSession(callback: (String) -> Unit): Boolean = false
    override fun getCurrentSessionUrl(): String? = null
    override fun setOnSessionStartedCallback(callback: (String) -> Unit): Boolean = false
    override fun setConsent(consent: ClarityConsent): Boolean = false
}
