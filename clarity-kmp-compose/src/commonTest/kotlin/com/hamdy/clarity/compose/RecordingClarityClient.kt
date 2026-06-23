package com.hamdy.clarity.compose

import com.hamdy.clarity.ClarityClient
import com.hamdy.clarity.ClarityConsent
import com.hamdy.clarity.ClarityState

/**
 * A test double that delegates to a no-op client but records a label for every mutating call,
 * in call order. This lets Compose helper tests assert which SDK calls were made (and in what
 * order) without re-implementing the full ClarityClient surface as no-ops.
 *
 * Labels are prefixed by operation for readability in assertions:
 * "event:<value>", "screen:<value>", "user:<value>", "session:<value>", "tag:<key>=<values>".
 */
internal class RecordingClarityClient(
    override val state: ClarityState = ClarityState.Active,
    override val isSupported: Boolean = true,
    override val isPaused: Boolean = false,
) : ClarityClient {
    val calls = mutableListOf<String>()

    override fun setCustomUserId(value: String): Boolean = record("user:$value")
    override fun setCustomSessionId(value: String): Boolean = record("session:$value")
    override fun setCurrentScreenName(value: String?): Boolean = record("screen:$value")
    override fun sendCustomEvent(value: String): Boolean = record("event:$value")
    override fun setCustomTag(key: String, values: Set<String>): Boolean =
        record("tag:$key=${values.sorted().joinToString(",")}")

    override fun pause(): Boolean = false
    override fun resume(): Boolean = false
    override fun startNewSession(callback: (String) -> Unit): Boolean = false
    override fun getCurrentSessionUrl(): String? = null
    override fun setOnSessionStartedCallback(callback: (String) -> Unit): Boolean = false
    override fun setConsent(consent: ClarityConsent): Boolean = false

    private fun record(label: String): Boolean {
        calls += label
        return true
    }
}
