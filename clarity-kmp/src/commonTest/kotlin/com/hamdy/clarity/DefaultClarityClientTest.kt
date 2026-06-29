package com.hamdy.clarity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DefaultClarityClientTest {

    @Test
    fun disabledConfigurationDoesNotInitializeSdk() {
        val sdk = FakeClaritySdkAdapter()

        val client = DefaultClarityClient(
            config = ClarityConfig(projectId = "", enabled = false),
            sdk = sdk,
        )

        assertEquals(ClarityState.Disabled, client.state)
        assertEquals(0, sdk.initializeCalls)
        assertFalse(client.sendCustomEvent("event"))
    }

    @Test
    fun unsupportedPlatformDoesNotInitializeSdk() {
        val sdk = FakeClaritySdkAdapter(isSupported = false)

        val client = DefaultClarityClient(ClarityConfig("project"), sdk)

        assertEquals(ClarityState.Unsupported, client.state)
        assertEquals(0, sdk.initializeCalls)
        assertFalse(client.isSupported)
    }

    @Test
    fun acceptedInitializationWaitsForSessionBeforeBecomingActive() {
        val sdk = FakeClaritySdkAdapter()

        val client = DefaultClarityClient(ClarityConfig("project"), sdk)

        assertEquals(ClarityState.InitializationAccepted, client.state)
        sdk.startSession("session-1")
        assertEquals(ClarityState.Active, client.state)
    }

    @Test
    fun synchronousSessionCallbackKeepsClientActive() {
        val sdk = FakeClaritySdkAdapter(sessionOnCallbackRegistration = "already-active")

        val client = DefaultClarityClient(ClarityConfig("project"), sdk)

        assertEquals(ClarityState.Active, client.state)
    }

    @Test
    fun rejectedInitializationMovesToFailed() {
        val sdk = FakeClaritySdkAdapter(initializeResult = false)

        val client = DefaultClarityClient(ClarityConfig("project"), sdk)

        assertIs<ClarityState.Failed>(client.state)
    }

    @Test
    fun rejectedSessionCallbackRegistrationMovesToFailed() {
        // Mirrors the iOS path where the platform SDK rejects setOnSessionStartedCallback:
        // initialization itself succeeds, but registering the callback does not.
        val sdk = FakeClaritySdkAdapter(sessionCallbackResult = false)

        val client = DefaultClarityClient(ClarityConfig("project"), sdk)

        val failed = assertIs<ClarityState.Failed>(client.state)
        assertEquals("The platform SDK rejected the session callback.", failed.reason)
    }

    @Test
    fun initialMetadataIsAppliedOnlyAfterSessionStarts() {
        val sdk = FakeClaritySdkAdapter()
        DefaultClarityClient(
            config = ClarityConfig(
                projectId = "project",
                customUserId = "user-1",
                customSessionId = "checkout-42",
                customTags = mapOf("plan" to setOf("premium", "annual")),
            ),
            sdk = sdk,
        )

        assertTrue(sdk.operations.isEmpty())
        sdk.startSession("session-1")

        assertEquals(
            listOf(
                "user:user-1",
                "session:checkout-42",
                "tag:plan=annual,premium",
            ),
            sdk.operations,
        )
    }

    @Test
    fun sessionCallbackRunsAfterInitialMetadata() {
        val sdk = FakeClaritySdkAdapter()
        val client = DefaultClarityClient(
            ClarityConfig(projectId = "project", customUserId = "user-1"),
            sdk,
        )
        client.setOnSessionStartedCallback { sdk.operations += "callback:$it" }

        sdk.startSession("session-1")

        assertEquals(listOf("user:user-1", "callback:session-1"), sdk.operations)
    }

    @Test
    fun bufferedMetadataIsAcceptedBeforeActiveAndReplayedOnSessionStart() {
        // With bufferUntilActive = true (default), metadata calls made while
        // InitializationAccepted are accepted (return true) and replayed once the session
        // starts — instead of being silently dropped.
        val sdk = FakeClaritySdkAdapter()
        val client = DefaultClarityClient(ClarityConfig("project"), sdk)

        assertEquals(ClarityState.InitializationAccepted, client.state)

        // Nothing applied yet, but all calls are ACCEPTED (true), buffered for replay.
        assertTrue(client.setCustomUserId("runtime-user"))
        assertTrue(client.setCurrentScreenName("runtime-screen"))
        assertTrue(client.setCustomTag("plan", setOf("premium")))
        // A point-in-time event is NOT buffered — it still returns false pre-active.
        assertFalse(client.sendCustomEvent("early_event"))
        assertTrue(sdk.operations.isEmpty())

        sdk.startSession("session-1")

        assertEquals(
            listOf("user:runtime-user", "screen:runtime-screen", "tag:plan=premium"),
            sdk.operations,
        )
    }

    @Test
    fun bufferedMetadataIsReplayedAfterConfigMetadataAndBeforeCallback() {
        // Ordering contract: config metadata → buffered runtime metadata → caller callback.
        val sdk = FakeClaritySdkAdapter()
        val client = DefaultClarityClient(
            ClarityConfig(projectId = "project", customUserId = "config-user"),
            sdk,
        )
        client.setOnSessionStartedCallback { sdk.operations += "callback:$it" }
        // Runtime call buffered while initializing; config-user must still win for the SAME
        // field on the first session, but the buffer still flushes before the callback.
        assertTrue(client.setCustomTag("plan", setOf("premium")))

        sdk.startSession("session-1")

        assertEquals(
            listOf("user:config-user", "tag:plan=premium", "callback:session-1"),
            sdk.operations,
        )
    }

    @Test
    fun bufferIsClearedAfterReplay() {
        // Buffered metadata must not be re-applied on a second session.
        val sdk = FakeClaritySdkAdapter()
        val client = DefaultClarityClient(ClarityConfig("project"), sdk)
        assertTrue(client.setCustomTag("k", setOf("v")))

        sdk.startSession("session-1")
        val firstRun = sdk.operations.toList()
        assertEquals(listOf("tag:k=v"), firstRun)

        // No new buffered calls; a second session must NOT replay the old buffer.
        sdk.startSession("session-2")
        assertEquals(firstRun, sdk.operations)
    }

    @Test
    fun disablingBufferRestoresDropOnTheFloorBehavior() {
        // bufferUntilActive = false → pre-active metadata returns false and is not replayed.
        val sdk = FakeClaritySdkAdapter()
        val client = DefaultClarityClient(
            ClarityConfig(projectId = "project", bufferUntilActive = false),
            sdk,
        )

        assertEquals(ClarityState.InitializationAccepted, client.state)
        assertFalse(client.setCustomUserId("runtime-user"))
        assertFalse(client.setCustomTag("plan", setOf("premium")))
        assertTrue(sdk.operations.isEmpty())

        sdk.startSession("session-1")
        // Nothing was buffered, so nothing extra is applied beyond config (which is empty).
        assertTrue(sdk.operations.isEmpty())
    }

    @Test
    fun invalidMetadataIsNotBuffered() {
        // Over-length / blank values fail validation and must never enter the buffer.
        val sdk = FakeClaritySdkAdapter()
        val client = DefaultClarityClient(ClarityConfig("project"), sdk)

        assertFalse(client.setCustomUserId("x".repeat(256)))
        assertFalse(client.setCustomTag("", setOf("v")))
        assertFalse(client.setCustomTag("k", emptySet()))

        sdk.startSession("session-1")
        assertTrue(sdk.operations.isEmpty())
    }

    @Test
    fun pauseAndResumeUpdateStateOnlyWhenSdkAccepts() {
        val sdk = FakeClaritySdkAdapter()
        val client = DefaultClarityClient(ClarityConfig("project"), sdk)
        sdk.startSession("session-1")

        assertTrue(client.pause())
        assertEquals(ClarityState.Paused, client.state)
        assertTrue(client.resume())
        assertEquals(ClarityState.Active, client.state)
    }

    @Test
    fun consentUsesPrivacySafeAdsDefault() {
        val sdk = FakeClaritySdkAdapter()
        val client = DefaultClarityClient(ClarityConfig("project"), sdk)
        sdk.startSession("session-1")

        assertTrue(client.setConsent(ClarityConsent(analyticsStorage = true)))

        assertEquals("consent:false,true", sdk.operations.single())
    }

    @Test
    fun publicValuesEnforceOfficialLengthLimits() {
        val sdk = FakeClaritySdkAdapter()
        val client = DefaultClarityClient(ClarityConfig("project"), sdk)
        sdk.startSession("session-1")

        assertFalse(client.setCustomUserId("x".repeat(256)))
        assertFalse(client.setCustomSessionId("x".repeat(256)))
        assertFalse(client.setCurrentScreenName("x".repeat(256)))
        assertFalse(client.setCustomTag("key", setOf("x".repeat(256))))
        assertFalse(client.sendCustomEvent("x".repeat(255)))
        assertTrue(sdk.operations.isEmpty())
    }

    @Test
    fun onMainFastPathIsUnchangedWhenAlreadyOnMainThread() {
        // When isMainThread reports true, behavior is identical regardless of dispatchStrategy:
        // the call runs inline with its real result. No queueing, no throw.
        val sdk = FakeClaritySdkAdapter()
        val threads = FakeMainThreadAccess(isMainThread = true)
        val client = DefaultClarityClient(
            ClarityConfig("project", dispatchStrategy = ClarityDispatchStrategy.DispatchToMain),
            sdk,
            threads,
        )
        sdk.startSession("session-1")

        assertTrue(client.setCustomTag("plan", setOf("premium")))

        // Inline fast path: the action ran immediately, nothing was posted.
        assertTrue(threads.postedActions.isEmpty())
        assertEquals(listOf("tag:plan=premium"), sdk.operations)
    }

    @Test
    fun enforceMainThreadThrowsWhenCalledOffMainThread() {
        // Default strategy: an off-main call fails fast with IllegalStateException, preserving
        // the pre-dispatch-strategy contract. This locks the non-breaking default.
        val sdk = FakeClaritySdkAdapter()
        val client = DefaultClarityClient(
            ClarityConfig("project"), // dispatchStrategy defaults to EnforceMainThread
            sdk,
            FakeMainThreadAccess(isMainThread = false),
        )

        assertFailsWith<IllegalStateException> {
            client.setCustomTag("plan", setOf("premium"))
        }
        // Nothing was applied or buffered — the call never ran.
        assertTrue(sdk.operations.isEmpty())
    }

    @Test
    fun dispatchToMainQueuesOffMainMutatingCallAndReturnsTrue() {
        // DispatchToMain + off-main: the whole operation is posted to (here: captured for) the
        // main thread and the call returns true ("accepted / queued") without throwing.
        val sdk = FakeClaritySdkAdapter()
        val threads = FakeMainThreadAccess(isMainThread = false)
        val client = DefaultClarityClient(
            ClarityConfig("project", dispatchStrategy = ClarityDispatchStrategy.DispatchToMain),
            sdk,
            threads,
        )
        sdk.startSession("session-1")

        assertTrue(client.setCustomTag("plan", setOf("premium")))

        // The off-main call is accepted (true) but not yet applied — it's queued on main.
        assertTrue(sdk.operations.isEmpty())
        assertEquals(1, threads.postedActions.size)

        // When the main loop runs the posted action, it executes the full unit on main.
        threads.runPosted()
        assertEquals(listOf("tag:plan=premium"), sdk.operations)
    }

    @Test
    fun dispatchToMainReturnsNullForGetCurrentSessionUrlOffMain() {
        // A read can't return a real value it doesn't have synchronously from a background
        // thread, so under DispatchToMain it degrades to null instead of blocking/throwing.
        val sdk = FakeClaritySdkAdapter()
        val threads = FakeMainThreadAccess(isMainThread = false)
        val client = DefaultClarityClient(
            ClarityConfig("project", dispatchStrategy = ClarityDispatchStrategy.DispatchToMain),
            sdk,
            threads,
        )
        sdk.startSession("session-1")

        assertEquals(null, client.getCurrentSessionUrl())
        // The read was posted (best-effort) rather than thrown.
        assertEquals(1, threads.postedActions.size)
    }

    @Test
    fun observeStateDeliversCurrentStateImmediatelyOnRegistration() {
        val sdk = FakeClaritySdkAdapter()
        val client = DefaultClarityClient(ClarityConfig("project"), sdk)
        // After init the client is in InitializationAccepted (it has not yet seen a session).
        assertIs<ClarityState.InitializationAccepted>(client.state)

        val seen = mutableListOf<ClarityState>()
        client.observeState { seen += it }

        // The observer is invoked once, immediately, with the current state — nothing else.
        assertEquals(listOf<ClarityState>(ClarityState.InitializationAccepted), seen)
    }

    @Test
    fun observerFiresOnStateTransitionToActive() {
        val sdk = FakeClaritySdkAdapter()
        val client = DefaultClarityClient(ClarityConfig("project"), sdk)
        val seen = mutableListOf<ClarityState>()
        client.observeState { seen += it }

        sdk.startSession("session-1")

        // Immediate InitializationAccepted (on register), then Active on the first session.
        assertEquals(
            listOf<ClarityState>(ClarityState.InitializationAccepted, ClarityState.Active),
            seen,
        )
    }

    @Test
    fun cancelStopsFurtherNotifications() {
        val sdk = FakeClaritySdkAdapter()
        val client = DefaultClarityClient(ClarityConfig("project"), sdk)
        val seen = mutableListOf<ClarityState>()
        val handle = client.observeState { seen += it }

        handle.cancel()
        sdk.startSession("session-1")

        // Only the immediate registration delivery survives; nothing fires after cancel.
        assertEquals(listOf<ClarityState>(ClarityState.InitializationAccepted), seen)
    }

    @Test
    fun noNotificationWhenStateUnchanged() {
        val sdk = FakeClaritySdkAdapter()
        val client = DefaultClarityClient(ClarityConfig("project"), sdk)
        sdk.startSession("session-1")
        val seen = mutableListOf<ClarityState>()
        client.observeState { seen += it } // immediate Active

        // A call that does not transition state must not spuriously fire the observer.
        client.setConsent(ClarityConsent(analyticsStorage = true))

        assertEquals(listOf<ClarityState>(ClarityState.Active), seen)
    }

    @Test
    fun multipleObserversEachNotified() {
        val sdk = FakeClaritySdkAdapter()
        val client = DefaultClarityClient(ClarityConfig("project"), sdk)
        val seenA = mutableListOf<ClarityState>()
        val seenB = mutableListOf<ClarityState>()
        client.observeState { seenA += it }
        client.observeState { seenB += it }

        sdk.startSession("session-1")

        // Both observers receive the transition independently (multi-observer, not last-wins).
        assertEquals(
            listOf<ClarityState>(ClarityState.InitializationAccepted, ClarityState.Active),
            seenA,
        )
        assertEquals(
            listOf<ClarityState>(ClarityState.InitializationAccepted, ClarityState.Active),
            seenB,
        )
    }

    @Test
    fun noOpClientObservesReturnsDisabledAndNeverChanges() {
        val seen = mutableListOf<ClarityState>()
        val handle = noOpClarityClient().observeState { seen += it }

        // NoOp delivers its sole state (Disabled) once; cancel() is a harmless no-op.
        assertEquals(listOf<ClarityState>(ClarityState.Disabled), seen)
        handle.cancel()
        assertEquals(listOf<ClarityState>(ClarityState.Disabled), seen)
    }
}

private class FakeClaritySdkAdapter(
    override val isSupported: Boolean = true,
    private val initializeResult: Boolean = true,
    private val sessionCallbackResult: Boolean = true,
    private val sessionOnCallbackRegistration: String? = null,
) : ClaritySdkAdapter {
    var initializeCalls: Int = 0
    val operations = mutableListOf<String>()
    private var onSessionStarted: ((String) -> Unit)? = null

    override fun initialize(projectId: String, logLevel: ClarityLogLevel): Boolean {
        initializeCalls++
        return initializeResult
    }

    override fun setOnSessionStartedCallback(callback: (String) -> Unit): Boolean {
        onSessionStarted = callback
        sessionOnCallbackRegistration?.let(callback)
        return sessionCallbackResult
    }

    fun startSession(id: String) {
        onSessionStarted?.invoke(id)
    }

    override fun setCustomUserId(value: String): Boolean = true.also { operations += "user:$value" }
    override fun setCustomSessionId(value: String): Boolean = true.also { operations += "session:$value" }
    override fun setCurrentScreenName(value: String?): Boolean = true.also { operations += "screen:$value" }
    override fun sendCustomEvent(value: String): Boolean = true.also { operations += "event:$value" }
    override fun setCustomTag(key: String, values: Set<String>): Boolean = true.also {
        operations += "tag:$key=${values.sorted().joinToString(separator = ",")}"
    }

    override fun pause(): Boolean = true
    override fun resume(): Boolean = true
    override fun isPaused(): Boolean = false
    override fun startNewSession(callback: (String) -> Unit): Boolean = true
    override fun getCurrentSessionUrl(): String? = null
    override fun setConsent(adsStorage: Boolean, analyticsStorage: Boolean): Boolean = true.also {
        operations += "consent:$adsStorage,$analyticsStorage"
    }
}

/**
 * Test double for [MainThreadAccess]. It does NOT hop threads — it records every posted
 * action so a test can assert queueing, then flush them with [runPosted] to simulate the
 * main loop draining its queue. This keeps the pure-common tests dependency-free and
 * deterministic (no real Handler / dispatch_async in a unit test).
 */
private class FakeMainThreadAccess(
    override val isMainThread: Boolean,
) : MainThreadAccess {
    val postedActions = mutableListOf<() -> Unit>()

    override fun post(action: () -> Unit) {
        postedActions += action
    }

    fun runPosted() {
        val toRun = postedActions.toList()
        postedActions.clear()
        toRun.forEach { it() }
    }
}
