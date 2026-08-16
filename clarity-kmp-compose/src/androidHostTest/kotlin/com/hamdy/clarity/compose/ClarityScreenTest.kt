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
 * Compose-runtime tests for [ClarityScreen]. These run on the Android JVM via Robolectric,
 * which is the only way to exercise a real [androidx.compose.runtime.LaunchedEffect] from a
 * unit test (mirrors the Robolectric setup already used in the :clarity-kmp module).
 */
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM]) // SDK 35 — Robolectric 4.15 supports up to 35
class ClarityScreenTest {

    @Test
    fun reportsScreenNameOnComposition() = runComposeUiTest {
        val client = RecordingClarityClient()
        setContent {
            ClarityProvider(client = client) {
                ClarityScreen(name = "Home") { }
            }
        }
        waitForIdle()

        assertEquals(listOf("screen:Home"), client.calls)
    }

    @Test
    fun reReportsOnlyWhenNameChanges() = runComposeUiTest {
        val client = RecordingClarityClient()
        var name by mutableStateOf("Home")
        setContent {
            ClarityProvider(client = client) {
                ClarityScreen(name = name) { }
            }
        }
        waitForIdle()
        assertEquals(listOf("screen:Home"), client.calls)

        name = "Settings"
        waitForIdle()
        assertEquals(listOf("screen:Home", "screen:Settings"), client.calls)
    }

    @Test
    fun clearsScreenNameOnExitWhenRestoreOnExit() = runComposeUiTest {
        // restoreOnExit = true (default): dropping the screen from the composition resets the
        // reported name to null so a navigated-away screen stops reporting.
        val client = RecordingClarityClient()
        var visible by mutableStateOf(true)
        setContent {
            ClarityProvider(client = client) {
                if (visible) ClarityScreen(name = "Home") { }
            }
        }
        waitForIdle()
        assertEquals(listOf("screen:Home"), client.calls)

        visible = false // ClarityScreen leaves the composition → DisposableEffect disposes.
        waitForIdle()

        assertEquals(listOf("screen:Home", "screen:null"), client.calls)
    }

    @Test
    fun keepsScreenNameWhenRestoreOnExitFalse() = runComposeUiTest {
        // restoreOnExit = false: preserves the previous sticky behavior — no reset on exit.
        val client = RecordingClarityClient()
        var visible by mutableStateOf(true)
        setContent {
            ClarityProvider(client = client) {
                if (visible) ClarityScreen(name = "Home", restoreOnExit = false) { }
            }
        }
        waitForIdle()
        assertEquals(listOf("screen:Home"), client.calls)

        visible = false
        waitForIdle()

        // No screen:null reset: the name stays set after the screen leaves the composition.
        assertEquals(listOf("screen:Home"), client.calls)
    }

    @Test
    fun updatesScreenNameWhenClientChanges() = runComposeUiTest {
        val clientA = RecordingClarityClient()
        val clientB = RecordingClarityClient()
        var currentClient by mutableStateOf<RecordingClarityClient>(clientA)
        setContent {
            ClarityScreen(name = "Home", restoreOnExit = true, client = currentClient) { }
        }
        waitForIdle()
        assertEquals(listOf("screen:Home"), clientA.calls)
        assertEquals(emptyList(), clientB.calls)

        currentClient = clientB
        waitForIdle()
        // 1. Old DisposableEffect disposes → clientA.setCurrentScreenName(null)
        // 2. New LaunchedEffect enters → clientB.setCurrentScreenName("Home")
        assertEquals(listOf("screen:Home", "screen:null"), clientA.calls)
        assertEquals(listOf("screen:Home"), clientB.calls)
    }

    @Test
    fun doesNotClearScreenNameWhenNewScreenActiveDuringTransition() = runComposeUiTest {
        val client = RecordingClarityClient()
        var showHome by mutableStateOf(true)
        var showDetails by mutableStateOf(false)

        setContent {
            ClarityProvider(client = client) {
                if (showHome) {
                    ClarityScreen(name = "Home") { }
                }
                if (showDetails) {
                    ClarityScreen(name = "Details") { }
                }
            }
        }
        waitForIdle()
        assertEquals(listOf("screen:Home"), client.calls)
        assertEquals("Home", client.currentScreenName)

        // 1. Details screen enters composition (transition start: both screens active in UI tree)
        showDetails = true
        waitForIdle()
        assertEquals(listOf("screen:Home", "screen:Details"), client.calls)
        assertEquals("Details", client.currentScreenName)

        // 2. Home screen leaves composition (transition end: old screen disposed)
        showHome = false
        waitForIdle()

        // Screen:Home leaves composition, but because currentScreenName is "Details" (!= "Home"),
        // Home's disposal does NOT emit screen:null.
        assertEquals(listOf("screen:Home", "screen:Details"), client.calls)
        assertEquals("Details", client.currentScreenName)
    }
}
