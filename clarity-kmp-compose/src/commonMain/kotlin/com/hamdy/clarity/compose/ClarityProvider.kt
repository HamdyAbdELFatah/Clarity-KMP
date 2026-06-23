package com.hamdy.clarity.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.hamdy.clarity.ClarityClient
import com.hamdy.clarity.noOpClarityClient

public val LocalClarityClient: ProvidableCompositionLocal<ClarityClient> =
    staticCompositionLocalOf { noOpClarityClient() }

@Composable
public fun ClarityProvider(
    client: ClarityClient,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalClarityClient provides client, content = content)
}
