package com.hamdy.clarity.consumer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.hamdy.clarity.ClarityClient
import com.hamdy.clarity.ClarityConsent
import com.hamdy.clarity.ClarityState
import com.hamdy.clarity.compose.ClarityProvider
import com.hamdy.clarity.compose.ClarityScreen
import com.hamdy.clarity.compose.TrackClarityEvent
import com.hamdy.clarity.compose.clarityClickable
import com.hamdy.clarity.compose.clarityTag
import com.hamdy.clarity.compose.rememberClarityState
import com.hamdy.clarity.ifActive
import com.hamdy.clarity.ifReady
import com.hamdy.clarity.isActive
import com.hamdy.clarity.isReady
import com.hamdy.clarity.isTerminal
import com.hamdy.clarity.noOpClarityClient
import com.hamdy.clarity.screen
import com.hamdy.clarity.sendEvents
import com.hamdy.clarity.sessionId
import com.hamdy.clarity.setTags
import com.hamdy.clarity.tag
import com.hamdy.clarity.trackEvent
import com.hamdy.clarity.userId
import com.hamdy.clarity.withScreen

@Composable
fun ConsumerSmoke(client: ClarityClient) {
    val state by rememberClarityState(client)

    // Inspect property getters
    val isClientActive: Boolean = client.isActive
    val isClientReady: Boolean = client.isReady
    val isClientTerminal: Boolean = client.isTerminal
    val currentScreen: String? = client.currentScreenName
    val isPaused: Boolean = client.isPaused
    val isSupported: Boolean = client.isSupported
    val currentState: ClarityState = client.state

    // Fluent API extensions
    client.userId("user_smoke_123")
    client.sessionId("sess_smoke_456")
    client.screen("SmokeScreen")
    client.tag("tier", "gold")
    client.tag("features", setOf("analytics", "heatmaps"))
    client.trackEvent("smoke_event")
    client.sendEvents("event_1", "event_2")
    client.setTags("k1" to "v1", "k2" to "v2")
    client.setTags(mapOf("group" to setOf("admin", "user")))
    client.setConsent(ClarityConsent(analyticsStorage = true, adsStorage = false))
    client.getCurrentSessionUrl()
    client.startNewSession { _ -> }

    client.withScreen("ScopedScreen") {
        // block
    }

    client.ifActive {
        sendCustomEvent("active_event")
    }

    client.ifReady {
        sendCustomEvent("ready_event")
    }

    // Compose Multiplatform wrappers
    ClarityProvider(client) {
        ClarityScreen(name = "ConsumerSmoke", restoreOnExit = true) {
            TrackClarityEvent("smoke_composable_viewed")
            val smokeModifier = Modifier
                .clarityTag("tag_key", "tag_val")
                .clarityClickable("btn_clicked") { }
        }
    }

    val defaultNoOp = noOpClarityClient()
    defaultNoOp.observeState { }
}

