package com.hamdy.clarity.compose

import androidx.compose.runtime.Composable
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
 * @param name The screen name to report to Clarity.
 * @param content The composable content of the screen.
 */
@Composable
public fun ClarityScreen(
    name: String,
    client: ClarityClient = LocalClarityClient.current,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(name) {
        client.setCurrentScreenName(name)
    }

    content()
}
