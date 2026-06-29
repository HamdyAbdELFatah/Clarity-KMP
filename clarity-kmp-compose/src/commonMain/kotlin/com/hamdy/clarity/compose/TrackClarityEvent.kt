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
 * TrackClarityEvent("onboarding_shown")
 * OnboardingContent()
 * ```
 *
 * The event is sent only once per unique [name] per composition lifecycle: `LaunchedEffect(name)`
 * does not re-run while [name] is unchanged, so recompositions (even state-driven ones) do not
 * re-fire it.
 *
 * @param name The event name to send to Clarity.
 * @param client The client to report to; defaults to [LocalClarityClient].
 */
@Composable
public fun TrackClarityEvent(
    name: String,
    client: ClarityClient = LocalClarityClient.current,
) {
    LaunchedEffect(name, client) {
        client.sendCustomEvent(name)
    }
}
