package com.hamdy.clarity.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.hamdy.clarity.ClarityClient
import com.hamdy.clarity.ClarityState

/**
 * Subscribes to [ClarityClient.state] and returns it as reactive Compose [State], so a
 * composable can re-render as the client moves through its lifecycle
 * (`NotInitialized → InitializationAccepted → Active ⇄ Paused`, or a terminal state).
 *
 * The returned [State] is seeded synchronously with the current state on first composition,
 * then updated on every transition by way of [ClarityClient.observeState]. The observer is
 * cancelled automatically on disposal (or when [client] changes), so there is no leak.
 *
 * ```kotlin
 * val state by rememberClarityState()
 * when (state) {
 *     ClarityState.Active -> Text("Recording")
 *     else -> Text("Not recording")
 * }
 * ```
 *
 * @param client the client to observe; defaults to [LocalClarityClient].
 */
@Composable
public fun rememberClarityState(
    client: ClarityClient = LocalClarityClient.current,
): State<ClarityState> {
    // Seed synchronously with the current state so the very first frame is correct. The
    // observer's immediate delivery (below) will re-assign the same value, which is a harmless
    // no-op for Compose's equality check.
    val state = remember(client) { mutableStateOf(client.state) }
    DisposableEffect(client) {
        val handle = client.observeState { state.value = it }
        onDispose { handle.cancel() }
    }
    return state
}
