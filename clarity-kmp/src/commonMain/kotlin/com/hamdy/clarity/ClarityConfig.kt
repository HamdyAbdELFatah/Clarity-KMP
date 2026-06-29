package com.hamdy.clarity

/**
 * Configuration used by the platform-specific Clarity client factory
 * (`createClarityClient`).
 *
 * Values supplied here for [customUserId], [customSessionId], and [customTags]
 * are validated at construction and **re-applied on every new session** (see
 * [ClarityClient.startNewSession]); they will therefore override any value set
 * at runtime via [ClarityClient.setCustomUserId] / [setCustomSessionId] /
 * [setCustomTag] the next time a session starts.
 *
 * Construction validates all supplied values against Microsoft's length limits
 * ([MAX_VALUE_LENGTH]) and throws [IllegalArgumentException] on any violation,
 * so misconfiguration fails fast at startup rather than being silently dropped.
 *
 * @param projectId the Clarity project id from your Clarity dashboard. Required
 *                 (non-blank) when [enabled]; ignored otherwise.
 * @param enabled `false` to construct an inert client that records nothing
 *                (handy for debug builds). Defaults to `true`.
 * @param logLevel SDK log verbosity. Defaults to [ClarityLogLevel.None].
 * @param customUserId optional stable user id, validated to ≤ [MAX_VALUE_LENGTH] chars.
 * @param customSessionId optional session id, validated to ≤ [MAX_VALUE_LENGTH] chars.
 * @param customTags optional tags applied on session start; keys and each value
 *                   validated to ≤ [MAX_VALUE_LENGTH] chars and value-sets must be non-empty.
 * @param bufferUntilActive when `true` (the default), idempotent metadata calls made before
 *                the first session becomes [ClarityState.Active] — i.e. while the client is
 *                [ClarityState.InitializationAccepted] — are **buffered and replayed** on the
 *                next session start, instead of being silently dropped. This prevents the
 *                common data-loss footgun of tagging/identifying a user before Clarity's
 *                async initialization has finished. Only idempotent setters are buffered
 *                ([ClarityClient.setCustomUserId], [ClarityClient.setCustomSessionId],
 *                [ClarityClient.setCustomTag], [ClarityClient.setCurrentScreenName]);
 *                point-in-time calls ([ClarityClient.sendCustomEvent], pause/resume) are
 *                never buffered. Set `false` to restore the original drop-on-the-floor
 *                behavior (these calls return `false` when not yet active).
 * @param dispatchStrategy how calls made **off the platform main thread** are handled. The
 *                default [ClarityDispatchStrategy.EnforceMainThread] preserves the strict,
 *                fail-fast contract: any off-main mutating call throws [IllegalStateException]
 *                (use this when your code is already main-thread-bound, e.g. Compose). Opt
 *                into [ClarityDispatchStrategy.DispatchToMain] to make off-main calls safe:
 *                the whole operation (validation + state mutation + SDK call) is posted to the
 *                main thread and the call returns `true` ("accepted, will be applied") instead
 *                of blocking or throwing. Under `DispatchToMain` a call made **on** the main
 *                thread behaves exactly as under `EnforceMainThread` (synchronous, real result).
 *                Reads that cannot be satisfied synchronously from a background thread degrade
 *                gracefully: [ClarityClient.getCurrentSessionUrl] returns `null` when off-main.
 *                The strategy is advisory for [ClarityClient.state]/[ClarityClient.isSupported]/
 *                [ClarityClient.isPaused], which stay cheap best-effort reads.
 */
public data class ClarityConfig(
    val projectId: String,
    val enabled: Boolean = true,
    val logLevel: ClarityLogLevel = ClarityLogLevel.None,
    val customUserId: String? = null,
    val customSessionId: String? = null,
    val customTags: Map<String, Set<String>> = emptyMap(),
    val bufferUntilActive: Boolean = true,
    val dispatchStrategy: ClarityDispatchStrategy = ClarityDispatchStrategy.EnforceMainThread,
) {
    init {
        require(!enabled || projectId.isNotBlank()) { "projectId must not be blank when Clarity is enabled." }
        customUserId?.let { requireValid(it, MAX_VALUE_LENGTH, "customUserId") }
        customSessionId?.let { requireValid(it, MAX_VALUE_LENGTH, "customSessionId") }
        customTags.forEach { (key, values) ->
            requireValid(key, MAX_VALUE_LENGTH, "tag key")
            require(values.isNotEmpty()) { "tag values must not be empty." }
            values.forEach { requireValid(it, MAX_VALUE_LENGTH, "tag value") }
        }
    }

    public companion object {
        /**
         * Maximum character length enforced for project/user/session ids, tag keys,
         * tag values, and screen names. Matches Microsoft Clarity's documented limit.
         */
        public const val MAX_VALUE_LENGTH: Int = 255

        /**
         * Maximum character length enforced for custom event names.
         *
         * Note this is **254**, one less than [MAX_VALUE_LENGTH]: Microsoft Clarity
         * reserves one character for event names, so the event cap is 254 while all
         * other string fields allow 255.
         */
        public const val MAX_EVENT_LENGTH: Int = 254
    }
}

/**
 * User consent preferences passed to [ClarityClient.setConsent] (GDPR).
 *
 * @param analyticsStorage whether analytics/session data may be stored. Honored on
 *                         both Android and iOS.
 * @param adsStorage whether advertising-storage consent is granted. **Android only** —
 *                   ignored on iOS, where the Clarity SDK exposes analytics-storage
 *                   consent alone. `null` (the default) leaves ads storage untouched.
 */
public data class ClarityConsent(
    val analyticsStorage: Boolean,
    val adsStorage: Boolean? = null,
)

/**
 * SDK log verbosity, in increasing order of detail.
 * Maps 1:1 to the native Clarity SDK log levels on each platform.
 */
public enum class ClarityLogLevel { None, Error, Warning, Info, Debug, Verbose }

/**
 * How [ClarityClient] handles a call made **off the platform main thread**.
 *
 * Every Clarity mutating call ultimately runs on the platform main thread regardless of
 * strategy; this controls what happens when *you* call from another thread.
 *
 * - [EnforceMainThread] — the safe default: an off-main call throws [IllegalStateException]
 *   so misuse is caught early. Use this when your analytics calls are already main-thread
 *   bound (e.g. invoked from Compose or the UI layer).
 * - [DispatchToMain] — off-main calls are accepted and posted to the main thread for you.
 *   A mutating call returns `true` ("accepted / queued") rather than blocking or throwing,
 *   and the whole operation runs on main as a unit so main stays the single mutator.
 *
 * Under [DispatchToMain], a call made **on** the main thread is identical to
 * [EnforceMainThread]: synchronous, with its real result. See [ClarityConfig.dispatchStrategy].
 */
public enum class ClarityDispatchStrategy {
    /** Off-main calls throw [IllegalStateException]. The default, fail-fast behavior. */
    EnforceMainThread,

    /**
     * Off-main mutating calls are posted to the main thread and return `true` (queued);
     * they never block or throw. `getCurrentSessionUrl()` returns `null` when off-main.
     */
    DispatchToMain,
}

internal fun String.isValidClarityValue(maxLength: Int): Boolean = isNotBlank() && length <= maxLength

private fun requireValid(value: String, maxLength: Int, name: String) {
    require(value.isValidClarityValue(maxLength)) {
        "$name must not be blank and must contain at most $maxLength characters."
    }
}
