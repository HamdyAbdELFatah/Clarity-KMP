package com.hamdy.clarity

/**
 * The lifecycle state of a [ClarityClient], reported by [ClarityClient.state].
 *
 * State transitions on a successful run:
 *
 * ```
 * NotInitialized
 *      │  (createClarityClient called; SDK accepts initialization + callback)
 *      ▼
 * InitializationAccepted
 *      │  (platform SDK fires its first session-started callback)
 *      ▼
 * Active  ⇄  Paused        (via pause()/resume(); mutations are accepted here)
 * ```
 *
 * Terminal/non-running states (mutations return `false`):
 * - [Disabled] — [ClarityConfig.enabled] was `false`.
 * - [Unsupported] — the OS is below Clarity's capture floor (Android < 29, iOS < 15).
 * - [Failed] — the platform SDK rejected initialization or callback registration;
 *   [Failed.reason] carries the detail.
 */
public sealed interface ClarityState {
    /** Before [ClarityClient] has been constructed (the initial value). */
    public data object NotInitialized : ClarityState

    /** Clarity is disabled via [ClarityConfig.enabled] = `false`; nothing is recorded. */
    public data object Disabled : ClarityState

    /** The OS is below Clarity's capture floor; the SDK is present but inert. */
    public data object Unsupported : ClarityState

    /**
     * The SDK accepted initialization and is awaiting its first session-started
     * callback. Mutations are not yet accepted in this state.
     */
    public data object InitializationAccepted : ClarityState

    /** A session is running and capture is active; mutations are accepted. */
    public data object Active : ClarityState

    /** Capture is paused via [ClarityClient.pause]; mutations are still accepted. */
    public data object Paused : ClarityState

    /**
     * Initialization failed: the platform SDK rejected initialization or session-callback
     * registration. [reason] describes the failure.
     */
    public data class Failed(val reason: String) : ClarityState
}
