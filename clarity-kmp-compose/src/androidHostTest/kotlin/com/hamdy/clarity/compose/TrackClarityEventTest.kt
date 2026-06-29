package com.hamdy.clarity.compose

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.v2.runComposeUiTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Compose-runtime tests for [TrackClarityEvent]. Run on the Android JVM via Robolectric so the
 * underlying [androidx.compose.runtime.LaunchedEffect] executes for real.
 */
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM]) // SDK 35 — Robolectric 4.15 supports up to 35
class TrackClarityEventTest {

    @Test
    fun sendsEventOnceOnFirstComposition() = runComposeUiTest {
        val client = RecordingClarityClient()
        setContent {
            ClarityProvider(client = client) {
                TrackClarityEvent("onboarding_shown")
            }
        }
        waitForIdle()

        assertEquals(listOf("event:onboarding_shown"), client.calls)
    }

    @Test
    fun doesNotResendEventOnRecomposition() = runComposeUiTest {
        val client = RecordingClarityClient()
        var tick by mutableStateOf(0)
        setContent {
            ClarityProvider(client = client) {
                // Reading tick forces recomposition when it changes.
                tick
                TrackClarityEvent("feature_impression")
            }
        }
        waitForIdle()
        assertEquals(listOf("event:feature_impression"), client.calls)

        tick = 1
        waitForIdle()
        // Still only one event: LaunchedEffect(key1 = name) does not re-run when name is unchanged.
        assertEquals(listOf("event:feature_impression"), client.calls)
    }

    @Test
    fun resendEventWhenNameChanges() = runComposeUiTest {
        val client = RecordingClarityClient()
        var name by mutableStateOf("step_a")
        setContent {
            ClarityProvider(client = client) {
                TrackClarityEvent(name)
            }
        }
        waitForIdle()
        assertEquals(listOf("event:step_a"), client.calls)

        name = "step_b"
        waitForIdle()
        // LaunchedEffect(key1 = name) re-runs when name changes, sending the new event.
        assertEquals(listOf("event:step_a", "event:step_b"), client.calls)
    }

    @Test
    fun resendsEventWhenClientChanges() = runComposeUiTest {
        val clientA = RecordingClarityClient()
        val clientB = RecordingClarityClient()
        var currentClient by mutableStateOf<RecordingClarityClient>(clientA)
        setContent {
            TrackClarityEvent("event_a", client = currentClient)
        }
        waitForIdle()
        assertEquals(listOf("event:event_a"), clientA.calls)
        assertEquals(emptyList(), clientB.calls)

        currentClient = clientB
        waitForIdle()
        // LaunchedEffect(key1 = name, key2 = client) re-runs when client changes, sending it to clientB.
        assertEquals(listOf("event:event_a"), clientA.calls)
        assertEquals(listOf("event:event_a"), clientB.calls)
    }
}
