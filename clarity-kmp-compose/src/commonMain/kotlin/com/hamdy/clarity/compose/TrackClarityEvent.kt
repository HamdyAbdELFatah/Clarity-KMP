package com.hamdy.clarity.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.hamdy.clarity.ClarityClient

/**
 * Tracks a Clarity event once when this composable first enters the composition.
 *
 * Useful for tracking screen views or feature impressions:
 *
 * ```kotlin
 * TrackClarityEventOnFirstComposition("onboarding_shown")
 * OnboardingContent()
 * ```
 *
 * The event is sent only once per unique [eventName] per composition lifecycle.
 *
 * @param eventName The event name to send to Clarity.
 */
@Composable
public fun TrackClarityEventOnFirstComposition(
    eventName: String,
    client: ClarityClient = LocalClarityClient.current,
) {
    LaunchedEffect(eventName) {
        client.sendCustomEvent(eventName)
    }
}
