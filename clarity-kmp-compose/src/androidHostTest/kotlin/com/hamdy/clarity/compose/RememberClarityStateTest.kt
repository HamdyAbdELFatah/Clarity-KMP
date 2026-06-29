package com.hamdy.clarity.compose

import android.os.Build
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.v2.runComposeUiTest
import com.hamdy.clarity.ClarityState
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Compose-runtime tests for [rememberClarityState]. Run on the Android JVM via Robolectric so the
 * underlying [androidx.compose.runtime.DisposableEffect] / [androidx.compose.runtime.State] machinery
 * executes for real against [RecordingClarityClient]'s observer-aware test seam.
 */
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM]) // SDK 35 — Robolectric 4.15 supports up to 35
class RememberClarityStateTest {

    @Test
    fun seedsWithCurrentStateOnFirstComposition() = runComposeUiTest {
        val client = RecordingClarityClient(initialState = ClarityState.InitializationAccepted)
        val observed = mutableListOf<ClarityState>()
        setContent {
            ClarityProvider(client = client) {
                val state by rememberClarityState()
                // Capture every value the State takes on, in order.
                LaunchedEffect(state) { observed += state }
            }
        }
        waitForIdle()

        assertEquals(listOf<ClarityState>(ClarityState.InitializationAccepted), observed)
    }

    @Test
    fun updatesWhenClientTransitions() = runComposeUiTest {
        val client = RecordingClarityClient(initialState = ClarityState.InitializationAccepted)
        val observed = mutableListOf<ClarityState>()
        setContent {
            ClarityProvider(client = client) {
                val state by rememberClarityState()
                LaunchedEffect(state) { observed += state }
            }
        }
        waitForIdle()
        assertEquals(listOf<ClarityState>(ClarityState.InitializationAccepted), observed)

        // A real transition drives the observer → the State → recomposition.
        client.setStateForTest(ClarityState.Active)
        waitForIdle()

        assertEquals(
            listOf<ClarityState>(ClarityState.InitializationAccepted, ClarityState.Active),
            observed,
        )
    }

    @Test
    fun stopsUpdatingAfterCompositionDisposes() = runComposeUiTest {
        val client = RecordingClarityClient(initialState = ClarityState.Active)
        var visible by mutableStateOf(true)
        val observed = mutableListOf<ClarityState>()
        setContent {
            ClarityProvider(client = client) {
                if (visible) {
                    val state by rememberClarityState()
                    LaunchedEffect(state) { observed += state }
                }
            }
        }
        waitForIdle()
        assertEquals(listOf<ClarityState>(ClarityState.Active), observed)

        visible = false // rememberClarityState disposes → observer.cancel()
        waitForIdle()

        // A transition after disposal must NOT reach the disposed observer (no leak).
        client.setStateForTest(ClarityState.Paused)
        waitForIdle()

        assertEquals(listOf<ClarityState>(ClarityState.Active), observed)
    }
}
