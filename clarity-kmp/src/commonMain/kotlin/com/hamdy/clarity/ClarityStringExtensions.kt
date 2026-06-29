package com.hamdy.clarity

/**
 * String validation and sanitization helpers that mirror Microsoft's Clarity length rules.
 *
 * Use these to pre-validate user input before sending it to [ClarityClient], or to safely
 * truncate long strings so they are accepted rather than rejected.
 */

/**
 * Returns `true` if this string is non-blank and its length is at most
 * [ClarityConfig.MAX_VALUE_LENGTH] (255).
 */
public fun String.isValidClarityValue(): Boolean =
    isNotBlank() && length <= ClarityConfig.MAX_VALUE_LENGTH

/**
 * Returns `true` if this string is non-blank and its length is at most
 * [ClarityConfig.MAX_EVENT_LENGTH] (254).
 */
public fun String.isValidClarityEventName(): Boolean =
    isNotBlank() && length <= ClarityConfig.MAX_EVENT_LENGTH

/**
 * Returns a copy of this string truncated to [ClarityConfig.MAX_VALUE_LENGTH] characters.
 *
 * Note: blank strings remain blank; callers should still validate with [isValidClarityValue]
 * before sending them to Clarity.
 */
public fun String.truncatedToClarityValue(): String =
    take(ClarityConfig.MAX_VALUE_LENGTH)

/**
 * Returns a copy of this string truncated to [ClarityConfig.MAX_EVENT_LENGTH] characters.
 *
 * Note: blank strings remain blank; callers should still validate with [isValidClarityEventName]
 * before sending them to Clarity.
 */
public fun String.truncatedToClarityEventName(): String =
    take(ClarityConfig.MAX_EVENT_LENGTH)
