@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.hamdy.clarity

import com.hamdy.clarity.interop.ClarityConfig as MicrosoftClarityConfig
import com.hamdy.clarity.interop.ClarityLogLevel as IosLogLevel
import com.hamdy.clarity.interop.ClarityLogLevelDebug
import com.hamdy.clarity.interop.ClarityLogLevelError
import com.hamdy.clarity.interop.ClarityLogLevelInfo
import com.hamdy.clarity.interop.ClarityLogLevelNone
import com.hamdy.clarity.interop.ClarityLogLevelVerbose
import com.hamdy.clarity.interop.ClarityLogLevelWarning
import com.hamdy.clarity.interop.ClaritySDK
import platform.Foundation.NSProcessInfo
import kotlinx.cinterop.useContents

/**
 * Creates and initializes a Clarity client.
 *
 * Thread safety is governed by [ClarityConfig.dispatchStrategy], enforced centrally by the
 * returned client: under [ClarityDispatchStrategy.EnforceMainThread] (the default) an off-main
 * call throws, and under [ClarityDispatchStrategy.DispatchToMain] it is posted to the iOS main
 * thread. The adapter below no longer enforces the main thread per-call, so the dispatch
 * strategy can actually take effect.
 */
public fun createClarityClient(config: ClarityConfig): ClarityClient {
    return DefaultClarityClient(config, IosClaritySdkAdapter)
}

/**
 * The minimum iOS major version on which the Microsoft Clarity SDK captures data.
 * Below this the SDK is present but inert; [ClarityClient.isSupported] reports `false`.
 */
internal const val MIN_IOS_MAJOR_VERSION: Int = 15

/**
 * `true` when the host iOS version meets the Clarity capture floor ([MIN_IOS_MAJOR_VERSION]).
 */
internal fun isHostIosSupported(): Boolean =
    NSProcessInfo.processInfo.operatingSystemVersion.useContents { majorVersion >= MIN_IOS_MAJOR_VERSION }

internal object IosClaritySdkAdapter : ClaritySdkAdapter {
    // NOTE: main-thread enforcement used to live here (per-method checkMainThread). It now lives
    // once, in DefaultClarityClient, so that ClarityDispatchStrategy.DispatchToMain can hop calls
    // onto the main thread instead of throwing. The adapter is therefore thread-agnostic by
    // design; callers reach it only through the client, which guarantees main-thread execution.

    override val isSupported: Boolean
        get() = isHostIosSupported()

    override fun initialize(projectId: String, logLevel: ClarityLogLevel): Boolean {
        val config = MicrosoftClarityConfig(projectId).apply { this.logLevel = logLevel.toIosLogLevel() }
        // ClaritySDK.initializeWithConfig: is declared `BOOL` in ClarityInterop.h, so cinterop
        // maps it to a non-null Boolean. Returning it directly propagates the SDK's real result.
        return runCatching { ClaritySDK.initializeWithConfig(config) }.getOrDefault(false)
    }

    override fun setOnSessionStartedCallback(callback: (String) -> Unit): Boolean {
        // The trailing lambda is the session-started callback (a `void` block); the surrounding
        // `setOnSessionStartedCallback:` call itself returns `BOOL`, which is the value returned.
        return runCatching { ClaritySDK.setOnSessionStartedCallback { it?.let(callback) } }.getOrDefault(false)
    }

    override fun setCustomUserId(value: String): Boolean = ClaritySDK.setCustomUserId(value)

    override fun setCustomSessionId(value: String): Boolean = ClaritySDK.setCustomSessionId(value)

    override fun setCurrentScreenName(value: String?): Boolean = ClaritySDK.setCurrentScreenName(value)

    override fun sendCustomEvent(value: String): Boolean = ClaritySDK.sendCustomEventWithValue(value)

    override fun setCustomTag(key: String, values: Set<String>): Boolean =
        ClaritySDK.setCustomTagWithKey(key, values = values)

    override fun pause(): Boolean {
        // ClaritySDK.pause is declared `void` in ClarityInterop.h (no success return), so we
        // request the pause and then read the SDK's own isPaused state to infer the outcome.
        // This trusts that isPaused reflects the just-issued request; if pause() were rejected,
        // isPaused would report false and this method would correctly return false.
        ClaritySDK.pause()
        return ClaritySDK.isPaused()
    }

    override fun resume(): Boolean {
        // Mirrors pause(): ClaritySDK.resume is `void`, so we resume then read isPaused and
        // negate it to infer whether capture is now active again.
        ClaritySDK.resume()
        return !ClaritySDK.isPaused()
    }

    override fun isPaused(): Boolean = ClaritySDK.isPaused()

    override fun startNewSession(callback: (String) -> Unit): Boolean =
        runCatching { ClaritySDK.startNewSessionWithCallback { it?.let(callback) } }.getOrDefault(false)

    override fun getCurrentSessionUrl(): String? = ClaritySDK.getCurrentSessionUrl()

    override fun setConsent(adsStorage: Boolean, analyticsStorage: Boolean): Boolean {
        // Clarity iOS 3.5.x exposes analytics-storage consent only; the ads-storage flag is
        // intentionally ignored here (see ClarityConsent.adsStorage KDoc). consentWithAnalyticsStorage:
        // is declared `BOOL`, so its real result is propagated.
        return ClaritySDK.consentWithAnalyticsStorage(analyticsStorage)
    }
}

private fun ClarityLogLevel.toIosLogLevel(): IosLogLevel = when (this) {
    ClarityLogLevel.None -> ClarityLogLevelNone
    ClarityLogLevel.Error -> ClarityLogLevelError
    ClarityLogLevel.Warning -> ClarityLogLevelWarning
    ClarityLogLevel.Info -> ClarityLogLevelInfo
    ClarityLogLevel.Debug -> ClarityLogLevelDebug
    ClarityLogLevel.Verbose -> ClarityLogLevelVerbose
}
