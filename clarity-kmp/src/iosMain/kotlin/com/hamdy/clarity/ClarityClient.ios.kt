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
import platform.Foundation.NSThread
import kotlinx.cinterop.useContents

/** Creates and initializes a Clarity client. Call this on the iOS main thread. */
public fun createClarityClient(config: ClarityConfig): ClarityClient {
    checkMainThread()
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
    override val isSupported: Boolean
        get() = isHostIosSupported()

    override fun initialize(projectId: String, logLevel: ClarityLogLevel): Boolean = onMainThread {
        val config = MicrosoftClarityConfig(projectId).apply { this.logLevel = logLevel.toIosLogLevel() }
        // ClaritySDK.initializeWithConfig: is declared `BOOL` in ClarityInterop.h, so cinterop
        // maps it to a non-null Boolean. Returning it directly propagates the SDK's real result.
        runCatching { ClaritySDK.initializeWithConfig(config) }.getOrDefault(false)
    }

    override fun setOnSessionStartedCallback(callback: (String) -> Unit): Boolean = onMainThread {
        // The trailing lambda is the session-started callback (a `void` block); the surrounding
        // `setOnSessionStartedCallback:` call itself returns `BOOL`, which is the value returned.
        runCatching { ClaritySDK.setOnSessionStartedCallback { it?.let(callback) } }.getOrDefault(false)
    }

    override fun setCustomUserId(value: String): Boolean = onMainThread {
        ClaritySDK.setCustomUserId(value)
    }

    override fun setCustomSessionId(value: String): Boolean = onMainThread {
        ClaritySDK.setCustomSessionId(value)
    }

    override fun setCurrentScreenName(value: String?): Boolean = onMainThread {
        ClaritySDK.setCurrentScreenName(value)
    }

    override fun sendCustomEvent(value: String): Boolean = onMainThread {
        ClaritySDK.sendCustomEventWithValue(value)
    }

    override fun setCustomTag(key: String, values: Set<String>): Boolean = onMainThread {
        ClaritySDK.setCustomTagWithKey(key, values = values)
    }

    override fun pause(): Boolean = onMainThread {
        // ClaritySDK.pause is declared `void` in ClarityInterop.h (no success return), so we
        // request the pause and then read the SDK's own isPaused state to infer the outcome.
        // This trusts that isPaused reflects the just-issued request; if pause() were rejected,
        // isPaused would report false and this method would correctly return false.
        ClaritySDK.pause()
        ClaritySDK.isPaused()
    }

    override fun resume(): Boolean = onMainThread {
        // Mirrors pause(): ClaritySDK.resume is `void`, so we resume then read isPaused and
        // negate it to infer whether capture is now active again.
        ClaritySDK.resume()
        !ClaritySDK.isPaused()
    }

    override fun isPaused(): Boolean = onMainThread { ClaritySDK.isPaused() }

    override fun startNewSession(callback: (String) -> Unit): Boolean = onMainThread {
        runCatching { ClaritySDK.startNewSessionWithCallback { it?.let(callback) } }.getOrDefault(false)
    }

    override fun getCurrentSessionUrl(): String? = onMainThread { ClaritySDK.getCurrentSessionUrl() }

    override fun setConsent(adsStorage: Boolean, analyticsStorage: Boolean): Boolean = onMainThread {
        // Clarity iOS 3.5.x exposes analytics-storage consent only; the ads-storage flag is
        // intentionally ignored here (see ClarityConsent.adsStorage KDoc). consentWithAnalyticsStorage:
        // is declared `BOOL`, so its real result is propagated.
        ClaritySDK.consentWithAnalyticsStorage(analyticsStorage)
    }
}

private inline fun <T> onMainThread(block: () -> T): T {
    checkMainThread()
    return block()
}

private fun checkMainThread() {
    check(NSThread.isMainThread) { "Clarity APIs must be called on the iOS main thread." }
}

private fun ClarityLogLevel.toIosLogLevel(): IosLogLevel = when (this) {
    ClarityLogLevel.None -> ClarityLogLevelNone
    ClarityLogLevel.Error -> ClarityLogLevelError
    ClarityLogLevel.Warning -> ClarityLogLevelWarning
    ClarityLogLevel.Info -> ClarityLogLevelInfo
    ClarityLogLevel.Debug -> ClarityLogLevelDebug
    ClarityLogLevel.Verbose -> ClarityLogLevelVerbose
}
