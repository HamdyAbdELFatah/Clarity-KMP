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
}
