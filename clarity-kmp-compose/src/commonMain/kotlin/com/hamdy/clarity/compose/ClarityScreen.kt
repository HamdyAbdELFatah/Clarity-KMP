package com.hamdy.clarity.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.hamdy.clarity.ClarityClient

/**
 * Wraps [content] and automatically tracks [name] as the current screen in Clarity.
 *
 * Use this at the top level of each screen in your navigation:
 *
 * ```kotlin
 * ClarityScreen(name = "Home") {
 *     HomeScreenContent()
 * }
 * ```
 *
 * This is navigation-framework agnostic — it works with any navigation solution.
 *
 * The screen name is reported on enter and, by default, **reset on exit** ([restoreOnExit] = `true`)
 * so the dashboard's "current screen" always reflects the on-screen destination rather than a
 * stale name lingering after navigation away. Pass `restoreOnExit = false` to keep the name set
 * after this composable leaves the composition (the previous sticky behavior).
 *
 * @param name The screen name to report to Clarity.
 * @param restoreOnExit When `true` (default), reset the current screen name (`null`) when this
 *                      composable leaves the composition.
 * @param client The client to report to; defaults to [LocalClarityClient].
 * @param content The composable content of the screen.
 */
@Composable
public fun ClarityScreen(
    name: String,
    restoreOnExit: Boolean = true,
    client: ClarityClient = LocalClarityClient.current,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(name) {
        client.setCurrentScreenName(name)
    }

    // Reset the screen name when this composable leaves the composition so a navigated-away screen
    // does not keep reporting. Deliberately NOT keyed on `name`: an in-place name change is handled
    // by the LaunchedEffect above, and keying this on `name` would dispose (and spuriously reset to
    // null) on every such change. Keyed on `restoreOnExit` so toggling it re-evaluates which branch
    // `onDispose` takes; a change to `client` re-subscribes via the closure capture.
    DisposableEffect(restoreOnExit) {
        onDispose {
            if (restoreOnExit) client.setCurrentScreenName(null)
        }
    }

    content()
}
