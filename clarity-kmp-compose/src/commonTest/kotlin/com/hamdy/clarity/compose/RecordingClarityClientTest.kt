package com.hamdy.clarity.compose

import com.hamdy.clarity.ClarityConsent
import com.hamdy.clarity.ClarityState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecordingClarityClientTest {
    @Test
    fun mutatingCallsAreRecordedInOrder() {
        val client = RecordingClarityClient()

        client.setCustomUserId("u1")
        client.setCustomSessionId("s1")
        client.setCurrentScreenName("home")
        client.sendCustomEvent("tap")
        client.setCustomTag("plan", setOf("pro", "annual"))

        assertEquals(
            listOf("user:u1", "session:s1", "screen:home", "event:tap", "tag:plan=annual,pro"),
            client.calls,
        )
    }

    @Test
    fun mutatingCallsReturnTrue() {
        val client = RecordingClarityClient()

        assertTrue(client.sendCustomEvent("e"))
        assertTrue(client.setCurrentScreenName("s"))
        assertTrue(client.setCustomTag("k", setOf("v")))
    }

    @Test
    fun nonRecordedOperationsAreNoOps() {
        val client = RecordingClarityClient(state = ClarityState.Active)

        assertEquals(false, client.pause())
        assertEquals(false, client.resume())
        assertEquals(false, client.startNewSession {})
        assertEquals(null, client.getCurrentSessionUrl())
        assertEquals(false, client.setOnSessionStartedCallback {})
        assertEquals(false, client.setConsent(ClarityConsent(analyticsStorage = true)))

        // Only the state property is exposed; no mutating call was made.
        assertEquals(0, client.calls.size)
        assertEquals(ClarityState.Active, client.state)
    }
}
