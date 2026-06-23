package com.hamdy.clarity.consumer

import androidx.compose.runtime.Composable
import com.hamdy.clarity.ClarityClient
import com.hamdy.clarity.compose.ClarityProvider
import com.hamdy.clarity.compose.ClarityScreen

@Composable
fun ConsumerSmoke(client: ClarityClient) {
    ClarityProvider(client) {
        ClarityScreen("ConsumerSmoke") {}
    }
}
