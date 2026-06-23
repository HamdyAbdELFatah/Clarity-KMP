package com.hamdy.clarity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
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
