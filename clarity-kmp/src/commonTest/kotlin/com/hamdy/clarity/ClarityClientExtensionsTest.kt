package com.hamdy.clarity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ClarityClientExtensionsTest {

    @Test
    fun isActiveReflectsActiveState() {
        val client = FakeClient(state = ClarityState.Active)
        assertTrue(client.isActive)
        assertTrue(client.isReady)
        assertFalse(client.isTerminal)
    }

    @Test
    fun isReadyIncludesPausedState() {
        val client = FakeClient(state = ClarityState.Paused)
        assertFalse(client.isActive)
        assertTrue(client.isReady)
        assertFalse(client.isTerminal)
    }

    @Test
    fun isTerminalForDisabledUnsupportedAndFailed() {
        assertTrue(FakeClient(state = ClarityState.Disabled).isTerminal)
        assertTrue(FakeClient(state = ClarityState.Unsupported).isTerminal)
        assertTrue(FakeClient(state = ClarityState.Failed("oops")).isTerminal)
    }

    @Test
    fun isTerminalFalseForInitializationAccepted() {
        val client = FakeClient(state = ClarityState.InitializationAccepted)
        assertFalse(client.isActive)
        assertFalse(client.isReady)
        assertFalse(client.isTerminal)
    }

    @Test
    fun fluentAliasesDelegateToOriginalMethods() {
        val client = FakeClient(state = ClarityState.Active)

        assertTrue(client.trackEvent("purchase"))
        assertTrue(client.userId("user-1"))
        assertTrue(client.sessionId("session-1"))
        assertTrue(client.screen("Checkout"))
        assertTrue(client.tag("plan", "premium"))
        assertTrue(client.tag("plan", setOf("premium", "annual")))

        assertEquals(
            listOf(
                "event:purchase",
                "user:user-1",
                "session:session-1",
                "screen:Checkout",
                "tag:plan=premium",
                "tag:plan=annual,premium",
            ),
            client.operations,
        )
    }

    @Test
    fun sendEventsSendsAllEventsAndReportsFailures() {
        val client = FakeClient(state = ClarityState.Active, acceptedEvents = setOf("a", "c"))

        val result = client.sendEvents("a", "b", "c")

        assertFalse(result)
        assertEquals(listOf("event:a", "event:b", "event:c"), client.operations)
    }

    @Test
    fun setTagsPairSendsAllTagsAndReportsFailures() {
        val client = FakeClient(state = ClarityState.Active, acceptedTagKeys = setOf("a", "c"))

        val result = client.setTags("a" to "1", "b" to "2", "c" to "3")

        assertFalse(result)
        assertEquals(
            listOf("tag:a=1", "tag:b=2", "tag:c=3"),
            client.operations,
        )
    }

    @Test
    fun setTagsMapSendsAllTagsAndReportsFailures() {
        val client = FakeClient(
            state = ClarityState.Active,
            acceptedTagKeys = setOf("plan"),
        )

        val result = client.setTags(
            mapOf(
                "plan" to setOf("premium"),
                "role" to setOf("admin"),
            ),
        )

        assertFalse(result)
        assertEquals(
            listOf("tag:plan=premium", "tag:role=admin"),
            client.operations,
        )
    }

    @Test
    fun withScreenSetsAndResetsScreenName() {
        val client = FakeClient(state = ClarityState.Active)

        val result = client.withScreen("Checkout") {
            "inside"
        }

        assertEquals("inside", result)
        assertEquals(
            listOf("screen:Checkout", "screen:null"),
            client.operations,
        )
    }

    @Test
    fun withScreenResetsScreenNameEvenWhenBlockThrows() {
        val client = FakeClient(state = ClarityState.Active)

        runCatching {
            client.withScreen("Checkout") {
                error("boom")
            }
        }

        assertEquals(
            listOf("screen:Checkout", "screen:null"),
            client.operations,
        )
    }

    @Test
    fun ifActiveRunsBlockOnlyWhenActive() {
        val activeClient = FakeClient(state = ClarityState.Active)
        val inactiveClient = FakeClient(state = ClarityState.InitializationAccepted)

        assertTrue(activeClient.ifActive { sendCustomEvent("x") })
        assertFalse(inactiveClient.ifActive { sendCustomEvent("x") })

        assertEquals(listOf("event:x"), activeClient.operations)
        assertEquals(emptyList(), inactiveClient.operations)
    }

    @Test
    fun ifReadyRunsBlockWhenActiveOrPaused() {
        val pausedClient = FakeClient(state = ClarityState.Paused)
        val initClient = FakeClient(state = ClarityState.InitializationAccepted)

        assertTrue(pausedClient.ifReady { sendCustomEvent("x") })
        assertFalse(initClient.ifReady { sendCustomEvent("x") })

        assertEquals(listOf("event:x"), pausedClient.operations)
        assertEquals(emptyList(), initClient.operations)
    }

    private class FakeClient(
        override val state: ClarityState,
        private val acceptedEvents: Set<String> = emptySet(),
        private val acceptedTagKeys: Set<String> = emptySet(),
    ) : ClarityClient {
        val operations = mutableListOf<String>()

        override var currentScreenName: String? = null
            private set

        override val isSupported: Boolean = true
        override val isPaused: Boolean = state == ClarityState.Paused

        override fun setCustomUserId(value: String): Boolean = record("user:$value", true)
        override fun setCustomSessionId(value: String): Boolean = record("session:$value", true)
        override fun setCurrentScreenName(value: String?): Boolean {
            currentScreenName = value
            return record("screen:$value", true)
        }
        override fun sendCustomEvent(value: String): Boolean = record("event:$value", value in acceptedEvents || acceptedEvents.isEmpty())
        override fun setCustomTag(key: String, values: Set<String>): Boolean =
            record("tag:$key=${values.sorted().joinToString(",")}", key in acceptedTagKeys || acceptedTagKeys.isEmpty())
        override fun pause(): Boolean = true
        override fun resume(): Boolean = true
        override fun startNewSession(callback: (String) -> Unit): Boolean = true
        override fun getCurrentSessionUrl(): String? = null
        override fun setOnSessionStartedCallback(callback: (String) -> Unit): Boolean = true
        override fun setConsent(consent: ClarityConsent): Boolean = true
        override fun observeState(observer: StateObserver): ObserverHandle = ObserverHandle { }

        private fun record(operation: String, accepted: Boolean): Boolean {
            operations += operation
            return accepted
        }
    }
}
