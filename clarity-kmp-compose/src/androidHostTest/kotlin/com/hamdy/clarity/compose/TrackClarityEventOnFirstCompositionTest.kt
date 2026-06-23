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
 * Compose-runtime tests for [TrackClarityEventOnFirstComposition]. Run on the Android JVM via
 * Robolectric so the underlying [androidx.compose.runtime.LaunchedEffect] executes for real.
 */
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM]) // SDK 35 — Robolectric 4.15 supports up to 35
class TrackClarityEventOnFirstCompositionTest {

    @Test
    fun sendsEventOnceOnFirstComposition() = runComposeUiTest {
        val client = RecordingClarityClient()
        setContent {
            ClarityProvider(client = client) {
                TrackClarityEventOnFirstComposition("onboarding_shown")
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
                TrackClarityEventOnFirstComposition("feature_impression")
            }
        }
        waitForIdle()
        assertEquals(listOf("event:feature_impression"), client.calls)

        tick = 1
        waitForIdle()
        // Still only one event: LaunchedEffect(key1 = eventName) does not re-run when eventName is unchanged.
        assertEquals(listOf("event:feature_impression"), client.calls)
    }
}
