package com.hamdy.clarity.compose

import com.hamdy.clarity.ClarityClient
import com.hamdy.clarity.ClarityConsent
import com.hamdy.clarity.ClarityState
import com.hamdy.clarity.ObserverHandle
import com.hamdy.clarity.StateObserver

/**
 * A test double that delegates to a no-op client but records a label for every mutating call,
 * in call order. This lets Compose helper tests assert which SDK calls were made (and in what
 * order) without re-implementing the full ClarityClient surface as no-ops.
 *
 * Labels are prefixed by operation for readability in assertions:
 * "event:<value>", "screen:<value>", "user:<value>", "session:<value>", "tag:<key>=<values>".
 *
 * Unlike the real client, this double's [state] is **mutable** for tests via [setStateForTest]:
 * every transition (a real change) notifies currently-registered [StateObserver]s, mirroring
 * `DefaultClarityClient.updateState`. This lets `rememberClarityState` be exercised against
 * actual transitions from a pure common test.
 *
 * @param initialState the state reported by [state] until [setStateForTest] changes it.
 */
internal class RecordingClarityClient(
    initialState: ClarityState = ClarityState.Active,
    override val isSupported: Boolean = true,
    override val isPaused: Boolean = false,
) : ClarityClient {
    val calls = mutableListOf<String>()

    private val observers = mutableListOf<StateObserver>()

    // Backed by a field (not a constructor val) so setStateForTest can mutate it; the override is
    // re-declared as a custom getter that reads the field to keep ClarityClient.state's shape.
    override var state: ClarityState = initialState
        private set

    /**
     * Transitions [state] to [newState] and notifies registered observers, the same way the real
     * client does on a genuine change. No-ops on a same-value assignment (no spurious notifications),
     * matching `DefaultClarityClient.updateState`. Test-only seam.
     */
    fun setStateForTest(newState: ClarityState) {
        if (newState == state) return
        state = newState
        observers.toList().forEach { it.onStateChanged(newState) }
    }

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

    override fun observeState(observer: StateObserver): ObserverHandle {
        // Mirror the real client: deliver the current state immediately, then register for
        // subsequent transitions (driven here by setStateForTest). cancel() removes the observer.
        observers.add(observer)
        observer.onStateChanged(state)
        return ObserverHandle { observers.remove(observer) }
    }

    private fun record(label: String): Boolean {
        calls += label
        return true
    }
}
