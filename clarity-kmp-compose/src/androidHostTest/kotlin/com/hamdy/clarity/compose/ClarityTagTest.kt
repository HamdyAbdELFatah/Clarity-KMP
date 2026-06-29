package com.hamdy.clarity.compose

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.v2.runComposeUiTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Compose-runtime tests for `Modifier.clarityTag`. Run on the Android JVM via Robolectric so the
 * underlying [androidx.compose.runtime.LaunchedEffect] executes for real.
 */
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM]) // SDK 35 — Robolectric 4.15 supports up to 35
class ClarityTagTest {

    @Test
    fun appliesTagOnComposition() = runComposeUiTest {
        val client = RecordingClarityClient()
        setContent {
            ClarityProvider(client = client) {
                Box(modifier = Modifier.clarityTag("plan", "premium"))
            }
        }
        waitForIdle()

        assertEquals(listOf("tag:plan=premium"), client.calls)
    }

    @Test
    fun reappliesTagWhenValueChanges() = runComposeUiTest {
        val client = RecordingClarityClient()
        var plan by mutableStateOf("free")
        setContent {
            ClarityProvider(client = client) {
                Box(modifier = Modifier.clarityTag("plan", plan))
            }
        }
        waitForIdle()
        assertEquals(listOf("tag:plan=free"), client.calls)

        plan = "premium"
        waitForIdle()
        // LaunchedEffect(key1 = key, key2 = value) re-runs when the value changes.
        assertEquals(listOf("tag:plan=free", "tag:plan=premium"), client.calls)
    }
}
