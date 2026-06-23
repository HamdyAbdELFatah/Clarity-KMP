package com.hamdy.clarity.compose

import kotlin.test.Test
import kotlin.test.assertEquals

class TrackingActionsTest {
    @Test
    fun trackedClickSendsEventBeforeUserAction() {
        val client = RecordingClarityClient()

        performTrackedClick(client, "cta_clicked") { client.calls += "click" }

        assertEquals(listOf("event:cta_clicked", "click"), client.calls)
    }
}
