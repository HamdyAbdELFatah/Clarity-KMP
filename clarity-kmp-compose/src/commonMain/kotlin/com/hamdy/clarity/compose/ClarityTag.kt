package com.hamdy.clarity.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.hamdy.clarity.ClarityClient

/**
 * A [Modifier] that tags the current Clarity session with the single-value tag
 * `[key] = [value]` when the node enters the composition (and re-applies it whenever
 * [key] or [value] change).
 *
 * ```kotlin
 * Box(
 *     modifier = Modifier
 *         .clarityTag("plan", "premium")
 *         .clarityClickable("cta_clicked") { navigate() }
 * )
 * ```
 *
 * Clarity exposes no remove-tag API, so the tag is only **applied** — it cannot be unset
 * when this node leaves the composition. The single-value overload is mirrored from
 * [ClarityClient.setCustomTag]; for multi-value tags, call
 * `client.setCustomTag(key, setOf(...))` directly.
 *
 * @param key a non-blank tag key (max 255 chars).
 * @param value a non-blank tag value (max 255 chars).
 * @param client the client to tag through; defaults to [LocalClarityClient].
 */
@Composable
public fun Modifier.clarityTag(
    key: String,
    value: String,
    client: ClarityClient = LocalClarityClient.current,
): Modifier = composed {
    LaunchedEffect(key, value, client) {
        client.setCustomTag(key, value)
    }
    this
}
