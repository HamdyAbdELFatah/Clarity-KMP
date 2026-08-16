package com.hamdy.clarity.compose

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Compose UI tests for `Modifier.clarityClickable`. Runs on the Android JVM via Robolectric.
 */
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
class ClarityClickableTest {

    @Test
    fun performsClickAndTracksEvent() = runComposeUiTest {
        val client = RecordingClarityClient()
        var actionClicked = false
        setContent {
            ClarityProvider(client = client) {
                Box(
                    modifier = Modifier
                        .testTag("button")
                        .clarityClickable("cta_clicked") {
                            actionClicked = true
                        },
                )
            }
        }
        waitForIdle()

        onNodeWithTag("button").performClick()
        waitForIdle()

        assertTrue(actionClicked)
        assertEquals(listOf("event:cta_clicked"), client.calls)
    }

    @Test
    fun respectsEnabledFlagWhenDisabled() = runComposeUiTest {
        val client = RecordingClarityClient()
        var actionClicked = false
        setContent {
            ClarityProvider(client = client) {
                Box(
                    modifier = Modifier
                        .testTag("button")
                        .clarityClickable("cta_clicked", enabled = false) {
                            actionClicked = true
                        },
                )
            }
        }
        waitForIdle()

        onNodeWithTag("button").performClick()
        waitForIdle()

        assertEquals(false, actionClicked)
        assertEquals(emptyList(), client.calls)
    }
}
