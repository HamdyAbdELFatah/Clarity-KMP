package com.hamdy.clarity.compose

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.hamdy.clarity.ClarityClient

/**
 * A [Modifier] that tracks a Clarity event when the element is clicked,
 * then executes [onClick].
 *
 * ```kotlin
 * Box(
 *     modifier = Modifier.clarityClickable("cta_clicked") {
 *         navigateToDetails()
 *     }
 * )
 * ```
 *
 * @param eventName The event name to send to Clarity on click.
 * @param onClick The action to perform after tracking the event.
 */
@Composable
public fun Modifier.clarityClickable(
    eventName: String,
    client: ClarityClient = LocalClarityClient.current,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier = this.clickable(
    enabled = enabled,
    onClickLabel = onClickLabel,
    role = role,
) { performTrackedClick(client, eventName, onClick) }

internal fun performTrackedClick(
    client: ClarityClient,
    eventName: String,
    onClick: () -> Unit,
) {
    client.sendCustomEvent(eventName)
    onClick()
}
