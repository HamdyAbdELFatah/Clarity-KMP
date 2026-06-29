package com.hamdy.clarity

/**
 * Convenience state-checking extensions and fluent aliases for [ClarityClient].
 *
 * These helpers reduce boilerplate in shared Kotlin code and make analytics calls
 * read more naturally, while preserving the library's strict main-thread and
 * return-value contracts.
 */

/**
 * `true` when the client is in [ClarityState.Active].
 */
public inline val ClarityClient.isActive: Boolean
    get() = state == ClarityState.Active

/**
 * `true` when the client is operational — i.e. [ClarityState.Active] or [ClarityState.Paused].
 * Mutating calls are accepted in both states.
 */
public inline val ClarityClient.isReady: Boolean
    get() = state == ClarityState.Active || state == ClarityState.Paused

/**
 * `true` when the client has reached a terminal, non-recoverable state:
 * [ClarityState.Disabled], [ClarityState.Unsupported], or [ClarityState.Failed].
 */
public inline val ClarityClient.isTerminal: Boolean
    get() = state is ClarityState.Disabled || state is ClarityState.Unsupported || state is ClarityState.Failed

/**
 * Fluent alias for [ClarityClient.sendCustomEvent].
 *
 * ```kotlin
 * clarity.trackEvent("purchase_completed")
 * ```
 */
public fun ClarityClient.trackEvent(name: String): Boolean = sendCustomEvent(name)

/**
 * Fluent alias for [ClarityClient.setCustomUserId].
 *
 * ```kotlin
 * clarity.userId("user_123")
 * ```
 */
public fun ClarityClient.userId(value: String): Boolean = setCustomUserId(value)

/**
 * Fluent alias for [ClarityClient.setCustomSessionId].
 *
 * ```kotlin
 * clarity.sessionId("checkout-42")
 * ```
 */
public fun ClarityClient.sessionId(value: String): Boolean = setCustomSessionId(value)

/**
 * Fluent alias for [ClarityClient.setCurrentScreenName].
 *
 * ```kotlin
 * clarity.screen("Checkout")
 * ```
 */
public fun ClarityClient.screen(name: String?): Boolean = setCurrentScreenName(name)

/**
 * Fluent alias for [ClarityClient.setCustomTag] with a single value.
 *
 * ```kotlin
 * clarity.tag("plan", "premium")
 * ```
 */
public fun ClarityClient.tag(key: String, value: String): Boolean = setCustomTag(key, value)

/**
 * Fluent alias for [ClarityClient.setCustomTag] with multiple values.
 *
 * ```kotlin
 * clarity.tag("plan", setOf("premium", "annual"))
 * ```
 */
public fun ClarityClient.tag(key: String, values: Set<String>): Boolean = setCustomTag(key, values)

/**
 * Sends multiple custom events in order.
 *
 * All events are attempted even if an earlier one returns `false`. The result is `true`
 * only if every event was accepted.
 */
public fun ClarityClient.sendEvents(vararg events: String): Boolean {
    var allAccepted = true
    events.forEach { allAccepted = sendCustomEvent(it) && allAccepted }
    return allAccepted
}

/**
 * Sets multiple single-value tags at once.
 *
 * All tags are attempted even if an earlier one returns `false`. The result is `true`
 * only if every tag was accepted.
 */
public fun ClarityClient.setTags(vararg tags: Pair<String, String>): Boolean {
    var allAccepted = true
    tags.forEach { (key, value) ->
        allAccepted = setCustomTag(key, value) && allAccepted
    }
    return allAccepted
}

/**
 * Sets multiple multi-value tags at once.
 *
 * All tags are attempted even if an earlier one returns `false`. The result is `true`
 * only if every tag was accepted.
 */
public fun ClarityClient.setTags(tags: Map<String, Set<String>>): Boolean {
    var allAccepted = true
    tags.forEach { (key, values) ->
        allAccepted = setCustomTag(key, values) && allAccepted
    }
    return allAccepted
}

/**
 * Executes [block] with the current screen name set to [name], then resets it to `null`
 * afterwards (even if [block] throws).
 *
 * Useful in non-Compose code, ViewModels, or screens where
 * [com.hamdy.clarity.compose.ClarityScreen] is not available.
 *
 * ```kotlin
 * clarity.withScreen("Checkout") {
 *     // ... run checkout flow
 * }
 * ```
 */
public inline fun <R> ClarityClient.withScreen(name: String, block: () -> R): R {
    setCurrentScreenName(name)
    return try {
        block()
    } finally {
        setCurrentScreenName(null)
    }
}

/**
 * Runs [block] only when [isActive] is `true`.
 *
 * @return `true` if the block ran, `false` otherwise.
 */
public inline fun ClarityClient.ifActive(block: ClarityClient.() -> Unit): Boolean {
    if (isActive) {
        block()
        return true
    }
    return false
}

/**
 * Runs [block] only when [isReady] is `true`.
 *
 * @return `true` if the block ran, `false` otherwise.
 */
public inline fun ClarityClient.ifReady(block: ClarityClient.() -> Unit): Boolean {
    if (isReady) {
        block()
        return true
    }
    return false
}
